package com.puksh.pokenimgur.api.responses;

import android.support.annotation.NonNull;

import com.puksh.pokenimgur.classes.ImgurTrophy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 1/21/16.
 */
public class TrophyResponse extends BaseResponse {
    public Data data;

    public static class Data {
        @NonNull
        public List<ImgurTrophy> trophies = new ArrayList<>();
    }
}
