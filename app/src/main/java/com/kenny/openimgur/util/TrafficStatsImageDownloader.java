package com.kenny.openimgur.util;

import android.content.Context;
import android.net.TrafficStats;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tags image-loader network traffic so StrictMode can attribute sockets properly.
 */
public class TrafficStatsImageDownloader extends BaseImageDownloader {
    private static final int TRAFFIC_STATS_TAG_IMAGES = 0xF0A2;

    public TrafficStatsImageDownloader(Context context) {
        super(context);
    }

    public TrafficStatsImageDownloader(Context context, int connectTimeout, int readTimeout) {
        super(context, connectTimeout, readTimeout);
    }

    @Override
    protected InputStream getStreamFromNetwork(String imageUri, Object extra) throws IOException {
        TrafficStats.setThreadStatsTag(TRAFFIC_STATS_TAG_IMAGES);

        try {
            return super.getStreamFromNetwork(imageUri, extra);
        } finally {
            TrafficStats.clearThreadStatsTag();
        }
    }
}
