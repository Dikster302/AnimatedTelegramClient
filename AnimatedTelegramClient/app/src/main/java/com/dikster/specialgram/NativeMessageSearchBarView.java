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

/** Native in-chat search field with result counter and previous/next controls. */
public final class NativeMessageSearchBarView extends LinearLayout {
    public interface Listener {
        void onQuery(String value);
        void onPrevious();
        void onNext();
        void onClose();
    }
    private final EditText input;
    private final TextView counter, previous, next, close;
    private Listener listener;
    private boolean changing;
    private int textColor, mutedColor, accentColor, panelColor;

    public NativeMessageSearchBarView(Context context,String query,int index,int total,
                                      int text,int muted,int accent,int panel,Listener listener) {
        super(context);
        setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(8),dp(5),dp(6),dp(5));
        input=new EditText(context);input.setSingleLine(true);input.setTextSize(14);
        input.setPadding(dp(12),dp(7),dp(12),dp(7));input.setHint("Поиск в переписке");
        GradientDrawable bg=new GradientDrawable();bg.setColor(panel);bg.setCornerRadius(dp(18));
        input.setBackground(bg);
        addView(input,new LayoutParams(0,dp(44),1f));
        counter=text(context," ",muted,11);addView(counter,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
        previous=text(context,"↑",text,20);previous.setGravity(Gravity.CENTER);
        addView(previous,new LayoutParams(dp(34),dp(42)));
        next=text(context,"↓",text,20);next.setGravity(Gravity.CENTER);
        addView(next,new LayoutParams(dp(34),dp(42)));
        close=text(context,"✕",text,17);close.setGravity(Gravity.CENTER);
        addView(close,new LayoutParams(dp(34),dp(42)));
        input.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                if(!changing&&NativeMessageSearchBarView.this.listener!=null)
                    NativeMessageSearchBarView.this.listener.onQuery(s==null?"":s.toString());
            }
            @Override public void afterTextChanged(Editable e){}
        });
        previous.setOnClickListener(v->{if(this.listener!=null)this.listener.onPrevious();});
        next.setOnClickListener(v->{if(this.listener!=null)this.listener.onNext();});
        close.setOnClickListener(v->{if(this.listener!=null)this.listener.onClose();});
        update(query,index,total,text,muted,accent,panel,listener);
    }

    public void update(String query,int index,int total,int text,int muted,int accent,int panel,Listener callback){
        listener=callback;textColor=text;mutedColor=muted;accentColor=accent;panelColor=panel;
        input.setTextColor(textColor);input.setHintTextColor(mutedColor);
        GradientDrawable inputBackground=new GradientDrawable();inputBackground.setColor(panelColor);inputBackground.setCornerRadius(dp(18));input.setBackground(inputBackground);
        previous.setTextColor(textColor);next.setTextColor(textColor);close.setTextColor(textColor);
        counter.setTextColor(mutedColor);
        if(!query.equals(input.getText().toString())){
            changing=true;input.setText(query);input.setSelection(input.length());changing=false;
        }
        counter.setText(query.trim().isEmpty()?" ":((total==0?0:index+1)+"/"+total));
        previous.setEnabled(total>0);next.setEnabled(total>0);
        previous.setAlpha(total>0?1f:0.35f);next.setAlpha(total>0?1f:0.35f);
    }

    private TextView text(Context c,String value,int color,int size){
        TextView v=new TextView(c);v.setText(value);v.setTextColor(color);v.setTextSize(size);return v;
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
