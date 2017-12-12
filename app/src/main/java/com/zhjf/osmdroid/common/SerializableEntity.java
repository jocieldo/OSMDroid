package com.zhjf.osmdroid.common;

import com.zhjf.osmdroid.entity.AttributeEntity;

import java.util.List;

/**
 * Created by Administrator on 2017/11/22.
 */

public class SerializableEntity {
    private List<AttributeEntity> attributeEntities;

    public List<AttributeEntity> getAttributeEntities() {
        return attributeEntities;
    }

    public void setAttributeEntities(List<AttributeEntity> attributeEntities) {
        this.attributeEntities = attributeEntities;
    }
}
