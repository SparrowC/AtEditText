package com.kun.atedittext.view;

import android.text.TextUtils;

/**
 * Created by kun on 2016/12/22.
 */

public class AtStruct {
    public static int TYPE_TEXT = 0;
    public static int TYPE_AT = 1;
    int type;
    String text;
    String id;
    int length;
    Range mRange;

    public AtStruct() {
    }

    public AtStruct(int type, String text, String id) {
        this.type = type;
        this.text = text;
        length = TextUtils.isEmpty(text) ? 0 : text.length();
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        length = TextUtils.isEmpty(text) ? 0 : text.length();
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLength() {
        return length;
    }

    public Range getRange() {
        return mRange;
    }

    public void setRange(Range range) {
        mRange = range;
    }
}
