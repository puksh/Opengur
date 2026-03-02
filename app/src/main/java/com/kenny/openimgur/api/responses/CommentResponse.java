package com.puksh.pokenimgur.api.responses;

import com.puksh.pokenimgur.classes.ImgurComment;

import java.util.List;

/**
 * Created by kcampagna on 7/11/15.
 */
public class CommentResponse extends BaseResponse {
    public List<ImgurComment> data;

    public boolean hasComments() {
        return data != null && !data.isEmpty();
    }
}
