package personal.leo.instabox.component;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterViewFlipper;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;

import personal.leo.instabox.R;
import personal.leo.instabox.Utils;

public class MediaFlipper extends AdapterViewFlipper
        implements GestureDetector.OnGestureListener {

    private GestureDetector mGestureDetector;
    private FlipperAdapter mFlipperAdapter;
    private Callbacks mListener;

    public interface Callbacks {
        void onFlipCompleted(int position, Uri uri);
    }

    public MediaFlipper(Context context) {
        super(context);
        init();
    }

    public MediaFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MediaFlipper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        try {
            mListener = (Callbacks) getContext();
        } catch (ClassCastException e) {
            throw new ClassCastException(getContext().toString()
                    + " must implement SettingsDialogListener");
        }
        mGestureDetector = new GestureDetector(getContext(), this);
        mFlipperAdapter = new FlipperAdapter(getContext());
        this.setAdapter(mFlipperAdapter);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getX() - e2.getX() > 140) {
            mFlipperAdapter.stopPlayIfVideoPlaying();
            this.setInAnimation(getContext(), android.R.animator.fade_in);
            this.setOutAnimation(getContext(), android.R.animator.fade_out);
            this.showNext();
            mListener.onFlipCompleted(this.getDisplayedChild(), getDisplayedChildUri());
        } else if (e1.getX() - e2.getX() < -140) {
            mFlipperAdapter.stopPlayIfVideoPlaying();
            this.setInAnimation(getContext(), android.R.animator.fade_in);
            this.setOutAnimation(getContext(), android.R.animator.fade_out);
            this.showPrevious();
            mListener.onFlipCompleted(this.getDisplayedChild(), getDisplayedChildUri());
        }

        return true;
    }

    public Uri getDisplayedChildUri() {
        File file = mFlipperAdapter.getItem(this.getDisplayedChild());
        return Uri.fromFile(file);
    }

    public void refreshData(File[] data) {
        mFlipperAdapter.clear();
        mFlipperAdapter.addAll(data);
    }

    public void clearData() {
        mFlipperAdapter.clear();
    }

    public FlipperAdapter getFlipperAdapter() {
        return mFlipperAdapter;
    }

    public void setOnTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
    }

    public class FlipperAdapter extends ArrayAdapter<File> {
        private ViewHolder mViewHolder;

        FlipperAdapter(Context context) {
            super(context, -1);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                mViewHolder = new ViewHolder();
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.flipper_item, parent, false);
                mViewHolder.imageView = (ImageView) convertView.findViewById(R.id.iv_detail);
                mViewHolder.videoView = (TextureVideoView) convertView.findViewById(R.id.vv_detail);
                convertView.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) convertView.getTag();
            }
            File file = getItem(position);
            if (file != null) {
                if (Utils.isVideo(file.getPath())) {
                    mViewHolder.imageView.setVisibility(View.GONE);
                    mViewHolder.videoView.setVisibility(View.VISIBLE);
                    mViewHolder.videoView.setVideoPath(file.getPath());
                    mViewHolder.videoView.start();
                } else {
                    mViewHolder.imageView.setVisibility(View.VISIBLE);
                    mViewHolder.videoView.setVisibility(View.GONE);
                    Glide.with(getContext()).load(file).into(mViewHolder.imageView);
                }
            }
            return convertView;
        }

        public void stopPlayIfVideoPlaying() {
            if (mViewHolder.videoView.isPlaying()) {
                mViewHolder.videoView.stop();
            }
        }

        public void pauseIfVideoPlaying() {
            if (mViewHolder.videoView.isPlaying()) {
                mViewHolder.videoView.pause();
            }
        }

        public void resumeIfVideoPaused() {
            if (!mViewHolder.videoView.isPlaying()) {
                mViewHolder.videoView.resume();
            }
        }

        class ViewHolder {
            TextureVideoView videoView;
            ImageView imageView;
        }
    }
}
