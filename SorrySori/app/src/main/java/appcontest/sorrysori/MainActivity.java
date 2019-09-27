package appcontest.sorrysori;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.kakao.auth.AuthType;
import com.kakao.auth.ErrorCode;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;


public class MainActivity extends AppCompatActivity {
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
    DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
    private SessionCallback callback;

    private CheckBox checkBox;
    private String id;
    private boolean saveLoginData;
    private EditText idText;
    private SharedPreferences appData;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    public static int RC_SIGN_IN=1000;

    // Color for noise exposition representation
    public int[] NE_COLORS;
    public int[] NE_IMGS;
    public static final String RESULTS_RECORD_ID = "RESULTS_RECORD_ID";
    protected static final org.slf4j.Logger MAINLOGGER = LoggerFactory.getLogger(MainActivity.class);

    // For the list view
    public ListView mDrawerList;
    public DrawerLayout mDrawerLayout;
    public ActionBarDrawerToggle mDrawerToggle;
    private ProgressDialog progress;

    public static final int PERMISSION_RECORD_AUDIO_AND_GPS = 1;
    public static final int PERMISSION_WIFI_STATE = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Resources res = getResources();
        NE_COLORS = new int[]{res.getColor(R.color.R1_SL_level),
                res.getColor(R.color.R2_SL_level),
                res.getColor(R.color.R5_SL_level)};
        NE_IMGS = new int[]{(R.drawable.loud),(R.drawable.disgust),(R.drawable.peace)};

        mAuth = FirebaseAuth.getInstance();
        callback = new SessionCallback();
        Session.getCurrentSession().addCallback(callback);
        Session.getCurrentSession().checkAndImplicitOpen();

        checkBox = findViewById(R.id.checkBox);
        idText = findViewById(R.id.main_id);


        appData = getSharedPreferences("appData", MODE_PRIVATE);
        load();

        if (saveLoginData) {
            idText.setText(id);
            checkBox.setChecked(saveLoginData);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        SignInButton button = findViewById(R.id.google_login);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d("MainActivity", "셋 밸류");

                        //여긴 한번만 실행하게
                        //  mFirebaseDatabaseReference.child("User").setValue(null);
                        String mail = user.getEmail();
                        mFirebaseDatabaseReference.child("User").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    if(mail.equals(snapshot.child("email").getValue())){
                                        Log.d("MainActivity", String.valueOf(snapshot.child("email").getValue()));
                                        Toast.makeText(MainActivity.this, "중복된 이메일입니다.", Toast.LENGTH_LONG).show();
                                    } else{
                                        Log.d("MainActivity", "왜 중복값이 아니죠?");
                                        User user1 = new User(user.getEmail(), "asdasd");
                                        Log.d("MainActivity", String.valueOf(snapshot.child("email")));
                                        mFirebaseDatabaseReference.child("User").push().setValue(user1);
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });

                        Toast.makeText(MainActivity.this, "구글 로그인 성공!", LENGTH_LONG).show();
                        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                        startActivity(intent);
                    }
                        // Sign in success, update UI with the signed-in user's information

                });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
            }
        }
    }

    public void login(View view){
        signIn();
    }

    private void signIn(){
        String email = ((EditText)findViewById(R.id.main_id)).getText().toString();
        String password = ((EditText)findViewById(R.id.main_password)).getText().toString();

        if((email.length() >= 15 && email.length() <= 25) && (password.length() >= 6 && password.length() <= 14)) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(MainActivity.this, "이메일 또는 비밀번호가 다릅니다",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                    });
        }
        else if(email.isEmpty() || password.isEmpty())
            Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_LONG).show();
        else if(email.length() > 25 || password.length() > 14)
            Toast.makeText(this, "이메일 또는 패스워드가 깁니다.", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "이메일 또는 패스워드가 짧습니다.", Toast.LENGTH_LONG).show();
    }

    public void button(View view){
        Intent intent = new Intent(MainActivity.this, Decibelfragment.class);
        startActivity(intent);
    }

    public void kakaoLogin(View view){
        Session session = Session.getCurrentSession();
        session.addCallback(new SessionCallback());
        session.open(AuthType.KAKAO_LOGIN_ALL, MainActivity.this);
    }

    public void signUp(View view){
//        Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
//        startActivity(intent);
          Intent intent = new Intent(MainActivity.this, ChatActivity.class);
          startActivity(intent);
    }

    private void load() {
        saveLoginData = appData.getBoolean("SAVE_LOGIN_DATA", false);
        id = appData.getString("ID", "");
    }

    @Override
    protected void onDestroy() {
        // SharedPreferences 객체만으론 저장 불가능 Editor 사용
        super.onDestroy();
        SharedPreferences.Editor editor = appData.edit();
        // 에디터객체.put타입( 저장시킬 이름, 저장시킬 값 )
        // 저장시킬 이름이 이미 존재하면 덮어씌움
        editor.putBoolean("SAVE_LOGIN_DATA", checkBox.isChecked());
        editor.putString("ID", idText.getText().toString().trim());
        // apply, commit 을 안하면 변경된 내용이 저장되지 않음
        editor.apply();
    }

    public void kakaoLogout(View view){
        UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private class SessionCallback implements ISessionCallback {
        @Override
        public void onSessionOpened() {
            UserManagement.getInstance().requestMe(new MeResponseCallback() {

                @Override
                public void onFailure(ErrorResult errorResult) {
                    String message = "failed to get user info. msg=" + errorResult;
                    Logger.d(message);

                    ErrorCode result = ErrorCode.valueOf(errorResult.getErrorCode());
                    if (result == ErrorCode.CLIENT_ERROR_CODE) {
                        finish();
                    } else {
                        //redirectMainActivity();
                    }
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                }

                @Override
                public void onNotSignedUp() {
                }

                @Override
                public void onSuccess(UserProfile userProfile) {
                    //로그인에 성공하면 로그인한 사용자의 일련번호, 닉네임, 이미지url등을 리턴합니다.
                    //사용자 ID는 보안상의 문제로 제공하지 않고 일련번호는 제공합니다.
                    Log.e("UserProfile", userProfile.toString());
                    Intent intent = new Intent(MainActivity.this, Decibelfragment.class);
                    startActivity(intent);
                }
            });
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            // 세션 연결이 실패했을때
            // 어쩔때 실패되는지는 테스트를 안해보았음 ㅜㅜ
        }
    }

    //Decibel
    protected boolean checkAndAskPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                // After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this,R.string.permission_explain_audio_record, Toast.LENGTH_LONG).show();
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // After the user
                // sees the explanation, try again to request the permission.
                //Toast.makeText(this,R.string.permission_explain_gps, Toast.LENGTH_LONG).show();
            }
            // Request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.FOREGROUND_SERVICE},
                    PERMISSION_RECORD_AUDIO_AND_GPS);
            return false;
        }
        return true;
    }


    @Override
    protected void onPause() {
        if(progress != null && progress.isShowing()) {
            try {
                progress.dismiss();
            } catch (IllegalArgumentException ex) {
                //Ignore
            }
        }
        super.onPause();
    }

    /**
     * @return Version information on this application
     * @throws PackageManager.NameNotFoundException
     */

    /**
     * If necessary request user to acquire permisions for critical ressources (gps and microphone)
     * @return True if service can be bind immediately. Otherwise the bind should be done using the
     * @see #onRequestPermissionsResult
     */
    protected boolean checkAndAskWifiStatePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_WIFI_STATE)) {
                // After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this,R.string.permission_explain_access_wifi_state, Toast.LENGTH_LONG).show();
            }
            // Request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_WIFI_STATE},
                    PERMISSION_WIFI_STATE);
            return false;
        }
        return true;
    }

    void initDrawer(Integer recordId) {
        try {
            // List view
            mDrawerLayout = (DrawerLayout) findViewById(R.id.dec_layout);
            mDrawerList = (ListView) findViewById(R.id.left_drawer);
            // Set the adapter for the list view
            mDrawerToggle = new ActionBarDrawerToggle(
                    this,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    R.string.drawer_open,  /* "open drawer" description */
                    R.string.drawer_close  /* "close drawer" description */
            ) {
                /**
                 * Called when a drawer has settled in a completely closed state.
                 */
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    getSupportActionBar().setTitle(getTitle());
                }

                /**
                 * Called when a drawer has settled in a completely open state.
                 */
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    getSupportActionBar().setTitle(getString(R.string.title_menu));
                }
            };
            // Set the drawer toggle as the DrawerListener
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

        } catch (Exception e) {
            MAINLOGGER.error(e.getLocalizedMessage(), e);
        }
    }

    // Drawer navigation
    void initDrawer() {
        initDrawer(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public void onBackPressed() {
        if(!(this instanceof Decibelfragment)) {
            if(mDrawerLayout != null) {
                mDrawerLayout.closeDrawer(mDrawerList);
            }
            Intent im = new Intent(getApplicationContext(),Decibelfragment.class);
            im.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(im);
            finish();
        } else {
            finish();
            // Show home
            Intent im = new Intent(Intent.ACTION_MAIN);
            im.addCategory(Intent.CATEGORY_HOME);
            im.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(im);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        CharSequence mTitle = title;
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(mTitle);
        }
    }
    protected boolean isManualTransferOnly() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return !sharedPref.getBoolean("settings_data_transfer", true);
    }

    protected boolean isWifiTransferOnly() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("settings_data_transfer_wifi_only", false);
    }

    /**
     * Check the non-uploaded results and the connection states
     * Upload results if necessary
     */
    protected void checkTransferResults() {
        if (!isManualTransferOnly()) {
            MeasurementManager measurementManager = new MeasurementManager(this);
            if (!measurementManager.hasNotUploadedRecords()) {
                return;
            }
            if (isWifiTransferOnly()) {
                if (checkAndAskWifiStatePermission()) {
                    if (!checkWifiState()) {
                        return;
                    }
                } else {
                    // Transfer will begin when user validate check wifi rights
                    return;
                }
            }
            new Thread(new DoSendZipToServer(this)).start();
        }
    }


    /**
     * Ping largest internet dns access to check if internet is available
     * @return True if Internet is available
     */
    public boolean isOnline() {
        try {
            URL url = new URL(MeasurementUploadWPS.BASE_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            int code = urlConnection.getResponseCode();
            return code == 200 || code == 301 || code == 302;
        } catch (IOException e) {
            MAINLOGGER.error(e.getLocalizedMessage(), e);
        }
        return false;
    }

    /**
     * Transfer provided records
     */
    protected void doTransferRecords(List<Integer> selectedRecordIds) {
        runOnUiThread(new SendResults(this, selectedRecordIds));
    }

    /**
     * Transfer records without checking user preferences
     */
    protected void doTransferRecords() {
        if(!isOnline()) {
            MAINLOGGER.info("Not online, skip send of record");
            return;
        }
        MeasurementManager measurementManager = new MeasurementManager(this);
        List<Storage.Record> records = measurementManager.getRecords();
        final List<Integer> recordsToTransfer = new ArrayList<>();
        for(Storage.Record record : records) {
            // Auto send records only if the record is not in progress and if the user have
            // validated the Description activity
            if(record.getUploadId().isEmpty() && record.getTimeLength() > 0 && record
                    .getNoisePartyTag() != null) {
                recordsToTransfer.add(record.getId());
            }
        }
        if(!recordsToTransfer.isEmpty()) {
            doTransferRecords(recordsToTransfer);
        }
    }

    protected static final class SendResults implements Runnable {
        private MainActivity mainActivity;
        private List<Integer> recordsToTransfer;

        public SendResults(MainActivity mainActivity, List<Integer> recordsToTransfer) {
            this.mainActivity = mainActivity;
            this.recordsToTransfer = recordsToTransfer;
        }

        public SendResults(MainActivity mainActivity, Integer... recordsToTransfer) {
            this.mainActivity = mainActivity;
            this.recordsToTransfer = Arrays.asList(recordsToTransfer);
        }

        @Override
        public void run() {
            // Export
            try {
                mainActivity.progress = ProgressDialog.show(mainActivity, mainActivity
                                .getText(R.string
                                        .upload_progress_title),
                        mainActivity.getText(R.string.upload_progress_message), true);
            } catch (RuntimeException ex) {
                // This error may arise on some system
                // The display of progression are not vital so cancel the crash by handling the
                // error
                MAINLOGGER.error(ex.getLocalizedMessage(), ex);
            }
            new Thread(new SendZipToServer(mainActivity, recordsToTransfer, mainActivity
                    .progress, new
                    OnUploadedListener() {
                        @Override
                        public void onMeasurementUploaded() {
                            mainActivity.onTransferRecord();
                        }
                    })).start();
        }
    }

    protected void onTransferRecord() {
        // Nothing to do
    }


    /***
     * Checks that application runs first time and write flags at SharedPreferences
     * Need further codes for enhancing conditions
     * @return true if 1st time
     * see : http://stackoverflow.com/questions/9806791/showing-a-message-dialog-only-once-when-application-is-launched-for-the-first
     * see also for checking version (later) : http://stackoverflow.com/questions/7562786/android-first-run-popup-dialog
     * Can be used for checking new version
     */
    protected boolean CheckNbRun(String preferenceName, int maxCount) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Integer NbRun = preferences.getInt(preferenceName, 1);
        if (NbRun > maxCount) {
            NbRun=1;
        }
        editor.putInt(preferenceName, NbRun+1);
        editor.apply();
        return (NbRun==1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_WIFI_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkTransferResults();
                }
            }
        }
    }

    private boolean checkWifiState() {

        // Check connection state
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled()) { // WiFi adapter is ON
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            if (wifiInfo.getNetworkId() == -1) {
                return false; // Not connected to an access-Point
            }
            // Connected to an Access Point
            return true;
        } else {
            return false; // WiFi adapter is OFF
        }
    }

    public static final class DoSendZipToServer implements Runnable {
        MainActivity mainActivity;

        public DoSendZipToServer(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void run() {
            mainActivity.doTransferRecords();
        }
    }

    public static final class SendZipToServer implements Runnable {
        private Activity activity;
        private List<Integer> recordsId = new ArrayList<>();
        private ProgressDialog progress;
        private final OnUploadedListener listener;

        public SendZipToServer(Activity activity, Collection<Integer> records, ProgressDialog progress, OnUploadedListener listener) {
            this.activity = activity;
            this.recordsId.addAll(records);
            this.progress = progress;
            this.listener = listener;
        }

        @Override
        public void run() {
            MeasurementUploadWPS measurementUploadWPS = new MeasurementUploadWPS(activity);
            MeasurementManager measurementManager = new MeasurementManager(activity);
            try {
                for(Integer recordId : recordsId) {
                    Storage.Record record = measurementManager.getRecord(recordId);
                    if(record.getUploadId().isEmpty()) {
                        measurementUploadWPS.uploadRecord(recordId);
                    }
                }
                if(listener != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onMeasurementUploaded();
                        }
                    });
                }
            } catch (final IOException ex) {
                MAINLOGGER.error(ex.getLocalizedMessage(), ex);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity,
                                ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } finally {
                if(progress != null && progress.isShowing()) {
                    try {
                        progress.dismiss();
                    } catch (IllegalArgumentException ex) {
                        //Ignore
                    }
                }
            }
        }
    }

    public interface OnUploadedListener {
        void onMeasurementUploaded();
    }
    // Choose color category in function of sound level
    public static int getNEcatColors(double SL) {

        int NbNEcat;

        if (SL > 55.) {
            NbNEcat = 0;
        } else if (SL > 40) {
            NbNEcat = 1;
        } else {
            NbNEcat = 2;
        }
        return NbNEcat;
    }

    public static int getNEImgs(double SL) {

        int imgs;

        if (SL > 55.) {
            imgs = 0;
        } else if (SL > 40) {
            imgs = 1;
        } else {
            imgs = 2;
        }
        return imgs;
    }


    public static double getDouble(SharedPreferences sharedPref, String key, double defaultValue) {
        try {
            return Double.valueOf(sharedPref.getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static int getInteger(SharedPreferences sharedPref, String key, int defaultValue) {
        try {
            return Integer.valueOf(sharedPref.getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}