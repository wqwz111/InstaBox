package personal.leo.instabox;

import android.graphics.Bitmap;

/**
 * Created by leo on 17-2-23.
 */
class MediaInfo {
    private Bitmap bitmap;
    private String filePath;

    MediaInfo(Bitmap bitmap, String filePath) {
        this.bitmap = bitmap;
        this.filePath = filePath;
    }

    Bitmap getBitmap() {
        return bitmap;
    }

    String getFilePath() {
        return filePath;
    }
}
