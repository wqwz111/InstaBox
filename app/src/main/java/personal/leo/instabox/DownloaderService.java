package personal.leo.instabox;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class DownloaderService extends IntentService {
    private static final String ACTION_FOO = "personal.leo.instabox.action.FOO";

    private static final String EXTRA_URL = "personal.leo.instabox.extra.URL";

    public DownloaderService() {
        super("DownloaderService");
    }

    public static void startActionFoo(Context context, String url) {
        Intent intent = new Intent(context, DownloaderService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_URL, url);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                final String url = intent.getStringExtra(EXTRA_URL);
                handleActionFoo(url);
            }
        }
    }

    private void handleActionFoo(String urlStr) {
        try {
            ContentValues mediaInfo = new ContentValues();
            Document doc = Jsoup.connect(urlStr).timeout(5000).get();
            Document content = Jsoup.parse(doc.toString());
            String videoUrl = content.getElementsByAttributeValue("property","og:video")
                    .attr("content");
            String title = content.getElementsByAttributeValue("property","og:title")
                    .attr("content").replaceAll("[^a-zA-Z0-9\\u4E00-\\u9FA5\\s]","");

            if (!videoUrl.isEmpty()) {
                String videoName = title + ".mp4";
                mediaInfo.put(Constants.MEDIA_NAME, videoName);
                mediaInfo.put(Constants.MEDIA_URL, videoUrl);
            } else {
                String imgUrl = content.getElementsByAttributeValue("property","og:image").attr("content");
                String imgName = title + ".jpg";
                mediaInfo.put(Constants.MEDIA_NAME, imgName);
                mediaInfo.put(Constants.MEDIA_URL, imgUrl);
            }
            Intent intent = new Intent(Constants.BROADCAST_ACTION);
            intent.putExtra(Constants.MEDIA_INFO, mediaInfo);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
