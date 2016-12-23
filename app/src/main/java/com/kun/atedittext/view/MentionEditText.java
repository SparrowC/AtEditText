package com.kun.atedittext.view;


import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * MentionEditText adds some useful features for mention string(@xxxx), such as highlight,
 * intelligent deletion, intelligent selection and '@' input detection, etc.
 * <p>
 * Created by kun on 2016/12/22.
 */

public class MentionEditText extends AppCompatEditText {
    public static final String DEFAULT_MENTION_PATTERN = "@[\\u4e00-\\u9fa5\\w\\-]+";

    private Pattern mPattern;
    private Runnable mAction;

    private int mMentionTextColor;

    private boolean mIsSelected;
    private Range mLastSelectedRange;
    private List<Range> mRangeArrayList;
    private List<AtStruct> mAtStructList;

    private OnMentionInputListener mOnMentionInputListener;

    public MentionEditText(Context context) {
        super(context);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new HackInputConnection(super.onCreateInputConnection(outAttrs), true, this);
    }

    @Override
    public void setText(final CharSequence text, BufferType type) {
        super.setText(text, type);
        //hack, put the cursor at the end of text after calling setText() method
        if (mAction == null) {
            mAction = new Runnable() {
                @Override
                public void run() {
                    setSelection(getText().length());
                }
            };
        }
        post(mAction);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        colorMentionString();
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        //avoid infinite recursion after calling setSelection()
        if (mLastSelectedRange != null && mLastSelectedRange.isEqual(selStart, selEnd)) {
            return;
        }

        //if user cancel a selection of mention string, reset the state of 'mIsSelected'
        Range closestRange = getRangeOfClosestMentionString(selStart, selEnd);
        if (closestRange != null && closestRange.to == selEnd) {
            mIsSelected = false;
        }

        Range nearbyRange = getRangeOfNearbyMentionString(selStart, selEnd);
        //if there is no mention string nearby the cursor, just skip
        if (nearbyRange == null) {
            return;
        }

        //forbid cursor located in the mention string.
        if (selStart == selEnd) {
            setSelection(nearbyRange.getAnchorPosition(selStart));
        } else {
            if (selEnd < nearbyRange.to) {
                setSelection(selStart, nearbyRange.to);
            }
            if (selStart > nearbyRange.from) {
                setSelection(nearbyRange.from, selEnd);
            }
        }
    }

    /**
     * set regularExpression
     *
     * @param pattern regularExpression
     */
    public void setPattern(String pattern) {
        mPattern = Pattern.compile(pattern);
    }

    /**
     * set highlight color of mention string
     *
     * @param color value from 'getResources().getColor()' or 'Color.parseColor()' etc.
     */
    public void setMentionTextColor(int color) {
        mMentionTextColor = color;
    }

    /**
     * get a list of mention string
     *
     * @param excludeMentionCharacter if true, return mention string with format like 'Andy' instead of "@Andy"
     * @return list of mention string
     */
    public List<String> getMentionList(boolean excludeMentionCharacter) {
        List<String> mentionList = new ArrayList<>();
        if (TextUtils.isEmpty(getText().toString())) {
            return mentionList;
        }
        Matcher matcher = mPattern.matcher(getText().toString());
        while (matcher.find()) {
            String mentionText = matcher.group();
            //tailor the mention string, using the format likes 'Andy' instead of "@Andy"
            if (excludeMentionCharacter) {
                mentionText = mentionText.substring(1);
            }
            if (!mentionList.contains(mentionText)) {
                mentionList.add(mentionText);
            }
        }
        return mentionList;
    }

    /**
     * set listener for mention character('@')
     *
     * @param onMentionInputListener MentionEditText.OnMentionInputListener
     */
    public void setOnMentionInputListener(OnMentionInputListener onMentionInputListener) {
        mOnMentionInputListener = onMentionInputListener;
    }

    private void init() {
        mRangeArrayList = new ArrayList<>(5);
        mPattern = Pattern.compile(DEFAULT_MENTION_PATTERN);
        mMentionTextColor = Color.RED;
        //disable suggestion
        setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        addTextChangedListener(new MentionTextWatcher());
        mAtStructList = new ArrayList<>();
    }

    private void colorMentionString() {
        //reset state
        mIsSelected = false;
        if (mRangeArrayList != null) {
            mRangeArrayList.clear();
        }

        Editable spannableText = getText();
        if (spannableText == null || TextUtils.isEmpty(spannableText.toString())) {
            return;
        }

        //remove previous spans
        ForegroundColorSpan[] oldSpans = spannableText.getSpans(0, spannableText.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan oldSpan : oldSpans) {
            spannableText.removeSpan(oldSpan);
        }

        //find mention string and color it
        int count = mAtStructList.size();
        if (count > 0) {
            int atStructIndex = 0;
            int lastMentionIndex = -1;
            String text = spannableText.toString();
            Matcher matcher = mPattern.matcher(text);
            while (atStructIndex < count && matcher.find()) {
                String mentionText = matcher.group();
                if (TextUtils.equals(mentionText, "@" + mAtStructList.get(atStructIndex).getText())) {
                    int start;
                    if (lastMentionIndex != -1) {
                        start = text.indexOf(mentionText, lastMentionIndex);
                    } else {
                        start = text.indexOf(mentionText);
                    }
                    int end = start + mentionText.length();
                    spannableText.setSpan(new ForegroundColorSpan(mMentionTextColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    lastMentionIndex = end;
                    //record all mention-string's position
                    Range range = new Range(start, end);
                    mRangeArrayList.add(range);
                    mAtStructList.get(atStructIndex).setRange(range);
                    atStructIndex++;
                }
            }
        }
    }

    private Range getRangeOfClosestMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (Range range : mRangeArrayList) {
            if (range.contains(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    private Range getRangeOfNearbyMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (Range range : mRangeArrayList) {
            if (range.isWrappedBy(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    public void addMentionText(AtStruct atStruct, int index, ClickableSpan clickSpan) {
        mAtStructList.add(atStruct);
        SpannableString spannableString = new SpannableString(atStruct.getText() + " ");
        int length = spannableString.length();
        spannableString.setSpan(clickSpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(mMentionTextColor), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getText().insert(index + 1, spannableString);
    }

    public List<AtStruct> getAtStructList() {
        return mAtStructList;
    }

    //text watcher for mention character('@')
    private class MentionTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int index, int i1, int count) {
            if (count == 1 && !TextUtils.isEmpty(charSequence)) {
                char mentionChar = charSequence.toString().charAt(index);
                if ('@' == mentionChar && mOnMentionInputListener != null) {
                    mOnMentionInputListener.onMentionCharacterInput(index);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    //handle the deletion action for mention string, such as '@test'
    private class HackInputConnection extends InputConnectionWrapper {
        private EditText editText;

        public HackInputConnection(InputConnection target, boolean mutable, MentionEditText editText) {
            super(target, mutable);
            this.editText = editText;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                int selectionStart = editText.getSelectionStart();
                int selectionEnd = editText.getSelectionEnd();
                Range closestRange = getRangeOfClosestMentionString(selectionStart, selectionEnd);
                if (closestRange == null) {
                    mIsSelected = false;
                    return super.sendKeyEvent(event);
                }
                //if mention string has been selected or the cursor is at the beginning of mention string, just use default action(delete)
                if (mIsSelected || selectionStart == closestRange.from) {
                    mIsSelected = false;
                    //the selected mention string is going to be delete
                    for (AtStruct atStruct : mAtStructList) {
                        if (atStruct.getRange().from == closestRange.from) {
                            mAtStructList.remove(atStruct);
                            break;
                        }
                    }
                    return super.sendKeyEvent(event);
                } else {
                    //select the mention string
                    mIsSelected = true;
                    mLastSelectedRange = closestRange;
                    setSelection(closestRange.to, closestRange.from);
                }
                return true;
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (beforeLength == 1 && afterLength == 0) {
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }

    /**
     * Listener for '@' character
     */
    public interface OnMentionInputListener {
        /**
         * call when '@' character is inserted into EditText
         *
         * @param index
         */
        void onMentionCharacterInput(int index);
    }

}