package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native translucent chat-list search field. */
public final class NativeChatSearchBarView extends LinearLayout {
    public interface Listener { void onQuery(String value); }
    private final EditText input;
    private final TextView clear;
    private Listener listener;
    private boolean changing;
    private int textColor,mutedColor,panelColor;

    public NativeChatSearchBarView(Context context,String query,int textColor,int mutedColor,int panelColor,Listener listener){
        super(context);setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(12),dp(4),dp(8),dp(4));
        this.textColor=textColor;this.mutedColor=mutedColor;this.panelColor=panelColor;
        GradientDrawable background=new GradientDrawable();background.setColor(panelColor);background.setCornerRadius(dp(18));setBackground(background);
        TextView search=new TextView(context);search.setText("⌕");search.setTextColor(textColor);search.setTextSize(24);
        addView(search,new LayoutParams(dp(30),dp(40)));
        input=new EditText(context);input.setSingleLine(true);input.setTextSize(15);
        input.setTextColor(textColor);input.setHintTextColor(mutedColor);input.setHint("Поиск");
        input.setPadding(dp(4),0,dp(4),0);input.setBackgroundColor(Color.TRANSPARENT);
        addView(input,new LayoutParams(0,dp(42),1f));
        clear=new TextView(context);clear.setText("✕");clear.setTextColor(mutedColor);clear.setTextSize(16);
        clear.setGravity(Gravity.CENTER);addView(clear,new LayoutParams(dp(34),dp(40)));
        clear.setOnClickListener(v->{if(this.listener!=null)this.listener.onQuery("");});
        input.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                clear.setVisibility(s==null||s.length()==0?GONE:VISIBLE);
                if(!changing&&NativeChatSearchBarView.this.listener!=null)
                    NativeChatSearchBarView.this.listener.onQuery(s==null?"":s.toString());
            }
            @Override public void afterTextChanged(Editable e){}
        });
        update(query,listener);
    }
    public void focusSearch(){input.requestFocus();input.setSelection(input.length());
        ((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(input,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}
    public void update(String query,int textColor,int mutedColor,int panelColor,Listener callback){
        this.textColor=textColor;this.mutedColor=mutedColor;this.panelColor=panelColor;
        GradientDrawable background=new GradientDrawable();background.setColor(panelColor);background.setCornerRadius(dp(18));setBackground(background);
        input.setTextColor(textColor);input.setHintTextColor(mutedColor);
        clear.setTextColor(mutedColor);
        listener=callback;
        if(!query.equals(input.getText().toString())){
            changing=true;input.setText(query);input.setSelection(input.length());changing=false;
        }
        clear.setVisibility(query==null||query.isEmpty()?GONE:VISIBLE);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
