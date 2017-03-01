package com.antoniotari.reactiveampache.models;

import org.simpleframework.xml.ElementList;

import java.util.List;

/**
 * Created by mike on 2/24/17.
 */

public class TagsResponse extends BaseResponse {

    @ElementList(inline = true, required = false)
    private List<TagEntity> tags;

    public List<TagEntity> getTags() {
        return tags;
    }
}
