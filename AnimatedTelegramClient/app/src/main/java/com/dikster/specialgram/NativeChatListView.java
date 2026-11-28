package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.HapticFeedbackConstants;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

/** Virtualized native chat list backed by Java chat rows and TDLib action callbacks. */
public final class NativeChatListView extends LinearLayout {
    public interface Listener {
        void onOpen(TgChat chat);
        void onArchive(TgChat chat);
        void onTogglePinned(TgChat chat);
        void onToggleUnread(TgChat chat);
        void onToggleMute(TgChat chat);
        void onPreview(TgChat chat);
    }

    private final ListView list;
    private final ChatAdapter adapter;
    private List<TgChat> chats=new ArrayList<>();
    private Listener listener;
    private boolean compact;
    private int textColor,mutedColor,accentColor,panelColor;

    public NativeChatListView(Context context,List<TgChat> chats,boolean compact,
            int text,int muted,int accent,int panel,Listener listener){
        super(context);setOrientation(VERTICAL);
        list=new ListView(context);list.setDivider(null);list.setDividerHeight(0);
        list.setSelector(android.R.color.transparent);list.setCacheColorHint(Color.TRANSPARENT);
        list.setPadding(0,0,0,dp(8));list.setClipToPadding(false);
        adapter=new ChatAdapter();list.setAdapter(adapter);
        addView(list,new LayoutParams(LayoutParams.MATCH_PARENT,0,1f));
        update(chats,compact,text,muted,accent,panel,listener);
    }

    public void update(List<TgChat> values,boolean compact,int text,int muted,int accent,
                       int panel,Listener callback){
        listener=callback;
        boolean styleChanged=this.compact!=compact||textColor!=text||mutedColor!=muted
                ||accentColor!=accent||panelColor!=panel;
        List<TgChat> next=values==null?new ArrayList<>():new ArrayList<>(values);
        boolean dataChanged=!chats.equals(next);
        int oldFirst=list.getFirstVisiblePosition();
        long oldTopChatId=chats.isEmpty()?0L:chats.get(0).getId();
        long anchorId=oldFirst>=0&&oldFirst<chats.size()?chats.get(oldFirst).getId():0L;
        View firstChild=list.getChildAt(0);
        int oldTop=firstChild==null?0:firstChild.getTop();
        boolean wasEmpty=chats.isEmpty();
        boolean wasAtBottom=wasEmpty||list.getLastVisiblePosition()>=chats.size()-2;
        this.compact=compact;textColor=text;mutedColor=muted;accentColor=accent;panelColor=panel;
        boolean newTopArrived=oldFirst==0&&!next.isEmpty()&&next.get(0).getId()!=oldTopChatId;
        if(dataChanged)chats=next;
        if(dataChanged||styleChanged){
            adapter.notifyDataSetChanged();
            list.post(()->{
                if(chats.isEmpty())return;
                if(wasEmpty||newTopArrived){list.setSelection(0);return;}
                if(wasAtBottom){list.setSelection(chats.size()-1);return;}
                int anchor=-1;
                if(anchorId!=0L)for(int i=0;i<chats.size();i++)if(chats.get(i).getId()==anchorId){anchor=i;break;}
                if(anchor<0)anchor=Math.min(oldFirst,chats.size()-1);
                list.setSelectionFromTop(anchor,oldTop);
            });
        }
    }

    private final class ChatAdapter extends BaseAdapter {
        @Override public int getCount(){return chats.size();}
        @Override public Object getItem(int position){return chats.get(position);}
        @Override public long getItemId(int position){return chats.get(position).getId();}
        @Override public boolean hasStableIds(){return true;}
        @Override public View getView(int position,View convertView,android.view.ViewGroup parent){
            ChatItemView item=convertView instanceof ChatItemView?(ChatItemView)convertView:new ChatItemView(getContext(),chats.get(position));
            item.update(chats.get(position));
            return item;
        }
    }

    private final class ChatItemView extends LinearLayout {
        private final NativeChatRowView row;
        private final View divider;
        private float swipeStartX,swipeStartY;
        private boolean swipeActive;
        private final int touchSlop;
        ChatItemView(Context context,TgChat initial){
            super(context);setOrientation(VERTICAL);
            touchSlop=ViewConfiguration.get(context).getScaledTouchSlop();
            row=new NativeChatRowView(context,initial,compact,textColor,mutedColor,accentColor,panelColor,
                    null,null,null,null,null,null);
            addView(row,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
            divider=new View(context);
            LayoutParams dividerParams=new LayoutParams(LayoutParams.MATCH_PARENT,dp(1));
            dividerParams.leftMargin=dp(82);
            addView(divider,dividerParams);
        }
        @Override public boolean onInterceptTouchEvent(MotionEvent event){
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
                swipeStartX=event.getX();swipeStartY=event.getY();swipeActive=false;
                return false;
            }
            if(event.getActionMasked()==MotionEvent.ACTION_MOVE){
                float dx=event.getX()-swipeStartX,dy=event.getY()-swipeStartY;
                if(Math.abs(dx)>touchSlop*2&&Math.abs(dx)>Math.abs(dy)*1.25f){
                    swipeActive=true;return true;
                }
            }
            return false;
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&swipeActive){
                float dx=event.getX()-swipeStartX;
                setTranslationX(Math.max(-dp(92),Math.min(dp(92),dx)));
                return true;
            }
            if(event.getActionMasked()==MotionEvent.ACTION_UP&&swipeActive){
                float dx=event.getX()-swipeStartX;
                setTranslationX(0f);
                if(Math.abs(dx)>=dp(72)&&listener!=null){
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    TgChat current=(TgChat)getTag();
                    if(current!=null){
                        if(dx<0)listener.onArchive(current);
                        else listener.onToggleUnread(current);
                    }
                }
                swipeActive=false;return true;
            }
            if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){
                animate().translationX(0f).setDuration(120).start();
                swipeActive=false;return true;
            }
            return swipeActive||super.onTouchEvent(event);
        }
        void update(TgChat chat){
            setTag(chat);
            row.update(chat,compact,textColor,mutedColor,accentColor,panelColor,
                    ()->{if(listener!=null)listener.onOpen(chat);},
                    ()->{if(listener!=null)listener.onArchive(chat);},
                    ()->{if(listener!=null)listener.onTogglePinned(chat);},
                    ()->{if(listener!=null)listener.onToggleUnread(chat);},
                    ()->{if(listener!=null)listener.onToggleMute(chat);},
                    ()->{if(listener!=null)listener.onPreview(chat);});
            divider.setBackgroundColor((mutedColor&0x00FFFFFF)|0x29000000);
        }
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
