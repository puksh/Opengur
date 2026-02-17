package com.kenny.openimgur.fragments;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView.ScaleType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.FullScreenPhotoActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.PhotoResponse;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.ui.CustomMediaController;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LinkUtils;
import com.kennyc.view.MultiStateView;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import pl.droidsonroids.gif.GifDrawable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 7/19/14.
 */
public class PopupImageDialogFragment extends DialogFragment implements VideoCache.VideoCacheListener {
    private static final long PHOTO_SIZE_LIMIT = 1024 * 1024 * 5;

    private static final String KEY_URL = "url";

    private static final String KEY_ANIMATED = "animated";

    private static final String KEY_DIRECT_LINK = "direct_link";

    private static final String KEY_IS_VIDEO = "video";

    @BindView(R.id.multiView)
    MultiStateView mMultiView;

    @BindView(R.id.image)
    ImageView mImage;

    @BindView(R.id.video)
    VideoView mVideo;

    String mImageUrl;

    Unbinder mUnbinder;

    public static PopupImageDialogFragment getInstance(String url, boolean isAnimated, boolean isDirectLink, boolean isVideo) {
        PopupImageDialogFragment fragment = new PopupImageDialogFragment();
        Bundle args = new Bundle(4);
        args.putString(KEY_URL, url);
        args.putBoolean(KEY_ANIMATED, isAnimated);
        args.putBoolean(KEY_DIRECT_LINK, isDirectLink);
        args.putBoolean(KEY_IS_VIDEO, isVideo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (getDialog() == null) {
            setShowsDialog(false);
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStyle(DialogFragment.STYLE_NO_TITLE, OpengurApp.getInstance(getActivity()).getImgurTheme().getDialogTheme());
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.image_popup_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle == null || !bundle.containsKey(KEY_URL)) {
            dismiss();
            return;
        }

        mUnbinder = ButterKnife.bind(this, view);
        mImageUrl = bundle.getString(KEY_URL, null);
        boolean isAnimated = bundle.getBoolean(KEY_ANIMATED, false);
        boolean isDirectLink = bundle.getBoolean(KEY_DIRECT_LINK, true);
        boolean isVideo = bundle.getBoolean(KEY_IS_VIDEO, false);

        if (isDirectLink) {
            if (isVideo) {
                displayVideo(mImageUrl);
            } else {
                displayImage(mImageUrl, isAnimated);
            }
        } else {
            fetchImageDetails();
        }

        mImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (isTouchInsideImageContent(mImage, motionEvent.getX(), motionEvent.getY())) {
                        dismissAllowingStateLoss();
                        startActivity(FullScreenPhotoActivity.createIntent(getActivity(), mImageUrl));
                    } else {
                        dismissAllowingStateLoss();
                    }
                }

                return true;
            }
        });

        mVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                dismissAllowingStateLoss();
                mVideo.stopPlayback();
                startActivity(FullScreenPhotoActivity.createIntent(getActivity(), mImageUrl));
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVideo != null && mVideo.getDuration() > 0) {
            mVideo.start();
        }
    }

    @Override
    public void onPause() {
        if (mVideo != null && mVideo.isPlaying()) {
            mVideo.stopPlayback();
        }

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        ImageUtil.getImageLoader(getActivity()).cancelDisplayTask(mImage);
        if (mUnbinder != null) mUnbinder.unbind();
        super.onDestroyView();
    }

    /**
     * Loads the image into the ImageView
     *
     * @param url
     * @param isAnimated
     */
    public void displayImage(String url, final boolean isAnimated) {
        if (isAnimated) {
            mImage.setScaleType(ScaleType.FIT_CENTER);
            mImage.setAdjustViewBounds(true);
            loadGifAsync(url, mImage);
            return;
        }

        mImage.setScaleType(ScaleType.CENTER_CROP);
        mImage.setAdjustViewBounds(false);

        ImageUtil.getImageLoader(getActivity()).displayImage(url, mImage, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {

            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                if (isAdded()) {
                    dismissAllowingStateLoss();
                    Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                if (isAdded()) {
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                }
            }

            @Override
            public void onLoadingCancelled(String s, View view) {
                if (isAdded()) {
                    dismissAllowingStateLoss();
                    Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadGifAsync(final String url, final ImageView imageView) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                GifDrawable gifDrawable = null;

                try {
                    if (!isAdded() || getActivity() == null) {
                        return;
                    }

                    File file = DiskCacheUtils.findInCache(url, ImageUtil.getImageLoader(getActivity()).getDiskCache());

                    if (!FileUtil.isFileValid(file)) {
                        postGifLoadError();
                        return;
                    }

                    gifDrawable = new GifDrawable(file);
                } catch (IOException e) {
                    postGifLoadError();
                    return;
                }

                final GifDrawable finalGifDrawable = gifDrawable;

                if (!isAdded() || getActivity() == null) {
                    if (finalGifDrawable != null) {
                        finalGifDrawable.recycle();
                    }
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded() || getActivity() == null || imageView == null || mMultiView == null) {
                            if (finalGifDrawable != null) {
                                finalGifDrawable.recycle();
                            }
                            return;
                        }

                        imageView.setImageDrawable(finalGifDrawable);
                        mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    }
                });
            }
        }).start();
    }

    private void postGifLoadError() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || getActivity() == null) {
                    return;
                }

                Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                dismissAllowingStateLoss();
            }
        });
    }

    /**
     * Loads the video to be played
     *
     * @param url
     */
    public void displayVideo(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File file = VideoCache.getInstance().getVideoFile(url);
                final boolean isValid = FileUtil.isFileValid(file);

                if (getActivity() == null) return;

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) return;

                        if (isValid) {
                            displayVideo(file);
                        } else {
                            VideoCache.getInstance().putVideo(url, PopupImageDialogFragment.this);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * Displays the video once the file is loaded
     *
     * @param file
     */
    public void displayVideo(File file) {
        // The visibility needs to be set before the video is loaded or it won't play
        mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        mVideo.setVisibility(View.VISIBLE);
        // Needs to be set so the video is not dimmed
        mVideo.setZOrderOnTop(true);
        mImage.setVisibility(View.GONE);

        CustomMediaController mediaController = new CustomMediaController(getActivity());
        mediaController.setVideoUri(file.getAbsolutePath());
        mVideo.setCustomMediaController(mediaController);

        mVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setLooping(true);
            }
        });

        mVideo.setVideoPath(file.getAbsolutePath());
        mVideo.start();
    }

    @Override
    public void onVideoDownloadStart(String key, String url) {
        // NOOP
    }

    @Override
    public void onVideoDownloadFailed(Exception ex, String url) {
        if (isAdded() && isResumed() && getActivity() != null) {
            Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
            dismissAllowingStateLoss();
        }
    }

    @Override
    public void onVideoDownloadComplete(File file) {
        if (isAdded() && isResumed()) {
            displayVideo(file);
        }
    }

    @Override
    public void onProgress(int downloaded, int total) {

    }

    private void fetchImageDetails() {
        ApiClient.getService().getImageDetails(mImageUrl).enqueue(new Callback<PhotoResponse>() {
            @Override
            public void onResponse(Call<PhotoResponse> call, Response<PhotoResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null && response.body().data != null) {
                    ImgurPhoto photo = response.body().data;

                    if (photo.isAnimated()) {
                        boolean shouldUseVideo = photo.hasVideoLink() &&
                                (photo.isLinkAThumbnail() || photo.getSize() > PHOTO_SIZE_LIMIT || LinkUtils.isVideoLink(photo.getLink()));

                        if (shouldUseVideo) {
                            mImageUrl = photo.getVideoLink();
                            displayVideo(mImageUrl);
                        } else {
                            mImageUrl = photo.getLink();
                            displayImage(mImageUrl, photo.isAnimated());
                        }
                    } else {
                        mImageUrl = photo.getLink();
                        displayImage(mImageUrl, photo.isAnimated());
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onFailure(Call<PhotoResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                dismissAllowingStateLoss();
            }
        });
    }

    private boolean isTouchInsideImageContent(ImageView imageView, float x, float y) {
        if (imageView == null) {
            return false;
        }

        Drawable drawable = imageView.getDrawable();

        if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            return false;
        }

        RectF bounds = new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        imageView.getImageMatrix().mapRect(bounds);
        bounds.offset(imageView.getPaddingLeft(), imageView.getPaddingTop());
        return bounds.contains(x, y);
    }
}
