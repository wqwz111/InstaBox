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
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<List<MediaInfo>>{
    private GridView mGridView;
    private ProgressBar mProgressBar;
    private ArrayList<Long> mDownloadIds = new ArrayList<>();
    private static MediaAdapter mMediaAdapter;
    private final int PERMISSIONS_REQUEST_READ_WRITE_EXTERNAL_STORAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_loading_media);
        mProgressBar.setVisibility(View.VISIBLE);
        mGridView = (GridView) findViewById(R.id.gv_main);
        mMediaAdapter = new MediaAdapter(this);
        mGridView.setAdapter(mMediaAdapter);
        mGridView.setOnItemClickListener(onItemClick);

        requestPermission();
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(myReciver, new IntentFilter(Constants.BROADCAST_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasPermission()) {
            getSupportLoaderManager().initLoader(0, null, MainActivity.this);
            ClipboardManager clipboardManager =
                    (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            String url = clipboardManager.hasPrimaryClip() ?
                    clipboardManager.getPrimaryClip().getItemAt(0).getText().toString() : "";
            if (!url.isEmpty() && url.matches("^https://www[.]instagram[.]com/p/.*/$")) {
                DownloaderService.startActionFoo(this, url);
            }
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReciver);
        unregisterReceiver(onComplete);
        super.onDestroy();
    }

    private boolean doesFileExist(String fileName) {
        File file = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + fileName);
        return file.exists();
    }

    private boolean hasPermission() {
        return !(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        if (!hasPermission()) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_WRITE_EXTERNAL_STORAGE);
        }
    }

    private BroadcastReceiver myReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ContentValues contentValues = intent.getParcelableExtra(Constants.MEDIA_INFO);
            String mediaUrl = contentValues.getAsString(Constants.MEDIA_URL);
            String mediaName = contentValues.getAsString(Constants.MEDIA_NAME);
            if (!doesFileExist(mediaName)) {
                download(mediaUrl,mediaName);
            }
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
            MediaInfo mediaInfo = (MediaInfo) adapterView.getItemAtPosition(i);
            String filePath = mediaInfo.getFilePath();
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
        Request request = new Request(Uri.parse(url));
        request.setTitle(fileName);
        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long id = mDownloadManager.enqueue(request);
        mDownloadIds.add(id);
    }

    @Override
    public Loader<List<MediaInfo>> onCreateLoader(int id, Bundle args) {
        return new MediaLoader(MainActivity.this);
    }

    @Override
    public void onLoadFinished(Loader<List<MediaInfo>> loader, List<MediaInfo> data) {
        mProgressBar.setVisibility(View.GONE);
        mMediaAdapter.clear();
        if (data.isEmpty()) {
            mGridView.setEmptyView(findViewById(R.id.tv_empty));
        } else {
            mMediaAdapter.addAll(data);
            mGridView.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<MediaInfo>> loader) {
        mMediaAdapter.clear();
    }

    static class MediaLoader extends AsyncTaskLoader<List<MediaInfo>> {

        MediaLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        public List<MediaInfo> loadInBackground() {
            List<MediaInfo> mediaInfos = new ArrayList<>();
            File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String s) {
                            return s.toLowerCase().contains(Constants.FILE_PREFIX);
                        }
                    });
            if (files != null && files.length != 0) {
                sortFilesByLastModifiedTime(files);
                for (File file : files) {
                    String filePath = file.getPath();
                    Bitmap bitmap = generateBitmap(filePath);
                    MediaInfo mediaInfo = new MediaInfo(bitmap, filePath);
                    mediaInfos.add(mediaInfo);
                }
            }

            return mediaInfos;
        }

        private void sortFilesByLastModifiedTime(File[] files) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    if (lhs.lastModified() < rhs.lastModified()) {
                        return 1;
                    } else if (lhs.lastModified() > rhs.lastModified()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });
        }
    }

    private static Bitmap generateBitmap(String filePath) {
        Bitmap bitmap;
        if (Utils.isVideo(filePath)) {
            bitmap = ThumbnailUtils.createVideoThumbnail(filePath,
                    MediaStore.Video.Thumbnails.MINI_KIND);
        } else {
            bitmap = BitmapFactory.decodeFile(filePath);

        }
        return bitmap;
    }
}
