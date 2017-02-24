package personal.leo.instabox;

import android.graphics.Bitmap;

/**
 * Created by leo on 17-2-23.
 */
public class MediaInfo {
    private Bitmap bitmap;
    private String filePath;

    public MediaInfo(Bitmap bitmap, String filePath) {
        this.bitmap = bitmap;
        this.filePath = filePath;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public String getFilePath() {
        return filePath;
    }
}
