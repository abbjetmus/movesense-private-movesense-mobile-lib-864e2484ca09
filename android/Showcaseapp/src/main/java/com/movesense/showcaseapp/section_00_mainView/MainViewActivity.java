package com.movesense.showcaseapp.section_00_mainView;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.movesense.showcaseapp.BuildConfig;
import com.movesense.showcaseapp.R;
import com.movesense.showcaseapp.google_drive.SendLogsToGoogleDriveActivity;
import com.movesense.showcaseapp.section_01_movesense.MovesenseActivity;
import com.movesense.showcaseapp.section_02_multi_connection.connection.MultiConnectionActivity;
import com.movesense.showcaseapp.section_03_dfu.DfuActivity2;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainViewActivity extends AppCompatActivity {

    private final String TAG = MainViewActivity.class.getSimpleName();

    @BindView(R.id.mainView_movesense_Ll) RelativeLayout mMainViewMovesenseLl;
    @BindView(R.id.mainView_multiConnection_Ll) RelativeLayout mMainViewMultiConnectionLl;
    @BindView(R.id.mainView_dfu_Ll) RelativeLayout mMainViewDfuLl;
    @BindView(R.id.mainView_savedData_Ll) RelativeLayout mMainViewSavedDataLl;
    @BindView(R.id.mainView_appVersion_tv) TextView mMainViewAppVersionTv;
    @BindView(R.id.mainView_libraryVersion_tv) TextView mMainViewLibraryVersionTv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);
        ButterKnife.bind(this);

        String versionName = BuildConfig.VERSION_NAME;
        String libraryVersion = BuildConfig.MDS_VERSION;

        mMainViewAppVersionTv.setText(getString(R.string.application_version, versionName));
        mMainViewLibraryVersionTv.setText(getString(R.string.library_version, libraryVersion));

    }

    @OnClick({R.id.mainView_movesense_Ll, R.id.mainView_multiConnection_Ll, R.id.mainView_dfu_Ll, R.id.mainView_savedData_Ll})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.mainView_movesense_Ll:
                startActivity(new Intent(MainViewActivity.this, MovesenseActivity.class));
                break;
            case R.id.mainView_multiConnection_Ll:
                startActivity(new Intent(MainViewActivity.this, MultiConnectionActivity.class));
                break;
            case R.id.mainView_dfu_Ll:
                startActivity(new Intent(MainViewActivity.this, DfuActivity2.class));
                break;
            case R.id.mainView_savedData_Ll:
                startActivity(new Intent(MainViewActivity.this, SendLogsToGoogleDriveActivity.class));
                break;
        }
    }
}
