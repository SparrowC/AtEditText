package com.kun.atedittext;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

import com.kun.atedittext.view.AtStruct;

import java.util.ArrayList;
import java.util.List;

/**
 * Identify the word input by keyboard only.
 * Created by kun on 2016/12/22.
 */

public class AtEditText extends EditText implements TextWatcher {
    private String mKey;
    private InputKeyListener mInputKeyListener;
    private int mColor;
    private List<AtStruct> mAtStructList;

    public AtEditText(Context context) {
        super(context);
        init();
    }

    public AtEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AtEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mAtStructList = new ArrayList<>();
        mColor = Color.RED;
        addTextChangedListener(this);
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (lengthAfter == 1) {
            if (TextUtils.equals(mKey, text.subSequence(start, start + lengthAfter)) && mInputKeyListener != null) {
                mInputKeyListener.onInputKeyWords(mKey, start);
            }

        }
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

    @Override
    public void beforeTextChanged(CharSequence text, int start, int count/*被改变的旧内容数*/, int after/*改变后的内容数量*/) {
        Log.e("AtEditText", "curText:" + text + "  start: " + start + " count:" + count + " after:" + after);

        if (after == 0 && after < count && count == 1) {
            // TODO: 删除文本
            int index = getSelectionStart();
            int len = 0;
            for (AtStruct atStruct : mAtStructList) {
                len += atStruct.getLength();
                if (index < len) {
                    changedEditable = getText();
                    if (atStruct.getType() == AtStruct.TYPE_AT) {
                        changedEditable.delete(len - atStruct.getLength(), len);
                        isTextChanged = true;
                        mAtStructList.remove(atStruct);
                        break;
                    } else if (atStruct.getType() == AtStruct.TYPE_TEXT) {
                        changedEditable.delete(index - 1, index);
                        isTextChanged = true;
                        StringBuffer buffer = new StringBuffer(atStruct.getText());
                        int s = index - (len - atStruct.getLength()) - 1;
                        buffer.delete(s, s + 1);
                        if (buffer.length() == 0) {
                            mAtStructList.remove(atStruct);
                            break;
                        }
                        atStruct.setText(buffer.toString());
                        break;
                    }
                }
            }

        }
    }

    Editable changedEditable;
    boolean isTextChanged = false;

    @Override
    public void afterTextChanged(Editable editable) {
        if (isTextChanged) {
            setText(changedEditable);
            isTextChanged = false;
        }
    }

    public void setKeyWordsSpan(String text) {
        setKeyWordsSpan(text, this.getSelectionStart(), null);
    }

    //添加到光标所在到位置
    public void setKeyWordsSpan(String text, ClickableSpan clickableSpan) {
        setKeyWordsSpan(text, this.getSelectionStart(), clickableSpan);
    }

    public void setKeyWordsSpan(String text, int start, ClickableSpan clickableSpan) {
        int count = mAtStructList.size();
        String t = getEditableText().toString();
        if (!TextUtils.isEmpty(t)) {
            if (count <= 0) {//abc{@ab}
                AtStruct atStruct = new AtStruct();
                atStruct.setText(t);
                atStruct.setType(AtStruct.TYPE_TEXT);
            } else {
                int len = getStringLength(mAtStructList);
                if (t.length() > len) {//abc{@ab}aa
                    mAtStructList.add(new AtStruct(AtStruct.TYPE_TEXT, t.substring(len, t.length()), ""));
                }
            }
        }
        AtStruct atStruct = new AtStruct();
        atStruct.setType(AtStruct.TYPE_AT);
        atStruct.setText(text);
        mAtStructList.add(atStruct);
        SpannableString spannableString = new SpannableString(text);
        int length = text.length();
        spannableString.setSpan(clickableSpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(mColor);
        spannableString.setSpan(colorSpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        append(spannableString);
    }

    private int getStringLength(List<AtStruct> atStructList) {
        int length = 0;
        for (AtStruct atStruct : atStructList) {
            length += atStruct == null ? 0 : atStruct.getLength();
        }
        return length;
    }

    public void addInputKeyListener(String key, InputKeyListener inputKeyListener) {
        mKey = key;
        mInputKeyListener = inputKeyListener;
    }

    public interface InputKeyListener {
        void onInputKeyWords(String key, int start);
    }
}
