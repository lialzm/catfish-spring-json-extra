package com.catfish.bean;

import java.io.Serializable;

/**
 * Created by A on 2017/3/27.
 */
public class Body implements Serializable {
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Body{" +
                "id=" + id +
                '}';
    }
}
