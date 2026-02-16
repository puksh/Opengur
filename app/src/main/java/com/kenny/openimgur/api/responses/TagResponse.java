package com.kenny.openimgur.api.responses;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kenny.openimgur.classes.ImgurTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/12/15.
 */
public class TagResponse extends BaseResponse {
    private static final Gson GSON = new Gson();

    public JsonElement data;

    // Tags are returned in an array inside a data object,Boooo
    public static class Data {
        @NonNull
        public List<ImgurTag> tags = new ArrayList<>();
    }

    @NonNull
    public List<ImgurTag> getTags() {
        if (data == null || data.isJsonNull()) {
            return new ArrayList<>(0);
        }

        try {
            if (data.isJsonArray()) {
                List<ImgurTag> tags = GSON.fromJson(data, new TypeToken<List<ImgurTag>>() {
                }.getType());
                return tags != null ? tags : new ArrayList<ImgurTag>(0);
            }

            if (data.isJsonObject()) {
                JsonObject dataObject = data.getAsJsonObject();
                JsonElement tagsElement = dataObject.get("tags");

                if (tagsElement != null && tagsElement.isJsonArray()) {
                    List<ImgurTag> tags = GSON.fromJson(tagsElement, new TypeToken<List<ImgurTag>>() {
                    }.getType());
                    return tags != null ? tags : new ArrayList<ImgurTag>(0);
                }
            }
        } catch (Exception ignored) {
            // Return an empty list on malformed payloads to avoid crashing callers.
        }

        return new ArrayList<>(0);
    }
}
