package com.kenny.openimgur.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.OpengurApp;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

public class CustomMediaController extends FrameLayout {
    private static final int SHOW_PROGRESS = 1;
    private static final int FADE_OUT = 2;
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final int SEEK_AMOUNT = 10000;

    private MediaPlayerControl mPlayer;
    private Context mContext;
    private ViewGroup mAnchor;
    private View mRoot;
    private SeekBar mProgress;
    private TextView mEndTime;
    private TextView mCurrentTime;
    private boolean mShowing;
    private boolean mDragging;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private ImageButton mPauseButton;
    private ImageButton mRewindButton;
    private ImageButton mFastForwardButton;
    private ImageButton mExternalButton;
    private boolean mIsReleasing;
    private boolean mWasPlayingBeforeDrag;
    private String mVideoUri;

    private MessageHandler mHandler = new MessageHandler(this);

    public interface MediaPlayerControl {
        void start();
        void pause();
        int getDuration();
        int getCurrentPosition();
        void seekTo(int pos);
        boolean isPlaying();
        int getBufferPercentage();
        boolean canPause();
        boolean canSeekBackward();
        boolean canSeekForward();
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<CustomMediaController> mController;

        MessageHandler(CustomMediaController controller) {
            mController = new WeakReference<CustomMediaController>(controller);
        }

        @Override
        public void handleMessage(Message msg) {
            CustomMediaController controller = mController.get();
            if (controller == null) {
                return;
            }

            switch (msg.what) {
                case FADE_OUT:
                    controller.hide();
                    break;
                case SHOW_PROGRESS:
                    controller.setProgress();
                    if (!controller.mDragging && controller.mShowing && controller.mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000);
                    }
                    break;
            }
        }
    }

    public CustomMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    public CustomMediaController(Context context) {
        super(context);
        mContext = context;
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!mIsReleasing) {
            if (mHandler != null) {
                mHandler.removeMessages(SHOW_PROGRESS);
                mHandler.removeMessages(FADE_OUT);
            }
            mShowing = false;
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        if (mRoot != null) {
            initControllerView(mRoot);
        }
    }

    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    public void setVideoUri(String uri) {
        mVideoUri = uri;
    }

    public void setAnchorView(ViewGroup view) {
        if (mAnchor != null && mShowing) {
            hide();
        }
        
        mAnchor = view;

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        frameParams.gravity = Gravity.BOTTOM;

        removeAllViews();
        View v = makeControllerView();
        if (v != null) {
            addView(v, frameParams);
        }
    }

    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.custom_media_controller, null);

        initControllerView(mRoot);

        return mRoot;
    }

    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mRewindButton = (ImageButton) v.findViewById(R.id.rew);
        if (mRewindButton != null) {
            mRewindButton.setOnClickListener(mRewindListener);
        }

        mFastForwardButton = (ImageButton) v.findViewById(R.id.ffwd);
        if (mFastForwardButton != null) {
            mFastForwardButton.setOnClickListener(mFastForwardListener);
        }

        mExternalButton = (ImageButton) v.findViewById(R.id.open_external);
        if (mExternalButton != null) {
            mExternalButton.setOnClickListener(mExternalListener);
        }

        mProgress = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
        }

        mEndTime = (TextView) v.findViewById(R.id.time);
        mCurrentTime = (TextView) v.findViewById(R.id.time_current);
    }

    public void show() {
        show(DEFAULT_TIMEOUT);
    }

    public void show(int timeout) {
        if (!mShowing && mAnchor != null) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }

            try {
                if (mAnchor.indexOfChild(this) == -1) {
                    FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM
                    );
                    mAnchor.addView(this, tlp);
                }
                mShowing = true;
            } catch (Exception e) {
                return;
            }
        }
        updatePausePlay();

        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public boolean isShowing() {
        return mShowing;
    }

    public void hide() {
        if (mAnchor != null && mShowing && !mIsReleasing) {
            mShowing = false;
            try {
                mHandler.removeMessages(SHOW_PROGRESS);
                if (getParent() != null) {
                    mAnchor.removeView(this);
                }
            } catch (Exception ex) {
            }
        }
    }

    public void release() {
        if (mIsReleasing) {
            return;
        }
        mIsReleasing = true;
        
        if (mHandler != null) {
            mHandler.removeMessages(SHOW_PROGRESS);
            mHandler.removeMessages(FADE_OUT);
        }
        
        if (mShowing && mAnchor != null && getParent() != null) {
            try {
                mAnchor.removeView(this);
            } catch (Exception ex) {
            }
        }
        
        mShowing = false;
        mPlayer = null;
        mAnchor = null;
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                mProgress.setMax(duration);
                mProgress.setProgress(position);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress((int) ((duration * (long) percent) / 100));
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show(DEFAULT_TIMEOUT);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(DEFAULT_TIMEOUT);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN && (
                keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                        keyCode == KeyEvent.KEYCODE_SPACE)) {
            doPauseResume();
            show(DEFAULT_TIMEOUT);
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            hide();
            return true;
        }
        show(DEFAULT_TIMEOUT);
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(DEFAULT_TIMEOUT);
        }
    };

    private View.OnClickListener mRewindListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            int pos = mPlayer.getCurrentPosition();
            pos -= SEEK_AMOUNT;
            mPlayer.seekTo(pos);
            setProgress();

            show(DEFAULT_TIMEOUT);
        }
    };

    private View.OnClickListener mFastForwardListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            int pos = mPlayer.getCurrentPosition();
            pos += SEEK_AMOUNT;
            mPlayer.seekTo(pos);
            setProgress();

            show(DEFAULT_TIMEOUT);
        }
    };

    private View.OnClickListener mExternalListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mVideoUri == null) {
                return;
            }

            try {
                File videoFile = new File(mVideoUri);
                android.net.Uri contentUri = FileProvider.getUriForFile(mContext, OpengurApp.AUTHORITY, videoFile);
                
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, "video/*");
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                mContext.startActivity(intent);
            } catch (IllegalArgumentException e) {
            } catch (android.content.ActivityNotFoundException e) {
            }
        }
    };

    private void updatePausePlay() {
        if (mRoot == null || mPauseButton == null) {
            return;
        }

        if (mPlayer != null && mPlayer.isPlaying()) {
            mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            mPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);

            mDragging = true;
            mHandler.removeMessages(SHOW_PROGRESS);
            
            if (mPlayer != null && mPlayer.isPlaying()) {
                mWasPlayingBeforeDrag = true;
                mPlayer.pause();
            } else {
                mWasPlayingBeforeDrag = false;
            }
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mPlayer == null) {
                return;
            }

            if (!fromuser) {
                return;
            }

            if (mDragging) {
                mPlayer.seekTo(progress);
            }
            
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime(progress));
        }

        public void onStopTrackingTouch(SeekBar bar) {
            final int finalPosition = bar.getProgress();
            
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPlayer.seekTo(finalPosition);
                    
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mWasPlayingBeforeDrag && mPlayer != null) {
                                mPlayer.start();
                                mWasPlayingBeforeDrag = false;
                            }
                            mDragging = false;
                            updatePausePlay();
                            mHandler.sendEmptyMessage(SHOW_PROGRESS);
                            show(DEFAULT_TIMEOUT);
                        }
                    }, 150);
                }
            }, 50);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mRewindButton != null) {
            mRewindButton.setEnabled(enabled && mPlayer != null && mPlayer.canSeekBackward());
        }
        if (mFastForwardButton != null) {
            mFastForwardButton.setEnabled(enabled && mPlayer != null && mPlayer.canSeekForward());
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }
}
