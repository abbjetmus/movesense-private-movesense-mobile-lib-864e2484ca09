package com.movesense.samples.dataloggersample;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsResponseListener;
import com.movesense.mds.MdsSubscription;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class DataLoggerActivity extends AppCompatActivity
        implements
        AdapterView.OnItemClickListener,
        Spinner.OnItemSelectedListener
{
    private static final String URI_MDS_LOGBOOK_ENTRIES = "suunto://MDS/Logbook/{0}/Entries";
    private static final String URI_MDS_LOGBOOK_DATA= "suunto://MDS/Logbook/{0}/ById/{1}/Data";

    private static final String URI_LOGBOOK_ENTRIES = "suunto://{0}/Mem/Logbook/Entries";
    private static final String URI_DATALOGGER_STATE = "suunto://{0}/Mem/DataLogger/State";
    private static final String URI_DATALOGGER_CONFIG = "suunto://{0}/Mem/DataLogger/Config";
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String URI_LOGBOOK_DATA = "/Mem/Logbook/byId/{0}/Data";

    private static final String INSTANCE_STATE_DATAURI_KEY = "DataLoggerActivity_LogFileURI";

    static DataLoggerActivity s_INSTANCE = null;
    private static final String LOG_TAG = DataLoggerActivity.class.getSimpleName();

    public static final String SERIAL = "serial";
    String connectedSerial;

    private DataLoggerState mDLState;
    private String mDLConfigPath;
    private TextView mDataLoggerStateTextView;


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putString("mDataFileToCopy", mDataFileToCopy == null ? null : mDataFileToCopy.getAbsolutePath());
        savedInstanceState.putString("connectedSerial", connectedSerial);

        savedInstanceState.putInt("mDLState", mDLState.content);
        savedInstanceState.putString("mDLConfigPath", mDLConfigPath);
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        String dataFileToCopyPath =savedInstanceState.getString("mDataFileToCopy");
        mDataFileToCopy = (dataFileToCopyPath==null) ? null : new File(dataFileToCopyPath);
        String connectedSerial = savedInstanceState.getString("connectedSerial");
        int dlState = savedInstanceState.getInt("mDLState");
        mDLState = new DataLoggerState(dlState);
        String mDLConfigPath = savedInstanceState.getString("mDLConfigPath");
    }

    private ListView mLogEntriesListView;
    private static ArrayList<MdsLogbookEntriesResponse.LogEntry> mLogEntriesArrayList = new ArrayList<>();
    ArrayAdapter<MdsLogbookEntriesResponse.LogEntry> mLogEntriesArrayAdapter;

    public static final String SCHEME_PREFIX = "suunto://";

    private Mds getMDS() {return MainActivity.mMds;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        s_INSTANCE = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datalogger);

        // Init state UI
        mDataLoggerStateTextView = (TextView)findViewById(R.id.textViewDLState);

        // Init Log list
        mLogEntriesListView = (ListView)findViewById(R.id.listViewLogbookEntries);
        mLogEntriesArrayAdapter = new ArrayAdapter<MdsLogbookEntriesResponse.LogEntry>(this,
                android.R.layout.simple_list_item_1, mLogEntriesArrayList);
        mLogEntriesListView.setAdapter(mLogEntriesArrayAdapter);
        mLogEntriesListView.setOnItemClickListener(this);

        Spinner pathSpinner = (Spinner)findViewById(R.id.path_spinner);
        pathSpinner.setOnItemSelectedListener(this);
        mPathSelectionSetInternally = true;
        pathSpinner.setSelection(0);


        // Find serial in opening intent
        Intent intent = getIntent();
        connectedSerial = intent.getStringExtra(SERIAL);

        updateDataLoggerUI();

        fetchDataLoggerConfig();

        refreshLogList();
    }

    private void updateDataLoggerUI() {
        Log.d(LOG_TAG, "updateDataLoggerUI() state: " + mDLState + ", path: " + mDLConfigPath);

        mDataLoggerStateTextView.setText(mDLState != null ? mDLState.toString() : "--");

        findViewById(R.id.buttonStartLogging).setEnabled(mDLState != null && mDLConfigPath!=null);
        findViewById(R.id.buttonStopLogging).setEnabled(mDLState != null);

        if (mDLState != null) {
            if (mDLState.content == 2) {
                findViewById(R.id.buttonStartLogging).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonStopLogging).setVisibility(View.GONE);
            }
            if (mDLState.content == 3) {
                findViewById(R.id.buttonStopLogging).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonStartLogging).setVisibility(View.GONE);
            }
        }
    }

    private void configureDataLogger() {
        // Access the DataLogger/Config
        String configUri = MessageFormat.format(URI_DATALOGGER_CONFIG, connectedSerial);

        // Create the config object
        DataLoggerConfig.DataEntry[] entries = {new DataLoggerConfig.DataEntry(mDLConfigPath)};
        DataLoggerConfig config = new DataLoggerConfig(new DataLoggerConfig.Config(new DataLoggerConfig.DataEntries(entries)));
        String jsonConfig = new Gson().toJson(config,DataLoggerConfig.class);

        Log.d(LOG_TAG, "Config request: " + jsonConfig);
        getMDS().put(configUri, jsonConfig, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                updateDataLoggerUI();
                Log.i(LOG_TAG, "PUT config succesful: " + data);
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "PUT DataLogger/Config returned error: " + e);
            }
        });
    }

    private void fetchDataLoggerState() {
        // Access the DataLogger/State
        String stateUri = MessageFormat.format(URI_DATALOGGER_STATE, connectedSerial);

        getMDS().get(stateUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "GET state succesful: " + data);

                mDLState = new Gson().fromJson(data, DataLoggerState.class);
                updateDataLoggerUI();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "GET DataLogger/State returned error: " + e);
            }
        });
    }

    private boolean mPathSelectionSetInternally = false;
    private void fetchDataLoggerConfig() {
        // Access the DataLogger/State
        String stateUri = MessageFormat.format(URI_DATALOGGER_CONFIG, connectedSerial);
        mDLConfigPath=null;

        getMDS().get(stateUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "GET DataLogger/Config succesful: " + data);

                DataLoggerConfig config = new Gson().fromJson(data, DataLoggerConfig.class);
                Spinner spinner = (Spinner)findViewById(R.id.path_spinner);
                for (DataLoggerConfig.DataEntry de : config.content.dataEntries.dataEntry)
                {
                    Log.d(LOG_TAG, "DataEntry: " + de.path);

                    String dePath = de.path;
                    if (dePath.contains("{"))
                    {
                        dePath = dePath.substring(0,dePath.indexOf('{'));
                        Log.d(LOG_TAG, "dePath: " + dePath);

                    }
                    // Start searching for item from 1 since 0 is the default text for empty selection
                    for (int i=1; i<spinner.getAdapter().getCount(); i++)
                    {
                        String path = spinner.getItemAtPosition(i).toString();
                        Log.d(LOG_TAG, "spinner.path["+ i+"]: " + path);
                        // Match the beginning (skip the part with samplerate parameter)
                        if (path.toLowerCase().startsWith(dePath.toLowerCase()))
                        {
                            mPathSelectionSetInternally = true;
                            Log.d(LOG_TAG, "mPathSelectionSetInternally to #"+ i);

                            spinner.setSelection(i);
                            mDLConfigPath =path;
                            break;
                        }
                    }
                }
                // If no match found, set to first item (/Meas/Acc/13)
                if (mDLConfigPath == null)
                {
                    Log.d(LOG_TAG, "no match found, set to first item");

                    spinner.setSelection(0);
                }

                fetchDataLoggerState();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "GET DataLogger/Config returned error: " + e);
                fetchDataLoggerState();
            }
        });
    }

    private void setDataLoggerState(final boolean bStartLogging) {
        // Access the DataLogger/State
        String stateUri = MessageFormat.format(URI_DATALOGGER_STATE, connectedSerial);
        final Context me = this;
        int newState = bStartLogging ? 3 : 2;
        String payload = "{\"newState\":" + newState + "}";
        getMDS().put(stateUri, payload, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "PUT DataLogger/State state succesful: " + data);

                mDLState.content = newState;
                updateDataLoggerUI();
                // Update log list if we stopped
                if (!bStartLogging)
                    refreshLogList();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "PUT DataLogger/State returned error: " + e);

                if (e.getStatusCode()==423 && bStartLogging) {
                    // Handle "LOCKED" from NAND variant
                    new AlertDialog.Builder(me)
                            .setTitle("DataLogger Error")
                            .setMessage("Can't start logging due to error 'locked'. Possibly too low battery on the sensor.")
                            .show();

                }

            }
        });
    }

    public void onStartLoggingClicked(View view) {
        setDataLoggerState(true);
    }

    public void onStopLoggingClicked(View view) {
        setDataLoggerState(false);
    }

    private void refreshLogList() {
        // Access the /Logbook/Entries
        String entriesUri = MessageFormat.format(URI_MDS_LOGBOOK_ENTRIES, connectedSerial);

        getMDS().get(entriesUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "GET LogEntries succesful: " + data);

                MdsLogbookEntriesResponse entriesResponse = new Gson().fromJson(data, MdsLogbookEntriesResponse.class);
                findViewById(R.id.buttonRefreshLogs).setEnabled(true);

                mLogEntriesArrayList.clear();
                for (MdsLogbookEntriesResponse.LogEntry logEntry : entriesResponse.logEntries) {
                    Log.d(LOG_TAG, "Entry: " + logEntry);
                    mLogEntriesArrayList.add(logEntry);
                }
                mLogEntriesArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "GET LogEntries returned error: " + e);
            }
        });
    }

    private MdsLogbookEntriesResponse.LogEntry findLogEntry(final int id)
    {
        MdsLogbookEntriesResponse.LogEntry entry = null;
        for (MdsLogbookEntriesResponse.LogEntry e : mLogEntriesArrayList) {
            if ((e.id == id)) {
                entry = e;
                break;
            }
        }
        return entry;
    }

    public void onRefreshLogsClicked(View view) {
        refreshLogList();
    }

    public void onEraseLogsClicked(View view) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Erase Logs")
                .setMessage("Are you sure you want to wipe all logbook entries?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        eraseAllLogs();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    private void eraseAllLogs() {
        // Access the Logbook/Entries resource
        String entriesUri = MessageFormat.format(URI_LOGBOOK_ENTRIES, connectedSerial);

        findViewById(R.id.buttonStartLogging).setEnabled(false);
        findViewById(R.id.buttonStopLogging).setEnabled(false);
        findViewById(R.id.buttonRefreshLogs).setEnabled(false);

        getMDS().delete(entriesUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "DELETE LogEntries succesful: " + data);
                refreshLogList();
                updateDataLoggerUI();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "DELETE LogEntries returned error: " + e);
                refreshLogList();
                updateDataLoggerUI();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG,"onDestroy()");

        // Leave datalogger logging
        DataLoggerActivity.s_INSTANCE = null;

        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView != findViewById(R.id.path_spinner))
            return;
        Log.d(LOG_TAG, "Path selected: " + adapterView.getSelectedItem().toString() + ", i: "+ i);
        mDLConfigPath = (i==0) ? null : adapterView.getSelectedItem().toString();
        // Only update config if UI selection was not set by the code (result of GET /Config)
        if (mDLConfigPath != null &&
                !mPathSelectionSetInternally &&
                adapterView.getSelectedItemPosition()>0)
        {
            Log.d(LOG_TAG, "Calling configureDataLogger:" + mDLConfigPath);
            configureDataLogger();
        }
        mPathSelectionSetInternally = false;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Log.i(LOG_TAG, "Nothing selected");

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent != findViewById(R.id.listViewLogbookEntries))
            return;

        MdsLogbookEntriesResponse.LogEntry entry = mLogEntriesArrayList.get(position);


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                .setTitle("Choose Format: Json/RAW")
                .setMessage("Json uses a lot of RAM on phone (may crash if runs out), RAW you need to convert with sbem2json or your own parser afterwards.")
                .setPositiveButton("JSon", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                fetchLogEntry(entry.id, false);
                            }
                        }
                )
                .setNeutralButton("RAW", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                fetchLogEntry(entry.id, true);
                            }
                        }
                );
        alertDialogBuilder.show();
    }

    private void fetchLogEntry(final int id, boolean bRAW) {
        findViewById(R.id.headerProgress).setVisibility(View.VISIBLE);

        final MdsLogbookEntriesResponse.LogEntry entry = findLogEntry(id);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setIndeterminate(!bRAW);

        if (!bRAW) {
            fetchLogWithMDSProxy(id, entry);
        }
        else {
            fetchLogWithLogbookDataSub(id, entry);
        }
    }
    private MdsSubscription dataSub;
    private boolean bAlreadyLogSaved = false;
    private void fetchLogWithLogbookDataSub(int id, MdsLogbookEntriesResponse.LogEntry entry) {
        // GET the /Mem/Logbook/ direct url
        String logDataResourceUri = MessageFormat.format(URI_LOGBOOK_DATA, id);
        StringBuilder sb = new StringBuilder();
        String strContract = sb.append("{\"Uri\": \"").append(connectedSerial).append(logDataResourceUri).append("\"}").toString();
        Log.d(LOG_TAG, strContract);

        final Context me = this;
        final long logGetStartTimestamp = new Date().getTime();
        final long totalBytes = entry.size;
        dataSub = null;
        bAlreadyLogSaved = false;
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(0);

        final String filename =new StringBuilder()
                .append("MovesenseLog_").append(id).append(" ")
                .append(entry.getDateStr()).append(".sbem").toString();

        File tempFile = null;
        try {
            tempFile = File.createTempFile("MovesenseSBEMLog", ".sbem", getExternalCacheDir());
            Log.d(LOG_TAG, "tempFile: " + tempFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG,"Error creating temp file", e);
            return;
        }
        try {
            final FileOutputStream fos = new FileOutputStream(tempFile);

            File finalTempFile = tempFile;
            dataSub = getMDS().subscribe(URI_EVENTLISTENER, strContract, new MdsNotificationListener() {
                private long receivedBytes=0;
                @Override
                public void onNotification(String dataJson) {
                    Log.d(LOG_TAG,"DataNotification Json: " + dataJson);
                    Gson gson = new Gson();
                    Map map = gson.fromJson(dataJson, Map.class);
                    Map body = (Map)map.get("Body");
                    long startOffset = ((Double)body.get("offset")).longValue();
                    ArrayList<Double> dataArray =(ArrayList<Double>) body.get("bytes");
                    if (startOffset > totalBytes)
                    {
                        // Some data was skipped, show error and unsubscribe
                        Log.e(LOG_TAG, "DATA SKIPPED. finishing...");
                        dataSub.unsubscribe();
                        findViewById(R.id.headerProgress).setVisibility(View.GONE);
                    }
                    if (dataArray.size() == 0) {
                        // Close file
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Error closing temp file", e);
                        }

                        if (bAlreadyLogSaved)
                            return;

                        // Log end marker. Finish and show save dialog
                        dataSub.unsubscribe();

                        findViewById(R.id.headerProgress).setVisibility(View.GONE);
                        final long logGetEndTimestamp = new Date().getTime();
                        final float speedKBps = (float) entry.size / (logGetEndTimestamp-logGetStartTimestamp) / 1024.0f * 1000.f;
                        Log.i(LOG_TAG, "GET Log Data succesful. size: " + entry.size + ", speed: " + speedKBps);

                        final String message = new StringBuilder()
                                .append("Downloaded log #").append(id).append(" from the Movesense sensor.")
                                .append("\n").append("Size:  ").append(entry.size).append(" bytes")
                                .append("\n").append("Speed: ").append(speedKBps).append(" kB/s")
                                .append("\n").append("\n").append("File will be saved in location you choose.")
                                .toString();

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(me)
                                .setTitle("Save Log Data")
                                .setMessage(message).setPositiveButton("Save to Phone", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                saveLogToFile_SBEM(finalTempFile, filename);
                                            }
                                        }
                                );

                        findViewById(R.id.headerProgress).setVisibility(View.GONE);
                        alertDialogBuilder.show();

                        bAlreadyLogSaved = true;
                    }
                    else
                    {

                        try {
                            for(Double d : dataArray)
                            {
                                byte b = (byte)d.intValue();
                                fos.write(b);
                            }
                        } catch (IOException e) {
                            Log.e(LOG_TAG,"Error writing data to file", e);
                            dataSub.unsubscribe();
                            findViewById(R.id.headerProgress).setVisibility(View.GONE);
                            return;
                        }
                        receivedBytes += dataArray.size();
                        long percent = receivedBytes * 100 / totalBytes;
                        progressBar.setProgress((int)percent);
                    }
                }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "GET Log Data returned error: " + e);
                findViewById(R.id.headerProgress).setVisibility(View.GONE);
            }
        });
        } catch (IOException e) {
            Log.e(LOG_TAG,"Error writing to temp file", e);
            return;
        }
    }

    private void fetchLogWithMDSProxy(int id, MdsLogbookEntriesResponse.LogEntry entry) {
        // GET the /MDS/Logbook/Data proxy
        String logDataUri = MessageFormat.format(URI_MDS_LOGBOOK_DATA, connectedSerial, id);
        final Context me = this;
        final long logGetStartTimestamp = new Date().getTime();

        final String filename =new StringBuilder()
                .append("MovesenseLog_").append(id).append(" ")
                .append(entry.getDateStr()).append(".json").toString();

        // MDS stores downloaded files in android Files-dir
        final File tempFile = new File(this.getFilesDir(), filename);

        // Use ToFile parameter to save directly to file and do streaming json conversion (saves memory)
        final String strGetLogDataParameters = "{\"ToFile\":\"" + filename + "\"}";

        getMDS().get(logDataUri, strGetLogDataParameters, new MdsResponseListener() {
            @Override
            public void onSuccess(final String data) {
                final long logGetEndTimestamp = new Date().getTime();
                final float speedKBps = (float) entry.size / (logGetEndTimestamp-logGetStartTimestamp) / 1024.0f * 1000.f;
                Log.i(LOG_TAG, "GET Log Data succesful. size: " + entry.size + ", speed: " + speedKBps);

                final String message = new StringBuilder()
                        .append("Downloaded log #").append(id).append(" from the Movesense sensor.")
                        .append("\n").append("Size:  ").append(entry.size).append(" bytes")
                        .append("\n").append("Speed: ").append(speedKBps).append(" kB/s")
                        .append("\n").append("\n").append("File will be saved in location you choose.")
                        .toString();

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(me)
                        .setTitle("Save Log Data")
                        .setMessage(message).setPositiveButton("Save to Phone", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        saveLogToFile_Json(tempFile, filename);
                                    }
                                }
                        );

                findViewById(R.id.headerProgress).setVisibility(View.GONE);
                alertDialogBuilder.show();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "GET Log Data returned error: " + e);
                findViewById(R.id.headerProgress).setVisibility(View.GONE);
            }
        });
    }

    private File mDataFileToCopy;
    private static int CREATE_FILE = 1;
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == CREATE_FILE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected. The original filename is in mDataFilenameToWriteFile
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                copyFileToFileUri(uri, mDataFileToCopy.getAbsolutePath());
                mDataFileToCopy = null;
            }
        }
    }
    private void copyFileToFileUri(Uri outputUri, String inputFilePath)
    {
        // Save data to the selected output file
        Log.d(LOG_TAG, "Copying file data from " + inputFilePath + " to uri: " + outputUri);

        File inputFile = new File(inputFilePath);

        try
        {
            OutputStream outputStream = getContentResolver().openOutputStream(outputUri);

            InputStream inputStream = new FileInputStream(inputFile);
            // Write in pieces in case the file is big
            final int BLOCK_SIZE= 4096;
            byte buffer[] = new byte[BLOCK_SIZE];
            int length;
            int total=0;
            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
                total += length;
                Log.d(LOG_TAG, "Bytes written: " + total);
            }

            outputStream.close();
            inputStream.close();

        } catch (IOException e) {
            Log.e(LOG_TAG, "error in creating a file:", e);
            e.printStackTrace();
        }
        finally {
            inputFile.delete();
        }
    }

    private void saveLogToFile_SBEM(File file, String filename) {
        mDataFileToCopy = file;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octetstream");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        startActivityForResult(intent, CREATE_FILE);
    }

    private void saveLogToFile_Json(File file, String filename) {
        mDataFileToCopy = file;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        startActivityForResult(intent, CREATE_FILE);
    }

    public void onCreateNewLogClicked(View view) {
        createNewLog();
    }

    private void createNewLog() {
        // Access the Logbook/Entries resource
        String entriesUri = MessageFormat.format(URI_LOGBOOK_ENTRIES, connectedSerial);

        getMDS().post(entriesUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "POST LogEntries succesful: " + data);
                IntResponse logIdResp = new Gson().fromJson(data, IntResponse.class);

                TextView tvLogId = (TextView)findViewById(R.id.textViewCurrentLogID);
                tvLogId.setText("" + logIdResp.content);
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "POST LogEntries returned error: " + e);
                TextView tvLogId = (TextView)findViewById(R.id.textViewCurrentLogID);
                tvLogId.setText("##");
            }
        });

    }
}
