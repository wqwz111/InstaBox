package personal.leo.instabox;

import android.Manifest;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<File[]>,
        SettingsDialogFragment.SettingsDialogListener{
    private GridView mGridView;
    private ProgressBar mProgressBar;
    private SharedPreferences mSettings;
    private ArrayList<Long> mDownloadIds = new ArrayList<>();
    private static MediaAdapter mMediaAdapter;


    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    private static final String PREFERENCES_NAME = "settings";
    private static final String PREF_KEY_NETWORK = "network";
    private static final String PREF_KEY_AUTO_DOWNLOAD = "auto_download";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_loading_media);
        mGridView = (GridView) findViewById(R.id.gv_main);
        mMediaAdapter = new MediaAdapter(this);
        mGridView.setAdapter(mMediaAdapter);
        mGridView.setOnItemClickListener(onItemClick);

        mSettings = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(myReciver, new IntentFilter(Constants.BROADCAST_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasPermission()) {
            getSupportLoaderManager().initLoader(0, null, MainActivity.this);
            if (mSettings.getBoolean(PREF_KEY_AUTO_DOWNLOAD, true)) {
                fetchRemoteMediaInfo();
            }
        } else {
            requestPermission();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReciver);
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
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
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
        settingsDialogFragment.show(getSupportFragmentManager(), "settings");
    }

    private boolean hasPermission() {
        return !(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    private BroadcastReceiver myReciver = new BroadcastReceiver() {
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
                getSupportLoaderManager().getLoader(0).onContentChanged();
            }
        }
    };

    private AdapterView.OnItemClickListener onItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            File file = (File) adapterView.getItemAtPosition(i);
            String filePath = file.getPath();
            if (Utils.isVideo(filePath)) {
                intent.setDataAndType(Uri.fromFile(new File(filePath)),
                        "video/*");
            } else {
                intent.setDataAndType(Uri.fromFile(new File(filePath)), "image/*");
            }
            startActivity(intent);
        }
    };

    private void download(String url, String fileName) {
        File file = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + fileName);
        if (!file.exists()) {
            boolean allowedOverMetered = mSettings.getBoolean(PREF_KEY_NETWORK, true);
            Request request = new Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedOverMetered(allowedOverMetered);

            DownloadManager mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long id = mDownloadManager.enqueue(request);
            mDownloadIds.add(id);
        }
    }

    private void fetchRemoteMediaInfo() {
        ClipboardManager clipboardManager =
                (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        String url = clipboardManager.hasPrimaryClip() ?
                clipboardManager.getPrimaryClip().getItemAt(0).getText().toString() : "";
        if (!url.isEmpty() && url.matches("^https://www[.]instagram[.]com/p/.*/$")) {
            DownloaderService.startActionFoo(this, url);
        }
    }

    @Override
    public Loader<File[]> onCreateLoader(int id, Bundle args) {
        return new MediaLoader(MainActivity.this, mProgressBar);
    }

    @Override
    public void onLoadFinished(Loader<File[]> loader, File[] data) {
        mProgressBar.setVisibility(View.GONE);
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

    public void onFabDownloadClick(View view) {
        fetchRemoteMediaInfo();
    }

    static class MediaLoader extends AsyncTaskLoader<File[]> {

        private ProgressBar mProgressBar;

        MediaLoader(Context context, ProgressBar progressBar) {
            super(context);
            mProgressBar = progressBar;
        }

        @Override
        protected void onStartLoading() {
            mProgressBar.setVisibility(View.VISIBLE);
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        public File[] loadInBackground() {
            File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String s) {
                            return s.toLowerCase().contains(Constants.FILE_PREFIX);
                        }
                    });
            if (files != null && files.length != 0) {
                sortFilesByLastModifiedTime(files);
            }
            return files;
        }

        private void sortFilesByLastModifiedTime(File[] files) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    if (lhs.lastModified() < rhs.lastModified()) {
                        return -1;
                    } else if (lhs.lastModified() > rhs.lastModified()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
        }
    }
}
