package personal.leo.instabox;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import java.util.ArrayList;

import personal.leo.instabox.component.SettingsDialogFragment;
import personal.leo.instabox.service.DownloaderService;

import static android.app.DownloadManager.Request.NETWORK_MOBILE;
import static android.app.DownloadManager.Request.NETWORK_WIFI;

public class MainActivity extends Activity
        implements LoaderManager.LoaderCallbacks<File[]>,
        SettingsDialogFragment.SettingsDialogListener{
    private Button mBtnDownload;
    private GridView mGridView;
    private SharedPreferences mSettings;
    private ArrayList<Long> mDownloadIds = new ArrayList<>();

    private String mUrl;
    private boolean mShouldShowDownloadBtn;

    private static MediaAdapter mMediaAdapter;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    private static final String PREFERENCES_NAME = "settings";
    private static final String PREF_KEY_NETWORK = "network";
    private static final String PREF_KEY_AUTO_DOWNLOAD = "auto_download";

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
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(myReceiver, new IntentFilter(Constants.BROADCAST_ACTION));
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        unregisterReceiver(onComplete);
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

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ContentValues contentValues = intent.getParcelableExtra(Constants.MEDIA_INFO);
            String mediaUrl = contentValues.getAsString(Constants.MEDIA_URL);
            String mediaName = contentValues.getAsString(Constants.MEDIA_NAME);
            download(mediaUrl, mediaName);
        }
    };

    private BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long enqueueId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (mDownloadIds.contains(enqueueId)) {
                clearClipboard();
                mShouldShowDownloadBtn = false;
                if (mBtnDownload.getVisibility() == View.VISIBLE) {
                    mBtnDownload.setVisibility(View.INVISIBLE);
                }
                getLoaderManager().getLoader(0).onContentChanged();
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

    private void download(String url, String fileName) {
        DownloadManager mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (!doesRequestExist(mDownloadManager, url)) {
            boolean allowedOverMetered = mSettings.getBoolean(PREF_KEY_NETWORK, true);
            int networkType = NETWORK_WIFI;
            if (allowedOverMetered) {
                networkType = NETWORK_WIFI | NETWORK_MOBILE;
            }
            Request request = new Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedNetworkTypes(networkType);

            long id = mDownloadManager.enqueue(request);
            mDownloadIds.add(id);
        }
    }

    private boolean doesRequestExist(DownloadManager downloadManager, String url) {
        boolean result = false;
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL
            | DownloadManager.STATUS_PENDING
            | DownloadManager.STATUS_RUNNING);
        Cursor cursor = downloadManager.query(query);
        while (cursor.moveToNext()) {
            int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
            String uri = cursor.getString(uriIndex);
            if (uri.equals(url)) {
                result = true;
                break;
            }
        }
        cursor.close();
        return result;
    }

    private String checkClipboard() {
        ClipboardManager clipboardManager =
                (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        return clipboardManager.hasPrimaryClip() ?
                clipboardManager.getPrimaryClip().getItemAt(0).getText().toString() : "";
    }

    private void clearClipboard() {
        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
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
