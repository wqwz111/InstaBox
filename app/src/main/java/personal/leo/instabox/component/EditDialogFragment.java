package personal.leo.instabox.component;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import personal.leo.instabox.R;

public class EditDialogFragment extends DialogFragment {
    public static final String KEY_ARG_URI = "uri";

    private Context mContext;
    private WallpaperManager mWallpaperManager;
    private Uri mUri;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mUri = getArguments().getParcelable(KEY_ARG_URI);
        mWallpaperManager = WallpaperManager.getInstance(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = getActivity().getLayoutInflater().inflate(R.layout.edit_dialog, null);
        TextView tvSetWallpaper = (TextView) view.findViewById(R.id.tv_set_wallpaper);
        tvSetWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = mWallpaperManager.getCropAndSetWallpaperIntent(
                        getImageContentUri(mContext, mUri));
                startActivity(intent);
                dismiss();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }

    Uri getImageContentUri(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{uri.getPath()}, null);

        assert cursor != null;
        cursor.moveToFirst();
        int id = cursor.getInt(cursor
                .getColumnIndex(MediaStore.MediaColumns._ID));
        cursor.close();
        Uri baseUri = Uri.parse("content://media/external/images/media");
        return Uri.withAppendedPath(baseUri, "" + id);
    }
}
