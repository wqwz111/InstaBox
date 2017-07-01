package personal.leo.instabox;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;

import java.io.File;

import personal.leo.instabox.component.SettingsDialogFragment;
import personal.leo.instabox.service.DownloaderService;
import personal.leo.instabox.service.ForegroundService;

public class MainActivity extends Activity
        implements LoaderManager.LoaderCallbacks<File[]>,
        SettingsDialogFragment.SettingsDialogListener{
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    private static final String PREFERENCES_NAME = "settings";
    private static final String PREF_KEY_NETWORK = "network";
    private static final String PREF_KEY_AUTO_DOWNLOAD = "auto_download";
    private static MediaAdapter mMediaAdapter;
    private Button mBtnDownload;
    private GridView mGridView;
    private SharedPreferences mSettings;
    private String mUrl;
    private boolean mShouldShowDownloadBtn;
    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mShouldShowDownloadBtn = false;
            if (mBtnDownload.getVisibility() == View.VISIBLE) {
                mBtnDownload.setVisibility(View.INVISIBLE);
            }
            getLoaderManager().getLoader(0).onContentChanged();
        }
    };
    private BroadcastReceiver myReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mShouldShowDownloadBtn = true;
            if (mBtnDownload.getVisibility() == View.INVISIBLE) {
                mBtnDownload.setVisibility(View.VISIBLE);
            }
        }
    };
    private AdapterView.OnItemClickListener onItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Intent intent = new Intent(MainActivity.this, FlipperActivity.class);
            intent.putExtra("position", i);
            startActivity(intent);
        }
    };
    private AbsListView.OnScrollListener onScollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {
            if (i == SCROLL_STATE_IDLE && mShouldShowDownloadBtn) {
                mBtnDownload.setVisibility(View.VISIBLE);
            } else if (mBtnDownload.getVisibility() != View.INVISIBLE) {
                mBtnDownload.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onScroll(AbsListView absListView, int i, int i1, int i2) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnDownload = (Button) findViewById(R.id.btn_download);
        mGridView = (GridView) findViewById(R.id.gv_main);
        mMediaAdapter = new MediaAdapter(this);
        mGridView.setAdapter(mMediaAdapter);
        mGridView.setOnItemClickListener(onItemClick);
        mGridView.setOnScrollListener(onScollListener);

        mSettings = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(myReceiver,
                        new IntentFilter(Constants.BROADCAST_ACTION_RELOAD_MEDIA));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(myReceiver2,
                        new IntentFilter(Constants.BROADCAST_ACTION_SHOW_DOWNLOAD_BUTTON));
        Intent intent = new Intent(MainActivity.this, ForegroundService.class);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasPermission()) {
            getLoaderManager().initLoader(0, null, MainActivity.this);
            mUrl = checkClipboard();
            if (mSettings.getBoolean(PREF_KEY_AUTO_DOWNLOAD, true)) {
                fetchRemoteMediaInfo();
            } else if (!mUrl.isEmpty() && mUrl.matches("^https://www[.]instagram[.]com/p/.*/$")) {
                mShouldShowDownloadBtn = true;
                mBtnDownload.setVisibility(View.VISIBLE);
            }
        } else {
            requestPermission();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(myReceiver);
        manager.unregisterReceiver(myReceiver2);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings: {
                showSettingsDialog();
                break;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if ((grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_DENIED)
                    || grantResults.length == 0) {
                finish();
            }
        }
    }

    private void showSettingsDialog() {
        SettingsDialogFragment settingsDialogFragment = new SettingsDialogFragment();
        settingsDialogFragment.show(getFragmentManager(), "settings");
    }

    private boolean hasPermission() {
        return !(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    private String checkClipboard() {
        ClipboardManager clipboardManager =
                (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        return clipboardManager.hasPrimaryClip() ?
                clipboardManager.getPrimaryClip().getItemAt(0).getText().toString() : "";
    }

    private void fetchRemoteMediaInfo() {
        if (!mUrl.isEmpty() && mUrl.matches("^https://www[.]instagram[.]com/p/.*/$")) {
            DownloaderService.startActionFoo(this, mUrl);
        }
    }

    @Override
    public Loader<File[]> onCreateLoader(int id, Bundle args) {
        return new MediaLoader(MainActivity.this);
    }

    @Override
    public void onLoadFinished(Loader<File[]> loader, File[] data) {
        mMediaAdapter.clear();
        if (data == null || data.length == 0) {
            mGridView.setEmptyView(findViewById(R.id.tv_empty));
        } else {
            mMediaAdapter.addAll(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<File[]> loader) {
        mMediaAdapter.clear();
    }

    @Override
    public void onDismiss(boolean allowedOverMetered, boolean allowedAutoDownload) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(PREF_KEY_AUTO_DOWNLOAD, allowedAutoDownload);
        editor.putBoolean(PREF_KEY_NETWORK, allowedOverMetered);
        editor.apply();
    }

    public void onDownloadBtnClick(View view) {
        fetchRemoteMediaInfo();
    }
}
