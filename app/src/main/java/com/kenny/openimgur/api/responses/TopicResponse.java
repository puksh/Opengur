package com.puksh.pokenimgur.api.responses;

import android.support.annotation.NonNull;

import com.puksh.pokenimgur.classes.ImgurTopic;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/11/15.
 */
public class TopicResponse extends BaseResponse {
    @NonNull
    public List<ImgurTopic> data = new ArrayList<>();
}
