package personal.leo.instabox;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

/**
 * Created by leo on 17-2-23.
 */

class MediaAdapter extends ArrayAdapter<MediaInfo> {
    MediaAdapter(Context context) {
        super(context,R.layout.thumbnail_item);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
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
        MediaInfo mediaInfo = getItem(position);

        if (mediaInfo != null) {
            int visibility;
            viewHolder.thumbnail.setImageBitmap(mediaInfo.getBitmap());
            if (Utils.isVideo(mediaInfo.getFilePath())) {
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
}
