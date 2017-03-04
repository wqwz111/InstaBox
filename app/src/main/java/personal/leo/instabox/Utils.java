package personal.leo.instabox;


public class Utils {
    public static boolean isVideo(String filePath) {
        String type = filePath.substring(filePath.lastIndexOf("."));
        return type.equals(".mp4");
    }
}
