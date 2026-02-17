package com.kenny.openimgur.classes;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.net.TrafficStats;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by kcampagna on 10/9/14.
 */
public class VideoCache {
    private static final String TAG = "VideoCache";

    private static final int TRAFFIC_STATS_TAG_VIDEO = 0xF0A2;

    private static VideoCache mInstance;

    private File mCacheDir;

    private Md5FileNameGenerator mKeyGenerator;

    public static VideoCache getInstance() {
        if (mInstance == null) {
            mInstance = new VideoCache();
        }

        return mInstance;
    }

    private VideoCache() {
        OpengurApp app = OpengurApp.getInstance();
        String cacheKey = app.getPreferences().getString(SettingsActivity.KEY_CACHE_LOC, SettingsActivity.CACHE_LOC_INTERNAL);
        StrictMode.ThreadPolicy policy = allowDiskAccess();

        try {
            File dir = ImageUtil.getCacheDirectory(app.getApplicationContext(), cacheKey);
            mCacheDir = new File(dir, "video_cache");
            mCacheDir.mkdirs();
        } finally {
            StrictMode.setThreadPolicy(policy);
        }

        mKeyGenerator = new Md5FileNameGenerator();
    }

    public void setCacheDirectory(File dir) {
        mCacheDir = new File(dir, "video_cache");
        StrictMode.ThreadPolicy policy = allowDiskAccess();

        try {
            mCacheDir.mkdirs();
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    /**
     * Downloads and saves the video file to the cache
     *
     * @param url      The url of the video
     * @param listener Optional VideoCacheListener
     */
    public void putVideo(@Nullable String url, @Nullable VideoCacheListener listener) {
        if (TextUtils.isEmpty(url)) {
            Exception e = new IllegalArgumentException("Url is null");
            LogUtil.e(TAG, "Invalid url", e);
            if (listener != null) listener.onVideoDownloadFailed(e, url);
            return;
        }

        new DownloadVideo(url, listener).execute();
    }

    /**
     * Returns the cached video file for the given url. NULL if it does not exist
     *
     * @param url
     * @return
     */
    public File getVideoFile(String url) {
        if (TextUtils.isEmpty(url)) return null;

        String ext = getExtension(url);
        if (TextUtils.isEmpty(ext)) return null;

        String key = mKeyGenerator.generate(url);
        File file = new File(mCacheDir, key + ext);
        StrictMode.ThreadPolicy policy = allowDiskAccess();

        try {
            return FileUtil.isFileValid(file) ? file : null;
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    public void deleteCache() {
        StrictMode.ThreadPolicy policy = allowDiskAccess();

        try {
            FileUtil.deleteDirectory(mCacheDir);
            mCacheDir.mkdirs();
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    public long getCacheSize() {
        StrictMode.ThreadPolicy policy = allowDiskAccess();

        try {
            return FileUtil.getDirectorySize(mCacheDir);
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    public interface VideoCacheListener {
        // Called when the Video download starts
        void onVideoDownloadStart(String key, String url);

        // Called when the video download fails
        void onVideoDownloadFailed(Exception ex, String url);

        // Called when the video download completes
        void onVideoDownloadComplete(File file);

        void onProgress(int downloaded, int total);
    }

    private class DownloadVideo extends AsyncTask<Void, Integer, Object> {
        private final String mKey;

        private final VideoCacheListener mListener;

        private final String mOriginalUrl;

        private final String mDownloadUrl;

        public DownloadVideo(String url, VideoCacheListener listener) {
            mKey = mKeyGenerator.generate(url);
            mListener = listener;
            mOriginalUrl = url;
            mDownloadUrl = url.endsWith(".gifv") ? url.replace(".gifv", ".mp4") : url;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mListener != null) mListener.onVideoDownloadStart(mKey, mDownloadUrl);
        }

        @Override
        protected Object doInBackground(Void... values) {
            InputStream in = null;
            BufferedOutputStream buffer = null;
            LogUtil.v(TAG, "Downloading video from " + mDownloadUrl);
            File writeFile = null;
            TrafficStats.setThreadStatsTag(TRAFFIC_STATS_TAG_VIDEO);

            try {
                String ext = getExtension(mOriginalUrl);

                if (TextUtils.isEmpty(ext)) {
                    return new IllegalArgumentException("Invalid extension for url " + mOriginalUrl);
                }

                writeFile = new File(mCacheDir, mKey + ext);

                if (FileUtil.isFileValid(writeFile)) {
                    LogUtil.v(TAG, "File already exists, deleting existing file and replacing it");
                    writeFile.delete();
                }

                writeFile.createNewFile();

                if (!FileUtil.isFileValid(writeFile)) {
                    return new FileNotFoundException("Unable to create file for download");
                }

                URL url = new URL(mDownloadUrl);
                URLConnection connection = url.openConnection();
                connection.connect();
                in = connection.getInputStream();
                buffer = new BufferedOutputStream(new FileOutputStream(writeFile));
                byte byt[] = new byte[8192];
                int i;
                int total = 0;
                int size = connection.getContentLength();

                for (long l = 0L; (i = in.read(byt)) != -1; l += i) {
                    total += i;
                    buffer.write(byt, 0, i);
                    publishProgress(total, size);
                }

                buffer.flush();
                return writeFile;
            } catch (Exception e) {
                LogUtil.e(TAG, "An error occurred whiling downloading video", e);
                if (writeFile != null) writeFile.delete();
                return e;
            } finally {
                FileUtil.closeStream(in);
                FileUtil.closeStream(buffer);
                TrafficStats.clearThreadStatsTag();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values != null && values.length == 2) {
                if (mListener != null) mListener.onProgress(values[0], values[1]);
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            if (o instanceof File) {
                LogUtil.v(TAG, "Video downloaded successfully to " + ((File) o).getAbsolutePath());
                if (mListener != null) {
                    mListener.onVideoDownloadComplete((File) o);
                }
            } else if (mListener != null) {
                mListener.onVideoDownloadFailed((Exception) o, mDownloadUrl);
            }
        }
    }

    @Nullable
    private String getExtension(@NonNull String url) {
        if (url.endsWith(".gifv") || url.endsWith(".mp4")) {
            return ".mp4";
        } else if (url.endsWith(".webm")) {
            return ".webm";
        }

        return null;
    }

    private StrictMode.ThreadPolicy allowDiskAccess() {
        StrictMode.ThreadPolicy policy = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(policy)
                .permitDiskReads()
                .permitDiskWrites()
                .build());
        return policy;
    }
}
