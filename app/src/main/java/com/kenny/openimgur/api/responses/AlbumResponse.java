package com.puksh.pokenimgur.api.responses;

import com.puksh.pokenimgur.classes.ImgurPhoto;

import java.util.List;

/**
 * Created by kcampagna on 7/12/15.
 */
public class AlbumResponse extends BaseResponse {
    public List<ImgurPhoto> data;

    public boolean hasData() {
        return data != null && !data.isEmpty();
    }
}
