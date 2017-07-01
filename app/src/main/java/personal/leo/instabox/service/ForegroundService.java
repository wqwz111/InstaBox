package personal.leo.instabox.service;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;

import personal.leo.instabox.Constants;
import personal.leo.instabox.MainActivity;
import personal.leo.instabox.R;

import static android.app.DownloadManager.Request.NETWORK_MOBILE;
import static android.app.DownloadManager.Request.NETWORK_WIFI;

public class ForegroundService extends Service {
    private static final String PREFERENCES_NAME = "settings";
    private static final String PREF_KEY_NETWORK = "network";
    private static final String PREF_KEY_AUTO_DOWNLOAD = "auto_download";
    private SharedPreferences mSettings;
    private ArrayList<Long> mDownloadIds = new ArrayList<>();
    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ContentValues contentValues = intent.getParcelableExtra(Constants.MEDIA_INFO);
            String mediaUrl = contentValues.getAsString(Constants.MEDIA_URL);
            String mediaName = contentValues.getAsString(Constants.MEDIA_NAME);
            download(mediaUrl, mediaName);
        }
    };
    private BroadcastReceiver myReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopForeground(true);
            stopSelf();
        }
    };
    private BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long enqueueId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (mDownloadIds.contains(enqueueId)) {
                clearClipboard();
                Intent resultIntent = new Intent(Constants.BROADCAST_ACTION_RELOAD_MEDIA);
                LocalBroadcastManager.getInstance(ForegroundService.this)
                        .sendBroadcast(resultIntent);
                mDownloadIds.remove(enqueueId);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(myReceiver,
                        new IntentFilter(Constants.BROADCAST_ACTION));
        registerReceiver(myReceiver2,
                new IntentFilter(Constants.BROADCAST_ACTION_STOP_SERVICE));
        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        mSettings = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stopIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(Constants.BROADCAST_ACTION_STOP_SERVICE),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Action stopAction = new Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_stop_grey_24dp),
                getString(R.string.notification_stop),
                stopIntent)
                .build();

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.foreground_description))
                .setSmallIcon(R.drawable.smallicon)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(stopAction)
                .build();

        startForeground(101, notification);

        final ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(new ClipboardManager
                .OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                String clipStr = clipboardManager.getPrimaryClip().getItemAt(0)
                        .getText().toString();
                boolean allowedAutoDownload =
                        mSettings.getBoolean(PREF_KEY_AUTO_DOWNLOAD, false);
                if (!clipStr.isEmpty()
                        && clipStr.matches("^https://www[.]instagram[.]com/p/.*/$")) {
                    if (allowedAutoDownload) {
                        DownloaderService.startActionFoo(ForegroundService.this, clipStr);
                    } else {
                        Intent intent = new Intent(Constants.BROADCAST_ACTION_SHOW_DOWNLOAD_BUTTON);
                        LocalBroadcastManager
                                .getInstance(ForegroundService.this)
                                .sendBroadcast(intent);
                    }
                }
            }
        });

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        unregisterReceiver(myReceiver2);
        unregisterReceiver(onComplete);
        super.onDestroy();
    }

    private void clearClipboard() {
        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
    }

    private void download(String url, String fileName) {
        DownloadManager mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (!doesRequestExist(mDownloadManager, url)) {
            boolean allowedOverMetered = mSettings.getBoolean(PREF_KEY_NETWORK, true);
            int networkType = NETWORK_WIFI;
            if (allowedOverMetered) {
                networkType = NETWORK_WIFI | NETWORK_MOBILE;
            }
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
