package com.zhjf.osmdroid.entity.style;

/**
 * Created by Administrator on 2017/9/19.
 */

public class Style {

    private int color;
    private float size;
    private float width;
    private int fillColor;
    private float outlineWidth;
    private int outlineColor;

    public Style() {
    }

    /**
     * Point
     *
     * @param color
     * @param size
     */
    public Style(int color, float size) {
        this.color = color;
        this.size = size;
    }

    /**
     * Line
     *
     * @param size
     * @param color
     */
    public Style(float size, int color) {
        this.color = color;
        this.size = size;
    }

    /**
     * Polygon
     *
     * @param fillColor
     * @param outlineWidth
     * @param outlineColor
     */
    public Style(int fillColor, float outlineWidth, int outlineColor) {
        this.fillColor = fillColor;
        this.outlineWidth = outlineWidth;
        this.outlineColor = outlineColor;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public int getFillColor() {
        return fillColor;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

    public float getOutlineWidth() {
        return outlineWidth;
    }

    public void setOutlineWidth(float outlineWidth) {
        this.outlineWidth = outlineWidth;
    }

    public int getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(int outlineColor) {
        this.outlineColor = outlineColor;
    }
}
