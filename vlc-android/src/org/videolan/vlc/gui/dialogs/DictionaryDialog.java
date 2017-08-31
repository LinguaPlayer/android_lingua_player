package org.videolan.vlc.gui.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.PreferenceManager;
import android.text.Html;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.vlc.R;
import org.videolan.vlc.gui.video.VideoPlayerActivity;
import org.videolan.vlc.util.Dictionary.Dictionary;
import org.videolan.vlc.util.Dictionary.model.Glosbe;
import org.videolan.vlc.util.Dictionary.model.Phrase;
import org.videolan.vlc.util.Dictionary.model.Tuc;
import org.videolan.vlc.util.Dictionary.remote.GlosbeService;
import org.videolan.vlc.util.NoConnectivityException;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Created by habib on 4/20/17.
 */

public class DictionaryDialog extends DialogFragment implements AdapterView.OnItemSelectedListener{


    private EditText mWordToTranslateEditText;
    private ScrollView mainScrollView;
    private TextView mCaptionTextView;
    private Dictionary mDictionary;
    private TextView mOfflineTranslationTextView;
    private ProgressBar mOfflineDictionaryLoading;
    private TextView mOnlineTranslationTextView;
    private ProgressBar mOnlineDictionaryLoading;
    private LinearLayout mOfflineDictionaryLayout;
    private ImageView mSpeakButton;

    private String[] mFromLanguageValues = null;
    private String[] mToLanguageValues = null;
    //last selected Items position
    private static int mLastFromLangugeSelectedItem = 25;
    private static int mLastToLangugeSelectedItem = 84;

    private SharedPreferences mSettings = null;

    public final static String TAG = "VLC/DictionaryDialog";
    public final static String  LAST_FROM_LANGUAGE_SELECTED = "last_from_language_selected";
    public final static String  LAST_to_LANGUAGE_SELECTED = "last_to_language_selected";
    private boolean toLanguageInitialized = false;
    private boolean fromLanguageInitialized = false;


    public DictionaryDialog(){ }

    public static DictionaryDialog newInstance (String dialog,String wordToTranslate){
        DictionaryDialog dictionaryFragment = new DictionaryDialog();
        Bundle args = new Bundle();
        args.putString("word",wordToTranslate);
        args.putString("dialog",dialog);
        dictionaryFragment.setArguments(args);
        return dictionaryFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container , Bundle savedInstanceState){
        if(mSettings == null) {
            mSettings = PreferenceManager.getDefaultSharedPreferences(getContext());
            mLastFromLangugeSelectedItem = mSettings.getInt(LAST_FROM_LANGUAGE_SELECTED, 25);
            mLastToLangugeSelectedItem = mSettings.getInt(LAST_to_LANGUAGE_SELECTED, 84);
        }
        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        View v = inflater.inflate(R.layout.dictionary,container);

        return v;
    }

    private void translate(String word){
        mWordToTranslateEditText.setText(word);
        mWordToTranslateEditText.setSelection(word.length());

        mTranslateTask = new Translate();
        mTranslateTask.execute(new String[]{word});
        onlineTranslate(word);
    }

    @Override
    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        final String word = (String) getArguments().get("word");
        final String dialog = (String) getArguments().get("dialog");
        SpannableStringBuilder clickableCaption = makeWordsClickable(dialog);

        mWordToTranslateEditText = (EditText) view.findViewById(R.id.edit_text);
        mCaptionTextView = (TextView) view.findViewById(R.id.caption);
        mSpeakButton = (ImageView) view.findViewById(R.id.speak);
        mainScrollView = (ScrollView) view.findViewById(R.id.main_scroll_view);

        mOfflineTranslationTextView = (TextView) view.findViewById(R.id.offline_translation);
        mOfflineDictionaryLoading = (ProgressBar) view.findViewById(R.id.offline_dictionary_loading);

        mOnlineTranslationTextView = (TextView) view.findViewById(R.id.online_translation);
        mOnlineDictionaryLoading = (ProgressBar) view.findViewById(R.id.online_dictionary_loading);

        mOfflineDictionaryLayout = (LinearLayout) view.findViewById(R.id.offline_dictionary_layout);


        mWordToTranslateEditText.setText(word);
        mWordToTranslateEditText.setSelection(word.length());

        mCaptionTextView.setText(clickableCaption);
        mCaptionTextView.setMovementMethod(new LinkTouchMovementMethod());
        mCaptionTextView.setHighlightColor(getResources().getColor(R.color.orange500));

        mOfflineTranslationTextView.setMovementMethod(new LinkTouchMovementMethod());

        mSpeakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(mDictionary != null){
                   if(mDictionary.ttsForLanguageAvailable)
                       mDictionary.speakOut(mWordToTranslateEditText.getText().toString());
                   else
                       Toast.makeText(getContext(),getText(R.string.tts_not_available),Toast.LENGTH_SHORT).show();
               }
            }
        });

        mWordToTranslateEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    translate(mWordToTranslateEditText.getText().toString());
                    return false;
                }
                return false;
            }
        });

        Spinner fromLanguageSpinner = (Spinner) view.findViewById(R.id.language_from_list);
        Spinner toLanguageSpinner = (Spinner) view.findViewById(R.id.language_to_list);

        //From language spinner
        fromLanguageSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> fromLangugeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.online_from_language_entries, R.layout.language_list_spinner_item);
        mFromLanguageValues = getResources().getStringArray(R.array.online_from_language_values);
        fromLangugeAdapter.setDropDownViewResource(R.layout.language_list_spinner_dropdown_item);
        fromLanguageSpinner.setAdapter(fromLangugeAdapter);
        fromLanguageSpinner.setSelection(mLastFromLangugeSelectedItem);

        //To language spinner
        toLanguageSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> toLangugeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.online_to_language_entries, R.layout.language_list_spinner_item);
        mToLanguageValues = getResources().getStringArray(R.array.online_to_language_values);
        toLangugeAdapter.setDropDownViewResource(R.layout.language_list_spinner_dropdown_item);
        toLanguageSpinner.setAdapter(fromLangugeAdapter);
        toLanguageSpinner.setSelection(mLastToLangugeSelectedItem);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        SharedPreferences.Editor editor = mSettings.edit();
        switch (parent.getId()){
            case R.id.language_from_list:
                mLastFromLangugeSelectedItem = position;
                editor.putInt(LAST_FROM_LANGUAGE_SELECTED, mLastFromLangugeSelectedItem).commit();
                fromLanguageInitialized = true;
                break;
            case R.id.language_to_list:
                mLastToLangugeSelectedItem = position;
                editor.putInt(LAST_to_LANGUAGE_SELECTED, mLastToLangugeSelectedItem).commit();
                toLanguageInitialized = true;
                break;
        }
        String toLanguage = mToLanguageValues[mLastToLangugeSelectedItem];
        String fromLanguge = mFromLanguageValues[mLastFromLangugeSelectedItem];


        //prevent to translate two time
        if(toLanguageInitialized && fromLanguageInitialized) {
            mDictionaryLoadertask = new DictionaryLoader();
            mDictionaryLoadertask.execute(new String[]{fromLanguge, toLanguage, mWordToTranslateEditText.getText().toString()});
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    private void getMeaning(String from, final String dest, String phrase){
    }
    private void onlineTranslate(final String word){
        mOnlineTranslationTextView.setVisibility(View.GONE);
        mOnlineDictionaryLoading.setVisibility(View.VISIBLE);

        final String fromLanguge = mFromLanguageValues[mLastFromLangugeSelectedItem];
        final String toLanguage = mToLanguageValues[mLastToLangugeSelectedItem];

        GlosbeService mGlosbeService = mDictionary.getGlosbeService();
        if(mGlosbeService == null){
            if(isAdded()) {
                mOnlineDictionaryLoading.setVisibility(View.GONE);
                mOnlineTranslationTextView.setVisibility(View.VISIBLE);
                mOnlineTranslationTextView.setText(getString(R.string.online_translation_error));
            }
            return;
        }
        mGlosbeService.getMeaning(fromLanguge, toLanguage, word.toLowerCase()).enqueue(new Callback<Glosbe>() {
            @Override
            public void onResponse(Call<Glosbe> call, Response<Glosbe> response) {
                mOnlineDictionaryLoading.setVisibility(View.GONE);
                mOnlineTranslationTextView.setVisibility(View.VISIBLE);

                String translation = "";
                if(response.isSuccessful()) {
                    List<Tuc> tuc = response.body().getTuc();
                    if (tuc != null) {
                        int tucSize = tuc.size();
                        int meaningCount = 0;
                        for (int i = 0; i < tucSize; i++) {
                            Phrase phrase = tuc.get(i).getPhrase();
                            if (phrase != null && phrase.getLanguage().equals(toLanguage)) {
                                if (meaningCount++ > 0) {
                                    if (toLanguage.equals("fa"))
                                        translation += "، ";
                                    else if (toLanguage.equals("ja"))
                                        translation += "、 ";
                                    else
                                        translation += ", ";

                                }
                                translation += phrase.getText();
                            }
                        }
                        if (translation.isEmpty())
                            translation = word;
                        mOnlineTranslationTextView.setText(translation);
                    }
                }

                else{
                    if(isAdded()) {
                        mOnlineTranslationTextView.setText(getString(R.string.online_translation_error));
                    }
                }


            }

            @Override
            public void onFailure(Call<Glosbe> call, Throwable t) {
                mOnlineDictionaryLoading.setVisibility(View.GONE);
                mOnlineTranslationTextView.setVisibility(View.VISIBLE);
                if(t instanceof NoConnectivityException) {
                    if(isAdded()) {
                        final String message = "<font color='red' >" + "<b>" + getString(R.string.connection_error_title) + "</b>" + "</font>" +
                                "<br>" + getString(R.string.connection_error_message);
                        SpannableStringBuilder styledTranslation = (SpannableStringBuilder) Html.fromHtml(message);
                        mOnlineTranslationTextView.setText(styledTranslation);
                    }
                }
                else {
                    if(isAdded()) {
                        mOnlineTranslationTextView.setText(getString(R.string.unexpected_error));
                    }
                }
            }
        });
    }
    private Translate mTranslateTask = null;
    private class Translate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String translation = "";
            if(mDictionary != null)
                translation = mDictionary.getTranslation(params[0]);
            return translation;
        }

        @Override
        protected void onPostExecute (String translation) {
            SpannableStringBuilder translationWithLinks = getTranslationWithLinks(translation);
            mOfflineTranslationTextView.setText(translationWithLinks);
            mOfflineDictionaryLoading.setVisibility(View.GONE);
            mOfflineTranslationTextView.setVisibility(View.VISIBLE);

            focusOnView(mainScrollView, mOfflineDictionaryLayout);
        }

        @Override
        protected void onPreExecute() {
            mOfflineTranslationTextView.setVisibility(View.GONE);
            mOfflineDictionaryLoading.setVisibility(View.VISIBLE);
        }

    }

    private void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span, String url)
    {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        //prevent Error : not attached to Activity
        if(!isAdded())
            return;
        ClickableSpan clickableLinkSpan = new SubTouchSpan(url,true,Color.BLUE,Color.RED,getResources().getColor(R.color.white),getResources().getColor(R.color.white));
        strBuilder.setSpan(clickableLinkSpan, start, end, flags);
        strBuilder.removeSpan(span);
    }

    private SpannableStringBuilder getTranslationWithLinks (String html)
    {
        SpannableStringBuilder styledTranslation = (SpannableStringBuilder) Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(styledTranslation);
        URLSpan[] urls = strBuilder.getSpans(0, styledTranslation.length(), URLSpan.class);
        for(URLSpan span : urls) {
            makeLinkClickable(strBuilder, span, span.getURL().replaceAll("bword:\\/\\/","").replace("EP_","").replaceAll("_"," "));
        }

        return strBuilder;
    }

    private DictionaryLoader mDictionaryLoadertask = null;
    private class DictionaryLoader extends AsyncTask<String, String, Dictionary> {

        private String word;
        @Override
        protected Dictionary doInBackground(String... params) {
            String fromLanguage = params[0];
            String toLanguage = params[1];
            word = params[2];
            try {
                mDictionary = Dictionary.getInstance(getContext(), fromLanguage, toLanguage);
                return mDictionary;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Dictionary db) {
            mDictionary = db;
            translate(word);
        }

        @Override
        protected void onPreExecute() {
            mOfflineTranslationTextView.setVisibility(View.GONE);
            mOfflineDictionaryLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(String ...messages){
            Toast.makeText(getContext(),messages[0],Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        final Activity activity = getActivity();
        if(activity != null)
            ((VideoPlayerActivity) activity).onDictionaryDialogDismissed();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();

        Window window = getDialog().getWindow();
        Point size = new Point();

        Display display = window.getWindowManager().getDefaultDisplay();

        double width;
        double height;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            width = display.getWidth();
            height = display.getHeight();
        } else {
            display.getSize(size);
            width = size.x;
            height = size.y;
        }


        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        if(screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            window.setLayout((int) (width * 0.75), (int) (height * 0.7));
            window.setGravity(Gravity.CENTER);
        }
        else{
            mOfflineTranslationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
            mOnlineTranslationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
            mCaptionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
            window.setLayout((int) (width ), (int) (height * 0.85));
            window.setGravity(Gravity.TOP);
        }


    }


    private SpannableStringBuilder makeWordsClickable(String text){
        SpannableStringBuilder ss = new SpannableStringBuilder("");
        String pattern = "[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+]";
        Pattern r = Pattern.compile(pattern);
        String[] words = text.split("(?=[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+])|(?<=[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+])");
        int start,end;
        start = end =  0;
        int index = 0;
        for(final String word : words){
            index++;
            ss.append(word);
            end = start + word.length();

            if(!r.matcher(word).find()) {
                ClickableSpan clickableSpan = new SubTouchSpan(word,false,Color.BLACK,getResources().getColor(R.color.orange500),Color.parseColor("#ffdfae"),Color.parseColor("#ffdfae"));
                ss.setSpan(clickableSpan, start, end, 0);
            }

            start = end;
        }

        return ss;
    }

    private class LinkTouchMovementMethod extends LinkMovementMethod {
        private SubTouchSpan mPressedSpan;

        @Override
        public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mPressedSpan = getPressedSpan(textView, spannable, event);
                if (mPressedSpan != null) {
                    mPressedSpan.setPressed(true);
                    Selection.setSelection(spannable, spannable.getSpanStart(mPressedSpan),
                            spannable.getSpanEnd(mPressedSpan));
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                SubTouchSpan touchedSpan = getPressedSpan(textView, spannable, event);
                if (mPressedSpan != null && touchedSpan != mPressedSpan) {
                    mPressedSpan.setPressed(false);
                    mPressedSpan = null;
                    Selection.removeSelection(spannable);
                }
            } else {
                if (mPressedSpan != null) {
                    mPressedSpan.setPressed(false);
                    super.onTouchEvent(textView, spannable, event);
                }
                mPressedSpan = null;
                Selection.removeSelection(spannable);
            }
            return true;
        }

        private SubTouchSpan getPressedSpan(TextView textView, Spannable spannable, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= textView.getTotalPaddingLeft();
            y -= textView.getTotalPaddingTop();

            x += textView.getScrollX();
            y += textView.getScrollY();

            Layout layout = textView.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            SubTouchSpan[] link = spannable.getSpans(off, off, SubTouchSpan.class);
            SubTouchSpan touchedSpan = null;
            if (link.length > 0) {
                touchedSpan = link[0];
            }
            return touchedSpan;
        }

    }

    private class SubTouchSpan extends ClickableSpan {
        private final String mText;
        private boolean mIsPressed;
        private int mPressedBackgroundColor;
        private int mNormalTextColor;
        private int mPressedTextColor;
        private int bgColor;
        private boolean underlineText;

        private SubTouchSpan(final String text,boolean underlineText, int normalTextColor, int pressedTextColor,int normalBgColor, int pressedBackgroundColor) {
            mText = text;
            mNormalTextColor = normalTextColor;
            mPressedTextColor = pressedTextColor;
            mPressedBackgroundColor = pressedBackgroundColor;
            this.underlineText = underlineText;
            this.bgColor = normalBgColor;
        }

        public void setPressed(boolean isSelected) {
            mIsPressed = isSelected;
        }

        @Override
        public void onClick(final View widget) {
            translate(mText);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(mIsPressed ? mPressedTextColor : mNormalTextColor);
            ds.bgColor = mIsPressed ? mPressedBackgroundColor : this.bgColor ;
            ds.setUnderlineText(underlineText);
        }
    }

    private void focusOnView(final ScrollView scrollView, final View view){
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(0, view.getTop());
            }
        });
    }



}
