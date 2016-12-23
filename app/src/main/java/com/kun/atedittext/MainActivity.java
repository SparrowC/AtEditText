package com.kun.atedittext;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import com.kun.atedittext.view.AtStruct;
import com.kun.atedittext.view.MentionEditText;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private int mId;
    private MentionEditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        final AtEditText editText = (AtEditText) findViewById(R.id.edit_text);
//        editText.addInputKeyListener("@", new AtEditText.InputKeyListener() {
//            @Override
//            public void onInputKeyWords(String key, int start) {
//                editText.setKeyWordsSpan("ABC", start, new ClickableSpan() {
//                    @Override
//                    public void onClick(View view) {
//                        Log.e("AtEditText", "click");
//                    }
//                });
//            }
//        });
        mId = 0;

        mEditText = ((MentionEditText) findViewById(R.id.edit_text));
//        List<String> mentionList = editText.getMentionList(true); //get a list of mention string
        mEditText.setMentionTextColor(Color.RED); //optional, set highlight color of mention string
        mEditText.setPattern("@[\\u4e00-\\u9fa5\\w\\-]+"); //optional, set regularExpression
        mEditText.setOnMentionInputListener(new MentionEditText.OnMentionInputListener() {
            @Override
            public void onMentionCharacterInput(int index) {
                //call when '@' character is inserted into EditText
                mEditText.addMentionText(new AtStruct(AtStruct.TYPE_AT, "abc", "00" + mId++)
                        , index, new MyClickSpan("00" + mId));
            }
        });
        mEditText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void onClick(View view) {
        for (AtStruct atStruct : mEditText.getAtStructList()) {
            Log.e(TAG, "atStruct:" + atStruct.getText() + "  id:" + atStruct.getId());
        }
    }

    static class MyClickSpan extends ClickableSpan {
        private String id;

        public MyClickSpan(String id) {
            this.id = id;
        }

        @Override
        public void onClick(View view) {
            Log.e("click", "id :" + id);
        }
    }
}
