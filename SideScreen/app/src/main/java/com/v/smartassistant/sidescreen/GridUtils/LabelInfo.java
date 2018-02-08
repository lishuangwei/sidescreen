package com.v.smartassistant.sidescreen.GridUtils;

/**
 * Created by lishuangwei on 17-12-26.
 */

public class LabelInfo {
    private int drawable;
    private String title;
    private boolean isClick;

    public LabelInfo(int drawable, String title, boolean state) {
        this.drawable = drawable;
        this.title = title;
        this.isClick = state;
    }

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setClick(boolean state) {
        this.isClick = state;
    }

    public boolean isClick() {

        return isClick;
    }

    public int getDrawable() {
        return drawable;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "LabelInfo{" +
                "drawable=" + drawable +
                ", title='" + title + '\'' +
                ", state=" + isClick +
                '}';
    }
}
