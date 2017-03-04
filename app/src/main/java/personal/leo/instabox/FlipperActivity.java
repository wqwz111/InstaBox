package personal.leo.instabox;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import java.io.File;

import personal.leo.instabox.component.EditDialogFragment;
import personal.leo.instabox.component.MediaFlipper;

public class FlipperActivity extends Activity
        implements LoaderManager.LoaderCallbacks<File[]>, MediaFlipper.Callbacks {
    private static final String KEY_POSITION = "position";

    private MediaFlipper mMediaFlipper;
    private Uri mUri;
    private int mPosition;
    private boolean mIsVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flipper);

        mMediaFlipper = (MediaFlipper) findViewById(R.id.mf_main);

        if (savedInstanceState != null) {
            mPosition = savedInstanceState.getInt(KEY_POSITION);
        } else {
            mPosition = getIntent().getIntExtra(KEY_POSITION, 0);
        }
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsVideo) {
            mMediaFlipper.getFlipperAdapter().resumeIfVideoPaused();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsVideo) {
            mMediaFlipper.getFlipperAdapter().pauseIfVideoPlaying();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_POSITION, mPosition);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.flipper_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.menu_item_edit);
        menuItem.setVisible(!mIsVideo);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share: {
                Intent intent = getShareIntent(mMediaFlipper.getDisplayedChildUri());
                startActivity(Intent.createChooser(intent, getString(R.string.share_title)));
                break;
            }
            case R.id.menu_item_edit: {
                EditDialogFragment editDialogFragment = new EditDialogFragment();
                Bundle args = new Bundle();
                args.putParcelable(EditDialogFragment.KEY_ARG_URI, mUri);
                editDialogFragment.setArguments(args);
                editDialogFragment.show(getFragmentManager(), "edit");
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<File[]> onCreateLoader(int id, Bundle args) {
        return new MediaLoader(FlipperActivity.this);
    }

    @Override
    public void onLoadFinished(Loader<File[]> loader, File[] data) {
        mMediaFlipper.refreshData(data);
        mMediaFlipper.setSelection(mPosition);
        mUri = mMediaFlipper.getDisplayedChildUri();
        mIsVideo = Utils.isVideo(mUri.getPath());
        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<File[]> loader) {
        mMediaFlipper.clearData();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mMediaFlipper.setOnTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private Intent getShareIntent(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        String type;
        if (Utils.isVideo(uri.getPath())) {
            type = "video/*";
        } else {
            type = "image/*";
        }
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType(type);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public void onFlipCompleted(int position, Uri uri) {
        mPosition = position;
        mUri = uri;
        mIsVideo = Utils.isVideo(uri.getPath());
        invalidateOptionsMenu();
    }
}
