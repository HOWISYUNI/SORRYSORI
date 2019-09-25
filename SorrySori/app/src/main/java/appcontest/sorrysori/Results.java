/*
 * This file is part of the NoiseCapture application and OnoMap system.
 *
 * The 'OnoMaP' system is led by Lab-STICC and Ifsttar and generates noise maps via
 * citizen-contributed noise data.
 *
 * This application is co-funded by the ENERGIC-OD Project (European Network for
 * Redistributing Geospatial Information to user Communities - Open Data). ENERGIC-OD
 * (http://www.energic-od.eu/) is partially funded under the ICT Policy Support Programme (ICT
 * PSP) as part of the Competitiveness and Innovation Framework Programme by the European
 * Community. The application work is also supported by the French geographic portal GEOPAL of the
 * Pays de la Loire region (http://www.geopal.org).
 *
 * Copyright (C) IFSTTAR - LAE and Lab-STICC – CNRS UMR 6285 Equipe DECIDE Vannes
 *
 * NoiseCapture is a free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or(at your option) any later version. NoiseCapture is distributed in the hope that
 * it will be useful,but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation,Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301  USA or see For more information,  write to Ifsttar,
 * 14-20 Boulevard Newton Cite Descartes, Champs sur Marne F-77447 Marne la Vallee Cedex 2 FRANCE
 *  or write to scientific.computing@ifsttar.fr
 */

package appcontest.sorrysori;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendPosition;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import org.orbisgis.sos.LeqStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class Results extends MainActivity {
    private static final Logger LOGGER = LoggerFactory.getLogger(Results.class);
    private MeasurementManager measurementManager;
    private Storage.Record record;
    private static final double[][] CLASS_RANGES = new double[][]{{Double.MIN_VALUE, 40}, {40, 55},{55, Double.MAX_VALUE}};

    private ToolTipRelativeLayout toolTip;
    private ToolTipView lastShownTooltip = null;

    // For the Charts
    public PieChart rneChart;
    public PieChart neiChart;
    protected BarChart sChart; // Spectrum representation

    // Other ressources
    private String[] ltob;  // List of third-octave bands
    private String[] catNE; // List of noise level category (defined as ressources)
    private List<Float> splHistogram;
    private LeqStats leqStats = new LeqStats();
    private List<String> tags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showTooltip = sharedPref.getBoolean("settings_tooltip", true);
        setContentView(R.layout.activity_results);
        this.measurementManager = new MeasurementManager(this);
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra(RESULTS_RECORD_ID)) {
            record = measurementManager.getRecord(intent.getIntExtra(RESULTS_RECORD_ID, -1));
        } else {
            // Read the last stored record
            List<Storage.Record> recordList = measurementManager.getRecords();
            if(!recordList.isEmpty()) {
                record = recordList.get(0);
            } else {
                // Message for starting a record
                Toast.makeText(getApplicationContext(),
                        getString(R.string.no_results), Toast.LENGTH_LONG).show();
                initDrawer();
                return;
            }
        }
        tags = measurementManager.getTags(record.getId());
        initDrawer(record.getId());
        toolTip = (ToolTipRelativeLayout) findViewById(R.id.activity_tooltip);

        // RNE PieChart
        rneChart = (PieChart) findViewById(R.id.RNEChart);

        initRNEChart();
        Legend lrne = rneChart.getLegend();
        lrne.setTextColor(Color.BLACK);
        lrne.setTextSize(8f);
        lrne.setPosition(LegendPosition.RIGHT_OF_CHART_CENTER);
        lrne.setEnabled(true);

        View measureButton = findViewById(R.id.measureBtn);
        measureButton.setOnClickListener(new OnGoToMeasurePage(this));
        // Action on export button

        Button exportComment=(Button)findViewById(R.id.uploadBtn);
        exportComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new SendResults(Results.this, record.getId()));
            }
        });

        exportComment.setEnabled(record.getUploadId().isEmpty());


        AsyncTask.execute(new LoadMeasurements(this));
    }

    private static final class OnGoToMeasurePage implements View.OnClickListener {
        private Results activity;

        public OnGoToMeasurePage(Results activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View v) {
            //Open result page
            Intent ir = new Intent(activity, Decibelfragment.class);
            activity.startActivity(ir);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Transfer results automatically (with all checking)
        checkTransferResults();
    }

    protected void onTransferRecord() {
        // Nothing to do
        // Change upload state
        Button exportComment=(Button)findViewById(R.id.uploadBtn);
        // Refresh record
        record = measurementManager.getRecord(record.getId());
        exportComment.setEnabled(record.getUploadId().isEmpty());
    }

    // Read spl data for spectrum representation
    private void setDataS() {

        ArrayList<String> xVals = new ArrayList<String>();
        Collections.addAll(xVals, ltob);


        ArrayList<BarEntry> yVals1 = new ArrayList<BarEntry>();

        for (int i = 0; i < splHistogram.size(); i++) {
            yVals1.add(new BarEntry(splHistogram.get(i), i));
        }

        BarDataSet set1 = new BarDataSet(yVals1, "DataSet");
        set1.setValueTextColor(Color.BLACK);

        set1.setColors(
                new int[]{Color.rgb(0, 128, 255), Color.rgb(0, 128, 255), Color.rgb(0, 128, 255),
                        Color.rgb(102, 178, 255), Color.rgb(102, 178, 255),
                        Color.rgb(102, 178, 255)});

        ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
        dataSets.add(set1);
    }

    // Init RNE Pie Chart
    public void initRNEChart(){
        rneChart.setUsePercentValues(true);
        rneChart.setHoleColor(Color.TRANSPARENT);
        rneChart.setHoleRadius(40f);
        rneChart.setDescription("");
        rneChart.setDrawCenterText(true);
        rneChart.setDrawHoleEnabled(true);
        rneChart.setRotationAngle(0);
        rneChart.setRotationEnabled(true);
        rneChart.setDrawSliceText(false);
        rneChart.setCenterText("통계");
        rneChart.setCenterTextColor(Color.BLACK);
    }


    // Set computed data in chart
    private void setRNEData(List<Double> classRangeValue) {
        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        // IMPORTANT: In a PieChart, no values (Entry) should have the same
        // xIndex (even if from different DataSets), since no values can be
        // drawn above each other.
        catNE= getResources().getStringArray(R.array.catNE_list_array);
        ArrayList<String> xVals = new ArrayList<String>();
        double maxValue = 0;
        int maxClassId = 0;
        for (int idEntry = 0; idEntry < classRangeValue.size(); idEntry++) {
            float value = classRangeValue.get(classRangeValue.size() - 1 - idEntry).floatValue();
            // Fix background color issue if the pie is too thin
            if(value < 0.01) {
                value = 0;
            }
            yVals1.add(new Entry(value, idEntry));
            xVals.add(catNE[idEntry]);
            if (value > maxValue) {
                maxClassId = idEntry;
                maxValue = value;
            }
        }

        PieDataSet dataSet = new PieDataSet(yVals1,Results.this.getString(R.string.caption_SL));
        dataSet.setSliceSpace(3f);
        dataSet.setColors(NE_COLORS);

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new CustomPercentFormatter());
        data.setValueTextSize(8f);
        data.setValueTextColor(Color.BLACK);
        rneChart.setData(data);

        // highlight the maximum value of the RNE
        // Find the maximum of the array, in order to be highlighted
        Highlight h = new Highlight(maxClassId, 0);
        rneChart.highlightValues(new Highlight[] { h });
        rneChart.invalidate();
    }
    // Generate artificial data for NEI
    private void setNEIData() {

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        // IMPORTANT: In a PieChart, no values (Entry) should have the same
        // xIndex (even if from different DataSets), since no values can be
        // drawn above each other.
        yVals1.add(new Entry( record.getLeqMean(), 0));

        ArrayList<String> xVals = new ArrayList<String>();

        xVals.add(catNE[0 % catNE.length]);

        PieDataSet dataSet = new PieDataSet(yVals1, "NEI");
        dataSet.setSliceSpace(3f);
        int nc=getNEcatColors(record.getLeqMean());    // Choose the color category in function of the sound level
        dataSet.setColor(NE_COLORS[nc]);   // Apply color category for the corresponding sound level

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.BLACK);
        data.setDrawValues(false);

        neiChart.setData(data);
        neiChart.setCenterText(String.format(Locale.getDefault(), "%.1f", record.getLeqMean())
                .concat(" dB(A)" + ""));
        neiChart.invalidate();
    }

    private static class ToolTipListener implements View.OnTouchListener {
        private Results results;
        private int resId;

        public ToolTipListener(Results results, final int resId) {
            this.results = results;
            this.resId = resId;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(results.lastShownTooltip != null && results.lastShownTooltip.isShown()) {
                // Hide last shown tooltip
                results.lastShownTooltip.remove();
            }
            if(v != null) {
                ToolTip toolTipMinSL = new ToolTip()
                        .withText(resId)
                        .withColor(Color.DKGRAY)
                        .withAnimationType(ToolTip.AnimationType.NONE)
                        .withShadow();
                results.lastShownTooltip = results.toolTip.showToolTipForView(toolTipMinSL, v);
            }
            return false;
        }
    }



    private static final class ReadRecordsProgression implements MeasurementManager
            .ProgressionCallBack, View.OnClickListener {
        private AppCompatActivity activity;
        private long beginReadRecords = 0;
        AtomicBoolean canceled = new AtomicBoolean(false);
        int recordCount = 0;
        int record = 0;
        int lastProgress = 0;
        boolean handleProgression = false;
        private static final int MINIMAL_RECORD_DISPLAY_PROGRESS = 100;
        View progressView;
        ProgressBar progressBar;
        Button button;

        public ReadRecordsProgression(AppCompatActivity activity) {
            this.activity = activity;
            progressView = activity.findViewById(R.id.result_progress_layout);
            //progressBar = (ProgressBar) activity.findViewById(R.id.result_progress_control);
            button = (Button)activity.findViewById(R.id.result_progress_cancel);
            button.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            canceled.set(true);
        }

        @Override
        public void onCreateCursor(int recordCount) {
            this.recordCount = recordCount;
            beginReadRecords = System.currentTimeMillis();
            if(recordCount > MINIMAL_RECORD_DISPLAY_PROGRESS) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(0);
                        progressView.setVisibility(View.VISIBLE);
                    }
                });
                handleProgression = true;
            }
        }

        @Override
        public boolean onCursorNext() {
            if(handleProgression) {
                record++;
                final int newProgression = (int)((record / (double) recordCount) * 100);
                if(newProgression / 5 != lastProgress / 5) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                progressBar.setProgress(newProgression, true);
                            } else {
                                progressBar.setProgress(newProgression);
                            }
                        }
                    });
                    lastProgress = newProgression;
                }
            }
            return !canceled.get();
        }

        @Override
        public void onDeleteCursor() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressView.setVisibility(View.GONE);
                }
            });
            if(BuildConfig.DEBUG) {
                System.out.println("Fetch measurement time "+(System.currentTimeMillis() - beginReadRecords)+" " +
                        "ms");
            }
        }
    }
    private static class LoadMeasurements implements Runnable {
        private Results activity;

        LoadMeasurements(Results activity) {
            this.activity = activity;
        }

        @Override
        public void run() {

            // Query database
            List<Integer> frequencies = new ArrayList<Integer>();
            List<Float[]> leqValues = new ArrayList<Float[]>();
            activity.measurementManager.getRecordLeqs(activity.record.getId(), frequencies, leqValues, new
                    ReadRecordsProgression(activity));

            // Create leq statistics by frequency
            LeqStats[] leqStatsByFreq = new LeqStats[frequencies.size()];
            for(int idFreq = 0; idFreq < leqStatsByFreq.length; idFreq++) {
                leqStatsByFreq[idFreq] = new LeqStats();
            }
            // parse each leq window time
            for(Float[] leqFreqs : leqValues) {
                double rms = 0;
                int idFreq = 0;
                for(float leqValue : leqFreqs) {
                    leqStatsByFreq[idFreq].addLeq(leqValue);
                    rms += Math.pow(10, leqValue / 10);
                    idFreq++;
                }
                activity.leqStats.addLeq(10 * Math.log10(rms));
            }
            activity.splHistogram = new ArrayList<>(leqStatsByFreq.length);
            activity.ltob = new String[leqStatsByFreq.length];
            int idFreq = 0;
            for (LeqStats aLeqStatsByFreq : leqStatsByFreq) {
                //activity.ltob[idFreq] = Spectrogram.formatFrequency(frequencies.get(idFreq));
                activity.splHistogram.add((float) aLeqStatsByFreq.getLeqMean());
                idFreq++;
            }

            final LeqStats.LeqOccurrences leqOccurrences = activity.leqStats.computeLeqOccurrences
                    (CLASS_RANGES);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.setRNEData(leqOccurrences.getUserDefinedOccurrences());
                    //activity.setNEIData();
                    activity.setDataS();

                    TextView meanText = (TextView) activity.findViewById(R.id.textView_value_Mean_SL);
                    meanText.setText(String.format(Locale.getDefault(), "%.01f", activity.leqStats
                            .getLeqMean()));

                    activity.findViewById(R.id.textView_color_Mean_SL).setBackgroundColor(activity
                            .NE_COLORS[getNEcatColors(activity.leqStats.getLeqMean())]);

                    TextView minText = (TextView) activity.findViewById(R.id.textView_value_Min_SL);
                    minText.setText(String.format(Locale.getDefault(), "%.01f", activity.leqStats
                            .getLeqMin()));

                    activity.findViewById(R.id.textView_color_Min_SL).setBackgroundColor(activity
                            .NE_COLORS[getNEcatColors(activity.leqStats.getLeqMin())]);

                    TextView maxText = (TextView) activity.findViewById(R.id.textView_value_Max_SL);
                    maxText.setText(String.format(Locale.getDefault(), "%.01f", activity.leqStats
                            .getLeqMax()));

                    activity.findViewById(R.id.textView_color_Max_SL)
                            .setBackgroundColor(activity.NE_COLORS[getNEcatColors(activity.leqStats.getLeqMax())]);
                    if(activity.rneChart != null) {
                        activity.rneChart.animateXY(1500, 1500);
                    }
                }
            });

        }
    }
}
