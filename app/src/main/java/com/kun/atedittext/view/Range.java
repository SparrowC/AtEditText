package com.kun.atedittext.view;

/**
 * Created by kun on 2016/12/23.
 */

//helper class to record the position of mention string in EditText
public class Range {
    int from;
    int to;

    public Range(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public boolean isWrappedBy(int start, int end) {
        return (start > from && start < to) || (end > from && end < to);
    }

    public boolean contains(int start, int end) {
        return from <= start && to >= end;
    }

    public boolean isEqual(int start, int end) {
        return (from == start && to == end) || (from == end && to == start);
    }

    public int getAnchorPosition(int value) {
        if ((value - from) - (to - value) >= 0) {
            return to;
        } else {
            return from;
        }
    }
}
