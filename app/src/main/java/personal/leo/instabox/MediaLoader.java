package personal.leo.instabox;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

public class MediaLoader extends AsyncTaskLoader<File[]> {

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
