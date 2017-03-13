package personal.leo.instabox;


public class Utils {
    private static final String VIDEO_TYPE = ".mp4";
    private static final String DOT = ".";

    public static boolean isVideo(String filePath) {
        return filePath.substring(filePath.lastIndexOf(DOT)).equals(VIDEO_TYPE);
    }
}
