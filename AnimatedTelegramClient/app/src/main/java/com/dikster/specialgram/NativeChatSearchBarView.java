package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Telegram-style integrated search field for the chat list. */
public final class NativeChatSearchBarView extends LinearLayout {
    public interface Listener { void onQuery(String value); }

    private final EditText input;
    private final SearchGlyph searchIcon;
    private final TextView clear;
    private Listener listener;
    private boolean changing;
    private int textColor,mutedColor,panelColor;

    public NativeChatSearchBarView(Context context,String query,int textColor,int mutedColor,
            int panelColor,Listener listener) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(12),dp(2),dp(6),dp(2));
        setMinimumHeight(dp(46));
        setFocusable(false);
        setClipToOutline(true);
        this.textColor=textColor;
        this.mutedColor=mutedColor;
        this.panelColor=panelColor;
        applyBackground();

        searchIcon=new SearchGlyph(context);
        searchIcon.setContentDescription("Поиск");
        searchIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        addView(searchIcon,new LayoutParams(dp(24),dp(24)));

        input=new EditText(context);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(textColor);
        input.setHintTextColor(mutedColor);
        input.setHint("Поиск");
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                |android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(dp(8),0,dp(4),0);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setSelectAllOnFocus(false);
        input.setMinHeight(dp(42));
        input.setContentDescription("Поиск по чатам и сообщениям");
        addView(input,new LayoutParams(0,dp(42),1f));

        clear=new TextView(context);
        clear.setText("×");
        clear.setTextColor(mutedColor);
        clear.setTextSize(23);
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(ripple(Color.TRANSPARENT));
        clear.setContentDescription("Очистить поиск");
        clear.setVisibility(GONE);
        addView(clear,new LayoutParams(dp(34),dp(40)));
        clear.setOnClickListener(v->{
            input.setText("");
            input.requestFocus();
        });

        input.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                clear.setVisibility(s==null||s.length()==0?GONE:VISIBLE);
                if(!changing&&NativeChatSearchBarView.this.listener!=null)
                    NativeChatSearchBarView.this.listener.onQuery(s==null?"":s.toString());
            }
            @Override public void afterTextChanged(Editable e){}
        });
        update(query,textColor,mutedColor,panelColor,listener);
    }

    public void focusSearch(){
        input.requestFocus();
        input.setSelection(input.length());
        InputMethodManager manager=(InputMethodManager)getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if(manager!=null)manager.showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);
    }

    public void update(String query,int textColor,int mutedColor,int panelColor,Listener callback){
        this.textColor=textColor;
        this.mutedColor=mutedColor;
        this.panelColor=panelColor;
        applyBackground();
        searchIcon.setColor(mutedColor);
        input.setTextColor(textColor);
        input.setHintTextColor(mutedColor);
        clear.setTextColor(mutedColor);
        listener=callback;
        String safe=query==null?"":query;
        if(!safe.equals(input.getText().toString())){
            changing=true;
            input.setText(safe);
            input.setSelection(input.length());
            changing=false;
        }
        clear.setVisibility(safe.isEmpty()?GONE:VISIBLE);
    }

    private void applyBackground(){
        GradientDrawable background=new GradientDrawable();
        background.setColor(panelColor);
        background.setCornerRadius(dp(16));
        setBackground(background);
        setElevation(dp(1));
    }

    private android.graphics.drawable.Drawable ripple(int color){
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(18));
        return new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x2233B5E5),shape,null);
    }

    private final class SearchGlyph extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private int color=Color.GRAY;
        SearchGlyph(Context context){super(context);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setColor(int value){color=value;invalidate();}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float cx=getWidth()/2f,cy=getHeight()/2f;
            float radius=dp(6.5f);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.8f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawCircle(cx-dp(1.5f),cy-dp(1.5f),radius,paint);
            canvas.drawLine(cx+dp(3.5f),cy+dp(3.5f),cx+dp(8),cy+dp(8),paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private int dp(float value){
        return Math.round(value*getResources().getDisplayMetrics().density);
    }
}
