
package personal.leo.instabox.component;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

public class TextureVideoView extends TextureView implements Handler.Callback,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        TextureView.SurfaceTextureListener {

    private volatile int mCurrentState = STATE_IDLE;
    private volatile int mTargetState = STATE_IDLE;
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private static final int MSG_START = 100;
    private static final int MSG_PAUSE = 110;
    private static final int MSG_STOP = 111;

    private Uri mUri;
    private Context mContext;
    private Surface mSurface;
    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private MediaPlayerCallback mMediaPlayerCallback;
    private Handler mHandler;
    private Handler mVideoHandler;
    private boolean mHasAudio;

    private static final HandlerThread sThread = new HandlerThread("Thread_pv");

    static {
        sThread.start();
    }

    public interface MediaPlayerCallback {
        void onPrepared(MediaPlayer mp);

        void onCompletion(MediaPlayer mp);

        void onBufferingUpdate(MediaPlayer mp, int percent);

        void onVideoSizeChanged(MediaPlayer mp, int width, int height);

        boolean onInfo(MediaPlayer mp, int what, int extra);

        boolean onError(MediaPlayer mp, int what, int extra);
    }

    public TextureVideoView(Context context) {
        super(context);
        init();
    }

    public TextureVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TextureVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void scaleSize(int videoWidth, int videoHeight) {
        if (mMediaPlayer != null) {
            float sx = (float) getWidth() / videoWidth;
            float sy = (float) getHeight() / videoHeight;
            float minScale = Math.min(sx, sy);
            sx = minScale / sx;
            sy = minScale / sy;
            final Matrix matrix = new Matrix();
            matrix.setScale(sx, sy, getWidth() / 2f, getHeight() / 2f);
            if (Looper.myLooper() == Looper.getMainLooper()) {
                setTransform(matrix);
            } else {
                mHandler.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        setTransform(matrix);
                    }
                });
            }
        }
    }

    public void setMediaPlayerCallback(MediaPlayerCallback mediaPlayerCallback) {
        mMediaPlayerCallback = mediaPlayerCallback;
        if (mediaPlayerCallback == null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getDuration();
        }

        return -1;
    }

    public int getVideoHeight() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getVideoHeight();
        }
        return 0;
    }

    public int getVideoWidth() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getVideoWidth();
        }
        return 0;
    }

    @Override
    public boolean handleMessage(Message msg) {
        synchronized (TextureVideoView.class) {
            switch (msg.what) {
                case MSG_START:
                    openVideo();
                    break;
                case MSG_PAUSE:
                    if (mMediaPlayer != null) {
                        mMediaPlayer.pause();
                    }
                    mCurrentState = STATE_PAUSED;
                    break;
                case MSG_STOP:
                    release(true);
                    break;
            }
        }
        return true;
    }

    private void init() {
        if (!isInEditMode()) {
            mContext = getContext();
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
            mHandler = new Handler();
            mVideoHandler = new Handler(sThread.getLooper(), this);
            setSurfaceTextureListener(this);
        }
    }


    // release the media player in any state
    private void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
        }
    }

    private void openVideo() {
        if (mUri == null || mSurface == null || mTargetState != STATE_PLAYING) {
            // not ready for playback just yet, will try again later
            return;
        }

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnInfoListener(this);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setDataSource(mContext, mUri);
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepareAsync();

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            mTargetState = STATE_PREPARING;

            mHasAudio = true;
            try {
                MediaExtractor mediaExtractor = new MediaExtractor();
                mediaExtractor.setDataSource(mContext, mUri, null);
            } catch (Exception ex) {
                // may be failed to instantiate extractor.
            }

        } catch (IOException | IllegalArgumentException ex) {
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            if (mMediaPlayerCallback != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mMediaPlayerCallback.onError(mMediaPlayer,
                                MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
                    }
                });
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = new Surface(surface);
        if (mTargetState == STATE_PLAYING) {
            start();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurface = null;
        stop();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri) {
        mUri = uri;
    }

    public void start() {
        mTargetState = STATE_PLAYING;

        if (isInPlaybackState()) {
            mVideoHandler.obtainMessage(MSG_STOP).sendToTarget();
        }

        if (mUri != null && mSurface != null) {
            mVideoHandler.obtainMessage(MSG_START).sendToTarget();
        }
    }

    public void pause() {
        mTargetState = STATE_PAUSED;

        if (isPlaying()) {
            mVideoHandler.obtainMessage(MSG_PAUSE).sendToTarget();
        }
    }

    public void resume() {
        mTargetState = STATE_PLAYING;

        if (!isPlaying()) {
            mVideoHandler.obtainMessage(MSG_START).sendToTarget();
        }
    }

    public void stop() {
        mTargetState = STATE_PLAYBACK_COMPLETED;

        if (isInPlaybackState()) {
            mVideoHandler.obtainMessage(MSG_STOP).sendToTarget();
        }
    }

    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        mTargetState = STATE_PLAYBACK_COMPLETED;
        if (mMediaPlayerCallback != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mMediaPlayerCallback != null) {
                        mMediaPlayerCallback.onCompletion(mp);
                    }
                }
            });
        }
    }

    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        mCurrentState = STATE_ERROR;
        mTargetState = STATE_ERROR;
        if (mMediaPlayerCallback != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mMediaPlayerCallback != null) {
                        mMediaPlayerCallback.onError(mp, what, extra);
                    }
                }
            });
        }
        return true;
    }

    @Override
    public void onPrepared(final MediaPlayer mp) {
        if (mTargetState != STATE_PREPARING || mCurrentState != STATE_PREPARING) {
            return;
        }

        mCurrentState = STATE_PREPARED;

        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            mTargetState = STATE_PLAYING;
        }

        if (mMediaPlayerCallback != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mMediaPlayerCallback != null) {
                        mMediaPlayerCallback.onPrepared(mp);
                    }
                }
            });
        }
    }

    @Override
    public void onVideoSizeChanged(final MediaPlayer mp, final int width, final int height) {
        scaleSize(width, height);
        if (mMediaPlayerCallback != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mMediaPlayerCallback != null) {
                        mMediaPlayerCallback.onVideoSizeChanged(mp, width, height);
                    }
                }
            });
        }
    }

    @Override
    public void onBufferingUpdate(final MediaPlayer mp, final int percent) {
        if (mMediaPlayerCallback != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mMediaPlayerCallback != null) {
                        mMediaPlayerCallback.onBufferingUpdate(mp, percent);
                    }
                }
            });
        }
    }

    @Override
    public boolean onInfo(final MediaPlayer mp, final int what, final int extra) {
        if (mMediaPlayerCallback != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mMediaPlayerCallback != null) {
                        mMediaPlayerCallback.onInfo(mp, what, extra);
                    }
                }
            });
        }
        return true;
    }
}