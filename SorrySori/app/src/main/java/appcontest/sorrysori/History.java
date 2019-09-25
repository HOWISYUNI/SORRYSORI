package appcontest.sorrysori;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/* Source for example:
 * http://www.codelearn.org/android-tutorial/android-listview
 * http://www.androidbegin.com/tutorial/android-delete-multiple-selected-items-listview-tutorial/
 * @author Pranay Airan
 * @author Nicolas Fortin
 */
public class History extends MainActivity{
    private MeasurementManager measurementManager;
    private ListView infohistory;
    private SparseBooleanArray mSelectedItemsIds = new SparseBooleanArray();
    private static final Logger LOGGER = LoggerFactory.getLogger(History.class);
    private ProgressDialog progress;

    InformationHistoryAdapter historyListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.measurementManager = new MeasurementManager(getApplicationContext());
        setContentView(R.layout.activity_history);
        initDrawer();

        // Fill the listview
        historyListAdapter = new InformationHistoryAdapter(measurementManager, this);
        infohistory = (ListView)findViewById(R.id.listiew_history);
        infohistory.setMultiChoiceModeListener(new HistoryMultiChoiceListener(this));
        infohistory.setAdapter(historyListAdapter);
        infohistory.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        infohistory.setLongClickable(true);
        infohistory.setOnItemClickListener(new HistoryItemListener(this));
    }

    private static class HistoryMultiChoiceListener implements AbsListView.MultiChoiceModeListener {
        History history;

        public HistoryMultiChoiceListener(History history) {
            this.history = history;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    }

    @Override
    protected void onTransferRecord() {
        historyListAdapter.reload();
    }

    private static final class HistoryItemListener implements OnItemClickListener {
        private History historyActivity;

        public HistoryItemListener(History historyActivity) {
            this.historyActivity = historyActivity;
        }

        private void launchResult(int recordId) {
            Intent ir = new Intent(historyActivity.getApplicationContext(), Results.class);
            ir.putExtra(RESULTS_RECORD_ID, recordId);
            historyActivity.finish();
            historyActivity.startActivity(ir);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Results
            launchResult((int)id);
        }
    }

    public static void doBuildZip(File file, Context context,int recordId) throws IOException {
        // Create parent dirs if necessary
        file.getParentFile().mkdirs();
        FileOutputStream fop = new FileOutputStream(file);
        try {
            MeasurementExport measurementExport = new MeasurementExport(context);
            measurementExport.exportRecord(recordId, fop, true);
        } finally {
            fop.close();
        }
    }

    private static final class RefreshListener implements OnUploadedListener {

        private InformationHistoryAdapter historyListAdapter;

        public RefreshListener(InformationHistoryAdapter historyListAdapter) {
            this.historyListAdapter = historyListAdapter;
        }

        @Override
        public void onMeasurementUploaded() {
            historyListAdapter.reload();
        }
    }
    public static class InformationHistoryAdapter extends BaseAdapter {
        private List<Storage.Record> informationHistoryList;
        private History activity;
        private MeasurementManager measurementManager;
        private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm z", Locale.getDefault());

        public InformationHistoryAdapter(MeasurementManager measurementManager, History activity) {
            this.informationHistoryList = measurementManager.getRecords();
            this.activity = activity;
            this.measurementManager = measurementManager;
        }

        public SparseBooleanArray getSelectedIds() {
            return activity.mSelectedItemsIds;
        }

        public void toggleSelection(int position) {
            selectView(position, !activity.mSelectedItemsIds.get(position));
        }

        public void removeSelection() {
            activity.mSelectedItemsIds.clear();
            activity.historyListAdapter.notifyDataSetChanged();
        }

        public void selectView(int position, boolean value) {
            if (value) {
                activity.mSelectedItemsIds.put(position, true);
            } else {
                activity.mSelectedItemsIds.delete(position);
            }
            activity.historyListAdapter.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return informationHistoryList.size();
        }

        public void reload() {
            informationHistoryList = measurementManager.getRecords();
            notifyDataSetChanged();
        }

        @Override
        public Object getItem(int position) {
            return informationHistoryList.get(position);
        }

        public void remove(final Collection<Integer> ids) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            // Add the buttons
            builder.setPositiveButton(R.string.comment_delete_record, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Delete records
                    activity.measurementManager.deleteRecords(ids);
                    reload();
                }
            });
            builder.setNegativeButton(R.string.comment_cancel_change, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.setTitle(R.string.history_title_delete);
            dialog.show();
        }

        @Override
        public long getItemId(int position) {
            return informationHistoryList.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context
                        .LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.history_item_layout, parent, false);
            }
            TextView description = (TextView) convertView.findViewById(R.id.textView_description_item_history);
            TextView history_Date = (TextView) convertView.findViewById(R.id.textView_Date_item_history);
            TextView history_SEL = (TextView) convertView.findViewById(R.id.textView_SEL_item_history);
            TextView history_SEL_bar = (TextView) convertView.findViewById(R.id.textView_SEL_bar_item_history);
            Storage.Record record = informationHistoryList.get(position);

            // Update history item
            Resources res = activity.getResources();
            description.setText(record.getDescription());
            history_Date.setText(res.getString(R.string.history_length, record.getTimeLength()) +" " + res.getString(R.string.history_date, simpleDateFormat.format(new Date(record.getUtc()))));
            history_SEL.setText(res.getString(R.string.history_sel, record.getLeqMean()));
            int nc = getNEcatColors(record.getLeqMean());
            history_SEL.setTextColor(activity.NE_COLORS[nc]);
            history_SEL_bar.setBackgroundColor(activity.NE_COLORS[nc]);
            return convertView;
        }

        public Storage.Record getInformationHistory(int position)
        {
            return informationHistoryList.get(position);
        }

    }
}

