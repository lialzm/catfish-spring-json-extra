package com.catfish.bean;

import net.sf.json.JSONObject;

import java.io.Serializable;

/**
 * Created by A on 2017/3/27.
 */
public class JSONWrapper implements Serializable {

    private JSONObject jsonObject;

    public JSONWrapper(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JSONObject getJSONObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public String toString() {
        return "JSONObjectWrapper{" +
                "jsonObject=" + jsonObject +
                '}';
    }
}
