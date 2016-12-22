package com.avatar.personate;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.robot.scheduler.SchedulerManager;
import android.robot.speech.SpeechManager;
import android.robot.speech.SpeechService;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.avatar.personate.scene.PersonateScene;
import com.avatar.personate.Util;


public class PersonateActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "Activity";

    private SpeechManager mSpeechManager;
    private PersonateImpl mPersonate;

    private ListView mDialogListView;
    private FrameLayout mStateView;
    private ThinkView mThinkView;
    private ListeningView mListenView;
    private SpeakView mSpeakView;
    private DialogAdapter mDialogAdapter;

    private List<Drawable> mSpeakImageList;

    private final MyHandler mHandler = new MyHandler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ai_dialog);

        // TODO use mic-2
        SchedulerManager scm = (SchedulerManager) getSystemService(Context.SCHEDULER_SERVICE);
        scm.setMicroPhoneType(SchedulerManager.MICROPHONE_ARRAY);

        mStateView = (FrameLayout) findViewById(R.id.state_view);
        mListenView = (ListeningView) getLayoutInflater().inflate(R.layout.listening_view, null);
        mThinkView = (ThinkView) getLayoutInflater().inflate(R.layout.think_view, null);
        mSpeakView = (SpeakView) getLayoutInflater().inflate(R.layout.speak_view, null);

        Button home_1 = (Button) mThinkView.findViewById(R.id.btn_home_1);
        home_1.setOnClickListener(this);
        Button mic = (Button) mThinkView.findViewById(R.id.btn_mic);
        mic.setOnClickListener(this);
        Button question_1 = (Button) mThinkView.findViewById(R.id.btn_question_1);
        question_1.setOnClickListener(this);

        Button home_2 = (Button) mSpeakView.findViewById(R.id.btn_home_2);
        home_2.setOnClickListener(this);
        Button question_2 = (Button) mSpeakView.findViewById(R.id.btn_question_2);
        question_2.setOnClickListener(this);


        mStateView.addView(mThinkView);

        mDialogListView = (ListView) findViewById(R.id.list_dialog);
        mDialogAdapter = new DialogAdapter(this);
        mDialogListView.setAdapter(mDialogAdapter);
        mDialogListView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        mSpeechManager = (SpeechManager) getSystemService(SpeechService.SERVICE_NAME);
        if (mSpeechManager == null) {
            mSpeechManager = new SpeechManager(this, new SpeechManager.OnConnectListener() {
                @Override
                public void onConnect(boolean status) {
                    if (status) {
                        Util.Logd(TAG, "speech manager init success!");
                        if (mSpeechManager.getAsrEnable()) {

                        }
                    } else {
                        Util.Loge(TAG, "speech manager init fail!");
                    }
                }
            }, "com.avatar.dialog");
        } else {
            if (mSpeechManager.getAsrEnable()) {

            }
        }

        // create personate instance
        mPersonate = new PersonateImpl(this, mHandler);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Util.Loge(TAG, "enter onSaveInstanceState");
        this.onAttachedToWindow();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Util.Loge(TAG, "enter onSaveInstanceState");
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Util.Loge(TAG, "enter onSaveInstanceState");
    }

    @Override
    public void onStart() {
        super.onStart();

        Util.Loge(TAG, "enter onStart");
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onResume() {
        super.onResume();

        Util.Loge(TAG, "enter onResume");
        mSpeechManager.setTtsListener(mTtsListener);
        mSpeechManager.setNluListener(mNluListener);
        mSpeechManager.setAsrListener(mAsrListener);
        mSpeechManager.setAvwListener(mAvwListener);

        mPersonate.startScene(PersonateScene.SCENE_DEFAULT);
    }

    // @Override
    // public boolean onKeyDown(int keyCode, KeyEvent event) {
    // char keyValue = (char) event.getUnicodeChar();
    // return super.onKeyDown(keyCode, event);
    // }

    @Override
    public void onPause() {
        super.onPause();

        Util.Loge(TAG, "enter onPause");
        mSpeechManager.setTtsListener(null);
        mSpeechManager.setNluListener(null);
        mSpeechManager.setAsrListener(null);
        mSpeechManager.setAvwListener(null);

        // Util.Loge(TAG, "setAsrEnable true");
        // mSpeechManager.setAsrEnable(true);

        // mHandler.stopStateAnimation();

        mPersonate.stopScene();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSpeechManager.shutdown();
    }

    private List<Drawable> initImageDrawable(int[] imageArray) {
        List<Drawable> drawableList = new ArrayList<Drawable>(imageArray.length);

        for (int i = 0; i < imageArray.length; i++) {
            drawableList.add(getResources().getDrawable(imageArray[i]));
        }

        return drawableList;
    }

    private final SpeechManager.AsrListener mAsrListener = new SpeechManager.AsrListener() {
        @Override
        public void onBegin() {
            //Util.Loge(TAG, "ASR: Begin");
            Message.obtain(mHandler, MSG_START_LISTENING).sendToTarget();
        }

        @Override
        public void onVolumeChanged(float volume) {
            //Util.Loge(TAG, "ASR: onVolumeChanged " + volume);
            // Message.obtain(mHandler, MSG_VOLUME_CHANGED,
            // Float.valueOf(volume)).sendToTarget();
        }

        @Override
        public boolean onResult(String text) {
            if (!TextUtils.isEmpty(text)) {
                Util.Loge(TAG, "ASR Result: " + text);
                mDialogAdapter.addQuestion((String) text);
                //mDialogAdapter.addQuestion("我不知道你在说什么呢呢！看来我要学的还有很多呢");
            }
            return false;
        }

        @Override
        public void onError(int error) {
            //Util.Loge(TAG, "ASR: Error");
            //Message.obtain(mHandler, MSG_STOP_LISTENING).sendToTarget();
        }

        @Override
        public void onEnd() {
            //Util.Loge(TAG, "ASR: End");
            Message.obtain(mHandler, MSG_STOP_LISTENING).sendToTarget();
            //if (mSpeechManager.getAsrEnable()) {
            //    Message.obtain(mHandler, MSG_START_THINKING).sendToTarget();
            //}
        }

    };
    private final SpeechManager.AvwListener mAvwListener = new SpeechManager.AvwListener() {
        @Override
        public void onBegin() {
            Util.Loge(TAG, "AVW: Begin");
            //Message.obtain(mHandler, MSG_START_LISTENING).sendToTarget();
        }

        @Override
        public void onVolumeChanged(float volume) {
            // Util.Loge(TAG, "AVW: onVolumeChanged " + volume);
            // Message.obtain(mHandler, MSG_VOLUME_CHANGED,
            // Float.valueOf(volume)).sendToTarget();
        }

        @Override
        public boolean onResult(String text) {
            /*if (!TextUtils.isEmpty(text)) {
                Util.Loge(TAG, "AVW Result: " + text);
                mDialogAdapter.addQuestion((String) text);
            }*/
            return false;
        }

        @Override
        public void onError(int error) {
            Util.Loge(TAG, "AVW: Error");
            // Message.obtain(mHandler, MSG_STOP_LISTENING).sendToTarget();
        }

        @Override
        public void onEnd() {
            Util.Loge(TAG, "AVW: End");
            /*Message.obtain(mHandler, MSG_STOP_LISTENING).sendToTarget();
            if (mSpeechManager.getAsrEnable()) {
                Message.obtain(mHandler, MSG_START_THINKING).sendToTarget();
            }*/
        }

    };
    private final SpeechManager.NluListener mNluListener = new SpeechManager.NluListener() {

        @Override
        public void onBegin(int requestId) {
            Message.obtain(mHandler, MSG_START_THINKING).sendToTarget();
        }

        @Override
        public boolean onResult(int requestId, String text) {
            if (!TextUtils.isEmpty(text)) {
                Util.Loge(TAG, "NLU: " + text);
                mDialogAdapter.addAnswer(text);
                //mDialogAdapter.addAnswer("我不知道你在说什么呢！看来我要学的还有很多呢");
                if (mPersonate != null) {
                    mPersonate.onNluResult(requestId, text);
                }
            }
            return false;
        }

        @Override
        public void onError(int requestId) {
            Message.obtain(mHandler, MSG_STOP_THINKING).sendToTarget();
        }

        @Override
        public void onEnd(int requestId) {
            Message.obtain(mHandler, MSG_STOP_THINKING).sendToTarget();
        }

    };

    private final SpeechManager.TtsListener mTtsListener = new SpeechManager.TtsListener() {
        @Override
        public void onBegin(int requestId) {
            Util.Loge(TAG, "TTS: onBegin");
            Message.obtain(mHandler, MSG_START_SPEAKING).sendToTarget();
            if (mPersonate != null)
                mPersonate.onTtsBegin(requestId);
        }

        @Override
        public void onError(int requestId) {
            Util.Loge(TAG, "TTS: onError");
            Message.obtain(mHandler, MSG_STOP_SPEAKING).sendToTarget();
            if (mPersonate != null)
                mPersonate.onTtsError(requestId);
        }

        @Override
        public void onEnd(int requestId) {
            Util.Loge(TAG, "TTS: onEnd");;
            Message.obtain(mHandler, MSG_STOP_SPEAKING).sendToTarget();
            if (mPersonate != null)
                mPersonate.onTtsEnd(requestId);
        }
    };

    /*private class HelpAdapter extends ArrayAdapter<String> {
        private final LayoutInflater mInflater;
        private final String[] mData;

        public HelpAdapter(Context context, int resource, String[] data) {
            super(context, resource, data);
            mInflater = LayoutInflater.from(context);
            mData = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HelpAdapter.ViewHolder holder = null;

            // Util.Loge(TAG, "enter");
            if (convertView == null) {
                holder = new HelpAdapter.ViewHolder();
                convertView = mInflater.inflate(R.layout.list_help_item, null);
                holder.textView = (TextView) convertView.findViewById(R.id.text);
                convertView.setTag(holder);
            } else {
                holder = (HelpAdapter.ViewHolder) convertView.getTag();
            }

            holder.textView.setText(mData[position]);
            return convertView;
        }

        public final class ViewHolder {
            TextView textView;
        }
    }*/

    private class DialogAdapter extends BaseAdapter {
        private final int VIEW_TYPE_QUESTION = 0;
        private final int VIEW_TYPE_ANSWER = 1;

        private final float MAX_TEXT_SIZE = 13 * 2 * 90; // 13 characters per line, 2 line, textsize is 90px

        private final LayoutInflater mInflater;
        private final List<DialogAdapter.Dialog> mDialogList;

        public DialogAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            mDialogList = new ArrayList<DialogAdapter.Dialog>();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DialogAdapter.ViewHolder holder = null;

            // Util.Loge(TAG, "position: " + position);
            if (getItemViewType(position) == VIEW_TYPE_QUESTION) {
                if (convertView == null) {
                    holder = new DialogAdapter.ViewHolder();
                    // Util.Loge(TAG, "enter");
                    convertView = mInflater.inflate(R.layout.list_question_item, null);
                    holder.mTextView = (TextView) convertView.findViewById(R.id.question);
                    convertView.setTag("question".hashCode(), holder);
                } else {
                    holder = (DialogAdapter.ViewHolder) convertView.getTag("question".hashCode());
                    // Util.Loge(TAG, "enter");
                }
            } else {
                if (convertView == null) {
                    holder = new DialogAdapter.ViewHolder();
                    // Util.Loge(TAG, "enter");
                    convertView = mInflater.inflate(R.layout.list_answer_item, null);
                    holder.mTextView = (TextView) convertView.findViewById(R.id.answer);
                    convertView.setTag("answer".hashCode(), holder);
                } else {
                    holder = (DialogAdapter.ViewHolder) convertView.getTag("answer".hashCode());
                    // Util.Loge(TAG, "enter");
                }
            }

            TextView textView = holder.mTextView;
            TextPaint paint = textView.getPaint();

            CharSequence str = TextUtils.ellipsize(mDialogList.get(position).mItem, textView.getPaint(), MAX_TEXT_SIZE, TextUtils.TruncateAt.END);
            holder.mTextView.setText(str);
            return convertView;
        }

        public final class ViewHolder {
            TextView mTextView;
        }

        @Override
        public int getCount() {
            return mDialogList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDialogList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return mDialogList.get(position).mType;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public void notifyDataSetChanged() {
            if (getCount() > 0) {
                if (mDialogListView.getVisibility() == View.GONE) {
                    mDialogListView.setVisibility(View.VISIBLE);
                }
            } else {
                if (mDialogListView.getVisibility() == View.VISIBLE) {
                    mDialogListView.setVisibility(View.GONE);
                }
            }
            super.notifyDataSetChanged();
            mDialogListView.setSelection(mDialogAdapter.getCount() - 1);
        }

        public void addQuestion(String question) {
            mDialogList.add(new DialogAdapter.Dialog(question, VIEW_TYPE_QUESTION));
            // removeOldDialog();
            notifyDataSetChanged();
        }

        public void addAnswer(String answer) {
            mDialogList.add(new DialogAdapter.Dialog(answer, VIEW_TYPE_ANSWER));
            // removeOldDialog();
            notifyDataSetChanged();
        }

        private void removeOldDialog() {
            if (4 <= mDialogList.size()) {
                mDialogList.remove(0);
                // mDialogList.remove(0);
                // notifyDataSetChanged();
            }
        }

        private final class Dialog {
            public String mItem;
            public int mType;

            public Dialog(String item, int type) {
                mItem = item;
                mType = type;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_home_1:
            case R.id.btn_home_2:
                finish();
                break;

            case R.id.btn_mic:
                break;

            case R.id.btn_question_1:
            case R.id.btn_question_2:
                break;

            default:
                break;
        }
    }

    private void setAsrEnable(boolean enable) {
        mSpeechManager.setAsrEnable(enable);
        if (enable) {
            mSpeechManager.startListening();
        } else {
            mSpeechManager.stopListening();
            //mHandler.stopStateAnimation();
            //mHandler.stopMicroPhoneAnimation(enable);
        }
    }

    private void understand() {
        /*EditText editText = (EditText) findViewById(R.id.nlu_input);
        String text = editText.getEditableText().toString();
        if (!TextUtils.isEmpty(text)) {
            mDialogAdapter.addQuestion((String) text);
            mSpeechManager.startUnderstanding(text);
            editText.setText("");
        }*/
    }

    private static final int MSG_REFRESH_MICROPHONE = 0;
    private static final int MSG_REFRESH_STATE = 1;
    private static final int MSG_START_LISTENING = 2;
    private static final int MSG_VOLUME_CHANGED = 3;
    private static final int MSG_STOP_LISTENING = 4;
    private static final int MSG_START_THINKING = 5;
    private static final int MSG_STOP_THINKING = 6;
    private static final int MSG_START_SPEAKING = 7;
    private static final int MSG_STOP_SPEAKING = 8;

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_LISTENING:
                    //Util.Logd(TAG, "MSG_START_LISTENING");
                    startListenAnimation();
                    /*if (mHelpListView.getVisibility() == View.VISIBLE) {
                        mHelpListView.setVisibility(View.GONE);
                    }*/
                    //startStateAnimation(R.string.listening, mListenImageList);
                    //startMicroPhoneAnimation();
                    break;

                case MSG_VOLUME_CHANGED:
                    /*float volume = (Float) msg.obj;
                    //Util.Logd(TAG, "MSG_VOLUME_CHANGED:" + volume);
                    int index = (int) (mMicImageList.size() / 2 / 100 * volume);
                    if (index >= mMicImageList.size()) {
                        index = mMicImageList.size() - 1;
                    } else if (index < 0) {
                        index = 0;
                    }
                    mMicrophone.setBackground(mMicImageList.get(index));*/
                    break;
                case MSG_STOP_LISTENING:
                    startThinkAnimation();
                    //stopListenAnimation();
                    //Util.Logd(TAG, "MSG_STOP_LISTENING");
                    //stopStateAnimation();
                    //stopMicroPhoneAnimation(mSpeechManager.getAsrEnable());
                    break;

                case MSG_START_THINKING:
                    //startThinkAnimation();
                    //Util.Logd(TAG, "MSG_START_THINKING");
                    //startStateAnimation(R.string.thinking, mThinkImageList);
                    break;

                case MSG_STOP_THINKING:
                    stopThinkAnimation();
                    //Util.Logd(TAG, "MSG_STOP_THINKING");
                    //stopStateAnimation();
                    break;

                case MSG_START_SPEAKING:
                    startSpeakAnimation();
                    //Util.Logd(TAG, "MSG_START_SPEAKING");
                    //startStateAnimation(R.string.speaking, mSpeakImageList);
                    break;

                case MSG_STOP_SPEAKING:
                    stopSpeakAnimation();
                    //Util.Logd(TAG, "MSG_STOP_SPEAKING");
                    //stopStateAnimation();
                    break;

                case MSG_REFRESH_STATE:
                case MSG_REFRESH_MICROPHONE:
                    //Animation animation = (Animation) msg.obj;
                    //animation.play();
                    //sendMessageDelayed(Message.obtain(mHandler, msg.what, animation), animation.getFrequency());
                    break;

                default:
                    break;
            }
        }

        public void startListenAnimation() {
            mStateView.removeAllViews();
            mStateView.addView(mListenView);
        }

        public void stopListenAnimation() {
            mStateView.removeAllViews();
            //mStateView.addView(mThreeBtnView);
        }

        public void startThinkAnimation() {
            mStateView.removeAllViews();
            mStateView.addView(mThinkView);
            mThinkView.startThinkAnim();
        }

        public void stopThinkAnimation() {
            mThinkView.stopThinkAnim();
        }

        public void startSpeakAnimation() {
            mStateView.removeAllViews();
            mStateView.addView(mSpeakView);
        }

        public void stopSpeakAnimation() {
            mStateView.removeAllViews();
        }

        public void startMicroPhoneAnimation() {
            /*removeMessages(MSG_REFRESH_MICROPHONE);
            Message.obtain(this, MSG_REFRESH_MICROPHONE, new Animation(mMicrophone, mMicImageList).setFrequency(200))
                    .sendToTarget();*/
        }

        public void stopMicroPhoneAnimation(boolean enableAsr) {
            //removeMessages(MSG_REFRESH_MICROPHONE);
            if (enableAsr) {
                //mMicrophone.setBackgroundResource(R.drawable.mic_00);
            } else {
                //mMicrophone.setBackgroundResource(R.drawable.btn_microphone_click);
            }
        }

        public void startStateAnimation() {
            //findViewById(R.id.btn_help).setVisibility(View.GONE);
            //mTitleView.setText(tipId);

            //startStateAnimation(mState, imageList, 100);
        }

        public void startStateAnimation(View view, List<Drawable> imageList, int frequency) {
            //removeMessages(MSG_REFRESH_STATE);
            /*Message.obtain(this, MSG_REFRESH_STATE, new Animation(view, imageList).setFrequency(frequency))
                    .sendToTarget();*/
        }

        public void stopStateAnimation() {
            //removeMessages(MSG_REFRESH_STATE);
            //findViewById(R.id.btn_help).setVisibility(View.VISIBLE);
            //mTitleView.setText(R.string.help_tip);
            //mState.setBackgroundResource(R.drawable.speech_00);
        }
    };

    private class Animation {
        private View mView;
        private List<Drawable> mImageList;
        private int mFrequency;
        private int mIndex = 0;

        public Animation(View view, List<Drawable> drawables) {
            mView = view;
            mImageList = drawables;
            mFrequency = 100;
        }

        public Animation setFrequency(int frequency) {
            mFrequency = frequency;
            return this;
        }

        public int getFrequency() {
            return mFrequency;
        }

        protected void play(int index) {
            mView.setBackground(mImageList.get(index));
        }

        public void play() {
            play(mIndex);
            mIndex = ++mIndex % mImageList.size();
        }
    }
}

