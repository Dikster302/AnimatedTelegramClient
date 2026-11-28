package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Native horizontal story strip with per-chat grouping and unread rings. */
public final class NativeStoryStripView extends HorizontalScrollView {
    public interface Listener {
        void onPostStory();
        void onOpenStory(TgStory story);
    }
    private final LinearLayout content;
    private List<TgStory> stories = new ArrayList<>();
    private List<TgChat> chats = new ArrayList<>();
    private Listener listener;
    private TgAccount account;
    private int textColor, accentColor, panelColor;

    public NativeStoryStripView(Context context, List<TgStory> stories, List<TgChat> chats,
                                int text, int accent, int panel, TgAccount account, Listener listener) {
        super(context);
        setHorizontalScrollBarEnabled(false);
        this.textColor=text; this.accentColor=accent; this.panelColor=panel;this.account=account;
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(dp(12), dp(8), dp(12), dp(8));
        addView(content, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        update(stories,chats,text,accent,panel,account,listener);
    }

    public void update(List<TgStory> nextStories, List<TgChat> nextChats,
                       int text, int accent, int panel, TgAccount account, Listener callback) {
        listener=callback;
        boolean changed=!stories.equals(nextStories)||!chats.equals(nextChats)
                ||textColor!=text||accentColor!=accent||panelColor!=panel
                ||(this.account==null?account!=null:!this.account.equals(account));
        textColor=text; accentColor=accent; panelColor=panel;this.account=account;
        if (!changed) return;
        stories=nextStories==null?new ArrayList<>():new ArrayList<>(nextStories);
        chats=nextChats==null?new ArrayList<>():new ArrayList<>(nextChats);
        content.removeAllViews();
        addMyStory();
        Set<Long> seen=new HashSet<>();
        for(TgStory story:stories) {
            if(!seen.add(story.getChatId())) continue;
            List<TgStory> group=new ArrayList<>();
            for(TgStory item:stories) if(item.getChatId()==story.getChatId()) group.add(item);
            TgChat owner=findChat(story.getChatId());
            String name=owner==null?story.getOwnerName():owner.getTitle();
            String path=owner==null?"":owner.getAvatarPath();
            boolean viewed=true;
            for(TgStory item:group) if(!item.isViewed()) {viewed=false;break;}
            LinearLayout itemView=new LinearLayout(getContext());
            itemView.setOrientation(LinearLayout.VERTICAL);
            itemView.setGravity(Gravity.CENTER_HORIZONTAL);
            itemView.setPadding(dp(2),0,dp(2),0);
            NativeAvatarView avatar=new NativeAvatarView(getContext(),path,first(name),
                    story.getChatId(),62,false,true,viewed,panelColor);
            itemView.addView(avatar,new LinearLayout.LayoutParams(dp(62),dp(62)));
            TextView label=text(shortName(name),textColor,11,false);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setMaxWidth(dp(70));
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            itemView.addView(label,new LinearLayout.LayoutParams(dp(70),LayoutParams.WRAP_CONTENT));
            itemView.setOnClickListener(v->{
                TgStory first=null;
                for(TgStory candidate:group) if(!candidate.isViewed()){first=candidate;break;}
                if(first==null&&!group.isEmpty())first=group.get(0);
                if(first!=null&&listener!=null)listener.onOpenStory(first);
            });
            content.addView(itemView,new LinearLayout.LayoutParams(dp(70),LayoutParams.WRAP_CONTENT));
        }
    }

    private void addMyStory() {
        LinearLayout item=new LinearLayout(getContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout circle=new FrameLayout(getContext());
        GradientDrawable ring=new GradientDrawable();
        ring.setColor(panelColor); ring.setShape(GradientDrawable.OVAL);
        ring.setStroke(dp(2),accentColor);
        circle.setBackground(ring);
        String ownName=account==null?"Мой аккаунт":account.getName();
        NativeAvatarView ownAvatar=new NativeAvatarView(getContext(),
                account==null?"":account.getAvatarPath(),first(ownName),0L,62,false,true,false,panelColor);
        circle.addView(ownAvatar,new FrameLayout.LayoutParams(dp(62),dp(62),Gravity.CENTER));
        TextView plus=text("+",Color.WHITE,14,true);
        plus.setGravity(Gravity.CENTER);
        GradientDrawable plusBadge=new GradientDrawable();
        plusBadge.setColor(accentColor);
        plusBadge.setShape(GradientDrawable.OVAL);
        plus.setBackground(plusBadge);
        FrameLayout.LayoutParams plusParams=new FrameLayout.LayoutParams(dp(23),dp(23),
                Gravity.RIGHT|Gravity.BOTTOM);
        plusParams.rightMargin=dp(1);
        plusParams.bottomMargin=dp(1);
        circle.addView(plus,plusParams);
        circle.setContentDescription("Добавить историю");
        item.setContentDescription("Моя история — добавить фото или видео");
        item.addView(circle,new LinearLayout.LayoutParams(dp(62),dp(62)));
        TextView label=text("Моя история",textColor,11,false);
        label.setSingleLine(true); label.setMaxWidth(dp(70));
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        item.addView(label);
        item.setOnClickListener(v->{if(listener!=null)listener.onPostStory();});
        content.addView(item,new LinearLayout.LayoutParams(dp(70),LayoutParams.WRAP_CONTENT));
    }

    private TgChat findChat(long id) {
        for(TgChat chat:chats) if(chat.getId()==id) return chat;
        return null;
    }
    private TextView text(String value,int color,int size,boolean bold) {
        TextView v=new TextView(getContext());v.setText(value==null?"":value);
        v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private String first(String name){return name==null||name.isEmpty()?"?":name.substring(0,1).toUpperCase();}
    private String shortName(String name){return name==null?"":name;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
