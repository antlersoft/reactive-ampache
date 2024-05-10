package com.antoniotari.reactiveampache.models;

import org.simpleframework.xml.Text;

public class RateResponse extends BaseResponse {
    //@Text(required = false)
    private String responseText;

    public String getResponseText() {
        return responseText;
    }

    public RateResponse() {
        responseText = "";
    }
}
