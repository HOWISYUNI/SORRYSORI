package appcontest.sorrysori;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.concurrent.atomic.AtomicBoolean;

public class Decibelfragment extends MainActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener, MapFragment.MapFragmentAvailableListener {

    private AtomicBoolean isComputingMovingLeq = new AtomicBoolean(false);
    private MeasurementManager measurementManager;
    // For the Charts
    private DoProcessing doProcessing;
    private onButtonStop onbuttonStop;
    private Button buttonrecord;
    private Button buttonsaved;
    private Button buttonstop;

    // Other resources
    private boolean mIsBound = false;
    private Storage.Record record;
    public final static double MIN_SHOWN_DBA_VALUE = 20;
    public final static double MAX_SHOWN_DBA_VALUE = 120;

    private static final int DEFAULT_MINIMAL_LEQ = 1;
    private static final int DEFAULT_DELETE_LEQ_ON_PAUSE = 0;

    private boolean hasMaximalMeasurementTime;
    private int maximalMeasurementTime = 0;

    // NoiseCapture will switch from Hann window to Rectangular window if the measurement delay
    // is superior than this value in ms
    private final static long SWITCH_TO_FAST_RECTANGULAR_DELAY = 1500;

    private static final String LOG_SCALE_SETTING = "settings_spectrogram_logscalemode";
    private static final String DELETE_LEQ_ON_PAUSE_SETTING = "settings_delete_leq_on_pause";
    private static final String HAS_MAXIMAL_MEASURE_TIME_SETTING = "settings_recording";
    private static final String MAXIMAL_MEASURE_TIME_SETTING = "settings_recording_duration";
    private static final String SETTINGS_MEASUREMENT_DISPLAY_WINDOW = "settings_measurement_display_window";
    private static final int DEFAULT_MAXIMAL_MEASURE_TIME_SETTING = 10;
    public static final boolean DEBUG = Boolean.parseBoolean("true");

    public int getRecordId() {
        return measurementService.getRecordId();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(LOG_SCALE_SETTING.equals(key)) {
        } else if(DELETE_LEQ_ON_PAUSE_SETTING.equals(key)) {
            measurementService.setDeletedLeqOnPause(getInteger(sharedPreferences,key, DEFAULT_DELETE_LEQ_ON_PAUSE));
        } else if(HAS_MAXIMAL_MEASURE_TIME_SETTING.equals(key)) {
            hasMaximalMeasurementTime = sharedPreferences.getBoolean(HAS_MAXIMAL_MEASURE_TIME_SETTING,
                    false);
        } else if(MAXIMAL_MEASURE_TIME_SETTING.equals(key)) {
            maximalMeasurementTime = getInteger(sharedPreferences,MAXIMAL_MEASURE_TIME_SETTING, DEFAULT_MAXIMAL_MEASURE_TIME_SETTING);
        } else if("settings_recording_gain".equals(key) && measurementService != null) {
            measurementService.setdBGain(getDouble(sharedPreferences, key, 0));
        } else if(SETTINGS_MEASUREMENT_DISPLAY_WINDOW.equals(key) && measurementService != null) {
            measurementService.getAudioProcess().setHannWindowFast(sharedPreferences.getString(SETTINGS_MEASUREMENT_DISPLAY_WINDOW, "RECTANGULAR").equals("HANN"));
            if(DEBUG) {
                System.out.println("Switch to Rectangular window");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(checkAndAskPermissions()) {
            // Application have right now all permissions
            doBindService();
        }

        setContentView(R.layout.activity_decibel);
        initDrawer();

        // Check if the dialog box (for caution) must be displayed
        // Depending of the settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        hasMaximalMeasurementTime = sharedPref.getBoolean(HAS_MAXIMAL_MEASURE_TIME_SETTING,
                false);
        maximalMeasurementTime = getInteger(sharedPref, MAXIMAL_MEASURE_TIME_SETTING, DEFAULT_MAXIMAL_MEASURE_TIME_SETTING);

        // Enabled/disabled buttons
        buttonsaved = (Button) findViewById(R.id.svdBtn);
        buttonsaved.setEnabled(true);

        buttonsaved.setOnClickListener(onButtonSaved);

        // To start a record (test mode)
        buttonrecord = (Button) findViewById(R.id.recordBtn);
        buttonrecord.setEnabled(true);

        // Actions on record button
        doProcessing = new DoProcessing(this);
        buttonrecord.setOnClickListener(doProcessing);

        //To stop a record
        buttonstop = (Button) findViewById(R.id.stopBtn);
        buttonstop.setEnabled(false);
        onbuttonStop = new onButtonStop(this);
        buttonstop.setOnClickListener(onbuttonStop);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_RECORD_AUDIO_AND_GPS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    doBindService();
                } else {
                    // permission denied
                    // Ask again
                    checkAndAskPermissions();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkTransferResults();
    }

    @Override
    public void onMapFragmentAvailable(MapFragment mapFragment) {

    }

    @Override
    public void onPageLoaded(MapFragment mapFragment) {

    }

    private View.OnClickListener onButtonSaved = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Stop measurement without waiting for the end of processing
            measurementService.stopRecording();
            Intent a = new Intent(getApplicationContext(), History.class);
            startActivity(a);
            finish();
        }
    };

    private static final class onButtonStop implements View.OnClickListener {
        private Decibelfragment activity;

        public onButtonStop(Decibelfragment activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            // Add the buttons
            builder.setPositiveButton(R.string.comment_no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if(activity.record != null) {
                        // Delete record
                        activity.measurementManager.deleteRecord(activity.record.getId());
                        activity.record = null;
                    }
                }
            });
            builder.setNegativeButton(R.string.comment_yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent ir = new Intent(activity, Results.class);
                    if(activity.record != null) {
                        ir.putExtra(MainActivity.RESULTS_RECORD_ID, activity.record.getId());
                    }
                    ir.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    activity.startActivity(ir);
                }
            });
            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.setTitle(R.string.comment_title_delete);
            dialog.show();
        }
    }

    private static class WaitEndOfProcessing implements Runnable {
        private Decibelfragment activity;
        private ProgressDialog processingDialog;

        public WaitEndOfProcessing(Decibelfragment activity, ProgressDialog processingDialog) {
            this.activity = activity;
            this.processingDialog = processingDialog;
        }

        @Override
        public void run() {
            int lastShownProgress = 0;
            while(activity.measurementService.getAudioProcess().getCurrentState() !=
                    AudioProcess.STATE.CLOSED && !activity.measurementService.isCanceled()) {
                try {
                    Thread.sleep(200);
                    int progress =  activity.measurementService.getAudioProcess().getRemainingNotProcessSamples();
                    if(progress != lastShownProgress) {
                        lastShownProgress = progress;
                        activity.runOnUiThread(new SetDialogMessage(processingDialog, activity.getResources().getString(R.string.measurement_processlastsamples,
                                lastShownProgress)));
                    }
                } catch (InterruptedException ex) {
                    return;
                }
            }


            // If canceled or ended before 1s
            if(!activity.measurementService.isCanceled() && activity.measurementService.getLeqAdded() != 0) {
                processingDialog.dismiss();
                // Goto the Results activity
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.finish();
                    }
                });


            } else {
                // No recordId available, restart measurement activity
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processingDialog.dismiss();
                        Intent im = new Intent(activity, Decibelfragment.class);
                        im.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        activity.startActivity(im);
                        activity.finish();
                    }});
            }
        }
    }

    private static class SetDialogMessage implements Runnable {
        private ProgressDialog dialog;
        private String message;

        public SetDialogMessage(ProgressDialog dialog, String message) {
            this.dialog = dialog;
            this.message = message;
        }

        @Override
        public void run() {
            dialog.setMessage(message);
        }
    }


    private void initGuiState() {
        if(measurementService == null) {
            // measurementService is required
            return;
        }
        //initComponents();
        if (measurementService.isStoring()) {
            // Start chronometer
            MeasurementManager measurementManager = new MeasurementManager(this);
        }
    }


    private static class DoProcessing implements CompoundButton.OnClickListener,
            PropertyChangeListener {
        private Decibelfragment activity;

        public DoProcessing(Decibelfragment activity) {
            this.activity = activity;
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if(AudioProcess.PROP_MOVING_SPECTRUM.equals(event.getPropertyName())) {
                AudioProcess.AudioMeasureResult measure =(AudioProcess.AudioMeasureResult) event.getNewValue();
                if(activity.isComputingMovingLeq.compareAndSet(false, true)&& activity.measurementService.isRecording()) {
                    activity.runOnUiThread(new UpdateText(activity));
                }
                if(activity.measurementService.getAudioProcess().isHannWindowFast() && activity.measurementService.getAudioProcess().getFastNotProcessedMilliseconds() > SWITCH_TO_FAST_RECTANGULAR_DELAY) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(Decibelfragment.SETTINGS_MEASUREMENT_DISPLAY_WINDOW, "RECTANGULAR");
                            editor.apply();
                        }
                    });
                }
            } else if(AudioProcess.PROP_STATE_CHANGED.equals(event.getPropertyName())) {
                if (AudioProcess.STATE.CLOSED.equals(event.getNewValue())) {
                    activity.runOnUiThread(new UpdateText(activity));
                }
            } else if(AudioProcess.PROP_DELAYED_STANDART_PROCESSING.equals(event.getPropertyName())) {
                if(activity.hasMaximalMeasurementTime && activity.measurementService.isStoring() &&
                        activity.maximalMeasurementTime <= activity.measurementService.getLeqAdded()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.buttonrecord.performClick();
                        }
                    });
                }
            }
            else if(MeasurementService.PROP_NEW_MEASUREMENT.equals(event.getPropertyName())) {
                if(DEBUG) {
                    System.out.println("Measure offset "+activity.measurementService.getAudioProcess().getFastNotProcessedMilliseconds()+" ms");
                }

                final MeasurementService.MeasurementEventObject measurement = (MeasurementService.MeasurementEventObject) event.getNewValue();
                if(!(Double.compare(measurement.leq.getLatitude(), 0) == 0 && Double.compare(measurement.leq.getLongitude(), 0) == 0)) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                         public void run() {

                         }
                    });
                }

            }
        }

        @Override
        public void onClick(View v) {
            Button buttonrecord= (Button) activity.findViewById(R.id.recordBtn);
            Button buttonstop= (Button) activity.findViewById(R.id.stopBtn);
            buttonrecord.setVisibility(View.GONE);
            buttonstop.setVisibility(View.VISIBLE);
            buttonstop.setEnabled(true);
            activity.measurementService.startStorage();
            // Force service to stay alive even if this activity is killed (Foreground service)
            activity.startService(new Intent(activity, MeasurementService.class));
            activity.initGuiState();
        }
    }

    private final static class UpdateText implements Runnable {
        Decibelfragment activity;

        private static void formatdBA(double dbAValue, TextView textView) {
            if(dbAValue > MIN_SHOWN_DBA_VALUE && dbAValue < MAX_SHOWN_DBA_VALUE) {
                textView.setText(String.format(" %.1f", dbAValue));
            } else {
                textView.setText(R.string.no_valid_dba_value);
            }
        }

        private UpdateText(Decibelfragment activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            try {
                if(activity.measurementService.isRecording()) {
                    int seconds = activity.measurementService.getLeqAdded();
                    // Update current location of user
                    final double leq = activity.measurementService.getAudioProcess().getLeq(true);
                    //activity.setData(activity.measurementService.getAudioProcess().getLeq(false));
                    // Change the text and the textcolor in the corresponding textview
                    // for the Leqi value
                    //LeqStats leqStats = activity.measurementService.getFastLeqStats();
                    final TextView mTextView = (TextView) activity.findViewById(R.id.dec_text);
                    formatdBA(leq, mTextView);
                    final ImageView mImageView = (ImageView) activity.findViewById(R.id.dec_imgs);

                    int nc = Decibelfragment.getNEcatColors(leq); // Choose the color category in
                    int img = Decibelfragment.getNEImgs(leq); // Choose the img category in
                    // function of the sound level
                    mTextView.setTextColor(activity.NE_COLORS[nc]);
                    mImageView.setImageResource(activity.NE_IMGS[img]);

                    // Spectrum data
                    //activity.updateSpectrumGUI();
                } else {
                    activity.initGuiState();
                }
                // Debug processing time
            } finally {
                activity.isComputingMovingLeq.set(false);
            }
        }

    }

    private MeasurementService measurementService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            measurementService = ((MeasurementService.LocalBinder)service).getService();

            measurementService.setMinimalLeqCount(Decibelfragment.DEFAULT_MINIMAL_LEQ);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Decibelfragment.this);
            measurementService.setDeletedLeqOnPause(getInteger(sharedPref,Decibelfragment.DELETE_LEQ_ON_PAUSE_SETTING,
                    Decibelfragment.DEFAULT_DELETE_LEQ_ON_PAUSE));
            measurementService.setdBGain(
                    getDouble(sharedPref,"settings_recording_gain", 0));
            // Init gui if recording is ongoing
            measurementService.addPropertyChangeListener(doProcessing);

            if(!measurementService.isRecording()) {
                measurementService.startRecording();
            }
            measurementService.getAudioProcess().setDoFastLeq(true);
            measurementService.getAudioProcess().setDoOneSecondLeq(true);
            measurementService.getAudioProcess().setWeightingA(true);
            measurementService.getAudioProcess().setHannWindowOneSecond(true);
            measurementService.getAudioProcess().setHannWindowFast(sharedPref.getString(SETTINGS_MEASUREMENT_DISPLAY_WINDOW, "RECTANGULAR").equals("HANN"));
            initGuiState();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            measurementService.removePropertyChangeListener(doProcessing);
            measurementService = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        if(!bindService(new Intent(this, MeasurementService.class), mConnection,
                Context.BIND_AUTO_CREATE)) {
            Toast.makeText(Decibelfragment.this, R.string.measurement_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        } else {
            mIsBound = true;
        }
    }

    void doUnbindService() {
        if (mIsBound && measurementService != null) {
            measurementService.removePropertyChangeListener(doProcessing);
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        doBindService();
        if(measurementService != null) {
            initGuiState();
            measurementService.getAudioProcess().setDoFastLeq(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(measurementService != null) {
            // Disconnect listener from measurement
            if(measurementService.isStoring()) {
                // Disable 125ms processing as it is only used for display
                measurementService.getAudioProcess().setDoFastLeq(false);
            }
            doUnbindService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}


