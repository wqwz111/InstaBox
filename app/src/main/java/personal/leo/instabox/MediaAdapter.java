package personal.leo.instabox;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.io.File;

/**
 * Created by leo on 17-2-23.
 */

class MediaAdapter extends ArrayAdapter<File> {

    MediaAdapter(Context context) {
        super(context, -1);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.thumbnail_item,parent,false);
            viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.iv_thumbnail);
            viewHolder.playIcon = (ImageView) convertView.findViewById(R.id.iv_icon_play);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        File file = getItem(position);

        if (file != null) {
            int visibility;
            viewHolder.thumbnail.setTag(file.getPath());
            viewHolder.thumbnail.setImageBitmap(null);
            new BitmapTask(viewHolder.thumbnail).execute(file.getPath());
            if (Utils.isVideo(file.getPath())) {
                visibility = View.VISIBLE;
            } else {
                visibility = View.INVISIBLE;
            }
            viewHolder.playIcon.setVisibility(visibility);
        }
        return convertView;
    }

    private class ViewHolder {
        ImageView thumbnail;
        ImageView playIcon;
    }

    private class BitmapTask extends AsyncTask<String, Void, MediaInfo> {

        private ImageView mImageView;

        BitmapTask(ImageView imageView) {
            mImageView = imageView;
        }

        @Override
        protected MediaInfo doInBackground(String... params) {
            String filepath = params[0];
            Bitmap bitmap = generateBitmap(filepath);
            return new MediaInfo(bitmap, filepath);
        }

        @Override
        protected void onPostExecute(MediaInfo mediaInfo) {
            String filePath = (String) mImageView.getTag();
            if (filePath.equals(mediaInfo.getFilePath())) {
                mImageView.setImageBitmap(mediaInfo.getBitmap());
            }
        }

        private Bitmap generateBitmap(String filePath) {
            Bitmap bitmap;
            if (Utils.isVideo(filePath)) {
                bitmap = ThumbnailUtils.createVideoThumbnail(filePath,
                        MediaStore.Video.Thumbnails.MINI_KIND);
            } else {
                BitmapFactory.Options bo = new BitmapFactory.Options();
                bo.inSampleSize = 3;
                bitmap = BitmapFactory.decodeFile(filePath, bo);
            }
            return bitmap;
        }
    }
}
