package com.zhjf.osmdroid.entity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/11/22.
 */

public class AttributeEntity implements Serializable {
    private String name;
    private Class aClass;

    public AttributeEntity() {
    }

    public AttributeEntity(String name, Class aClass) {
        this.name=name;
        this.aClass=aClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class getType() {
        return aClass;
    }

    public void setType(Class aClass) {
        this.aClass = aClass;
    }
}
