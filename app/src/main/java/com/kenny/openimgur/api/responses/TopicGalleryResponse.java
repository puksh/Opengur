package com.puksh.pokenimgur.api.responses;

import android.support.annotation.NonNull;

import com.puksh.pokenimgur.classes.ImgurBaseObject;

import java.util.ArrayList;
import java.util.List;

public class TopicGalleryResponse extends BaseResponse {
    @NonNull
    public TopicData data = new TopicData();

    @NonNull
    public GalleryResponse toGalleryResponse() {
        GalleryResponse response = new GalleryResponse();
        response.success = success;
        response.status = status;

        if (data != null && data.items != null && !data.items.isEmpty()) {
            response.data.addAll(data.items);
        }

        return response;
    }

    public static class TopicData {
        @NonNull
        public List<ImgurBaseObject> items = new ArrayList<>();
    }
}