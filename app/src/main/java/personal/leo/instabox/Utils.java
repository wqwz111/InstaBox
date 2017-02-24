package personal.leo.instabox;

/**
 * Created by leo on 17-2-23.
 */

class Utils {
    static boolean isVideo(String filePath) {
        String type = filePath.substring(filePath.lastIndexOf("."));
        return type.equals(".mp4");
    }
}
