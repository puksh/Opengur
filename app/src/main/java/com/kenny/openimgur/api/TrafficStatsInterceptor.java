package com.kenny.openimgur.api;

import android.net.TrafficStats;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class TrafficStatsInterceptor implements Interceptor {
    private static final int TRAFFIC_STATS_TAG_API = 0xF0A1;

    @Override
    public Response intercept(Chain chain) throws IOException {
        TrafficStats.setThreadStatsTag(TRAFFIC_STATS_TAG_API);

        try {
            return chain.proceed(chain.request());
        } finally {
            TrafficStats.clearThreadStatsTag();
        }
    }
}
