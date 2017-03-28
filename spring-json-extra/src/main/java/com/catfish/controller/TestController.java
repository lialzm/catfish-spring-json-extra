package com.catfish.controller;

import com.catfish.bean.Body;
import com.catfish.bean.JSONArrayWrapper;
import com.catfish.bean.JSONObjectWrapper;
import com.catfish.bean.JSONWrapper;
import com.catfish.spring.support.JsonRequest;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by A on 2017/3/17.
 */
@Controller
public class TestController {


    private Logger logger = LoggerFactory.getLogger(getClass());


    @RequestMapping("/getValue")
    @ResponseBody
    public String getParameterByPath(@RequestParam String id) {
        return  id;
    }

    @RequestMapping("/getParameterByPath")
    @ResponseBody
    public String getParameterByPath(@JsonRequest("$.id") Long id, @JsonRequest("$.name") String name) {
        return id + "," + name;
    }

    @RequestMapping("/getParameterByDefaultKey")
    @ResponseBody
    public String getParameterByDefaultKey(@JsonRequest Long id, @JsonRequest String name) {
        return id + "," + name;
    }
    @RequestMapping("/getParameterByDefaultValue")
    @ResponseBody
    public String getParameterByDefaultValue(@JsonRequest(defaultValue = "0") Long id, @JsonRequest String name) {
        return id + "," + name;
    }

    @RequestMapping("/getJsonObject")
    @ResponseBody
    public String getJsonObject(@JsonRequest JSONObjectWrapper body) {
        logger.info(body.toString());
        return body.getJSONObject().getString("id");
    }

    @RequestMapping("/getAllJsonObject")
    @ResponseBody
    public String getAllJsonObject(@JsonRequest JSONWrapper name) {
        logger.info(name.toString());
        return name.getJSONObject().getString("id");
    }

    @RequestMapping("/getBean")
    @ResponseBody
    public String getUserByJson7(@JsonRequest Body body) {
        return String.valueOf(body.getId());
    }

    @RequestMapping("/getJsonArray")
    @ResponseBody
    public String getJsonArray(@JsonRequest JSONArrayWrapper array) {
        JSONObject jsonObject = array.getJsonArray().getJSONObject(0);
        return jsonObject.getString("name");
    }

    @RequestMapping("/getJsonArrayAndBean")
    @ResponseBody
    public String getJsonArrayAndBean(@JsonRequest JSONArrayWrapper array,@JsonRequest Body body) {
        JSONObject jsonObject = array.getJsonArray().getJSONObject(0);
        return jsonObject.getString("name")+","+body.getId();
    }



}
