package com.catfish.bean;

import net.sf.json.JSONArray;

import java.io.Serializable;

/**
 * Created by A on 2017/3/27.
 */
public class JSONArrayWrapper implements Serializable {

    private JSONArray jsonArray;

    public JSONArrayWrapper(JSONArray jsonArray){
        this.jsonArray=jsonArray;
    }

    public JSONArray getJsonArray() {
        return jsonArray;
    }

    public void setJsonArray(JSONArray jsonArray) {
        this.jsonArray = jsonArray;
    }

    @Override
    public String toString() {
        return "JSONArrayWrapper{" +
                "jsonArray=" + jsonArray +
                '}';
    }
}
