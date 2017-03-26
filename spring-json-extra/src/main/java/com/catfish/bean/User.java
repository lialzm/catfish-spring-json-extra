package com.catfish.bean;

import java.io.Serializable;

/**
 * Created by A on 2017/3/17.
 */
public class User implements Serializable {

    private String id;

    private Integer age;

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }



    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                '}';
    }
}
