package personal.leo.instabox;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;


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
            Glide.with(getContext()).load(file).into(viewHolder.thumbnail);
            if (Utils.isVideo(file.getPath())) {
                viewHolder.playIcon.setVisibility(View.VISIBLE);
            } else {
                viewHolder.playIcon.setVisibility(View.INVISIBLE);
            }
        }
        return convertView;
    }

    private class ViewHolder {
        ImageView thumbnail;
        ImageView playIcon;
    }
}
