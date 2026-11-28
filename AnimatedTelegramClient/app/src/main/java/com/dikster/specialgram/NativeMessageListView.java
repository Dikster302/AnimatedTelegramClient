package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

/** Virtualized native message history list with grouping, date dividers and pagination. */
public final class NativeMessageListView extends LinearLayout {
    public interface Listener {
        void onLongPress(TgMessage message);
        void onReaction(TgMessage message,String emoji);
        void onLoadOlder();
    }

    private final ListView list;
    private final MessageAdapter adapter;
    private List<TgMessage> messages=new ArrayList<>();
    private Listener listener;
    private long chatId;
    private boolean searchMode,topTriggered;
    private int searchIndex;
    private int textColor,mutedColor,accentColor,incomingColor,outgoingColor,panelColor;
    private float bubbleRadius;
    private int previousFirstVisible=0;

    public NativeMessageListView(Context context,long chatId,List<TgMessage> messages,
            boolean searchMode,int searchIndex,int text,int muted,int accent,int incoming,
            int outgoing,int panel,float radius,Listener listener){
        super(context);setOrientation(VERTICAL);
        list=new ListView(context);list.setDivider(null);list.setDividerHeight(0);
        list.setSelector(android.R.color.transparent);
        list.setCacheColorHint(Color.TRANSPARENT);
        list.setPadding(0,dp(8),0,dp(8));list.setClipToPadding(false);
        adapter=new MessageAdapter();
        list.setAdapter(adapter);
        addView(list,new LayoutParams(LayoutParams.MATCH_PARENT,0,1f));
        list.setOnScrollListener(new android.widget.AbsListView.OnScrollListener(){
            @Override public void onScrollStateChanged(android.widget.AbsListView view,int state){}
            @Override public void onScroll(android.widget.AbsListView view,int first,int visible,int total){
                if(first>2)topTriggered=false;
                if(first<previousFirstVisible&&first<=2&&!topTriggered&&!NativeMessageListView.this.searchMode){
                    topTriggered=true;
                    if(NativeMessageListView.this.listener!=null)NativeMessageListView.this.listener.onLoadOlder();
                }
                previousFirstVisible=first;
            }
        });
        update(chatId,messages,searchMode,searchIndex,text,muted,accent,incoming,outgoing,panel,radius,listener);
    }

    public void update(long newChatId,List<TgMessage> values,boolean searching,int index,
            int text,int muted,int accent,int incoming,int outgoing,int panel,float radius,
            Listener callback){
        listener=callback;
        boolean chatChanged=chatId!=newChatId;
        boolean styleChanged=textColor!=text||mutedColor!=muted||accentColor!=accent
                ||incomingColor!=incoming||outgoingColor!=outgoing||panelColor!=panel
                ||bubbleRadius!=radius;
        boolean listChanged=values==null?!messages.isEmpty():!messages.equals(values);
        boolean modeChanged=searchMode!=searching;
        int oldFirst=list.getFirstVisiblePosition();
        View firstChild=list.getChildAt(0);
        int oldTop=firstChild==null?0:firstChild.getTop();
        boolean wasAtBottom=messages.isEmpty()||list.getLastVisiblePosition()>=messages.size()-2;
        chatId=newChatId;searchMode=searching;searchIndex=index;
        textColor=text;mutedColor=muted;accentColor=accent;incomingColor=incoming;
        outgoingColor=outgoing;panelColor=panel;bubbleRadius=radius;
        if(values!=null)messages=new ArrayList<>(values);else messages.clear();
        if(chatChanged){topTriggered=false;previousFirstVisible=0;}
        if(listChanged||styleChanged||modeChanged||chatChanged){
            adapter.notifyDataSetChanged();
            list.post(()->{
                if(messages.isEmpty())return;
                if(searchMode&&searchIndex>=0&&searchIndex<messages.size()){
                    list.setSelection(searchIndex);
                }else if(chatChanged||wasAtBottom){
                    list.setSelection(messages.size()-1);
                }else{
                    list.setSelectionFromTop(Math.min(oldFirst,messages.size()-1),oldTop);
                }
            });
        }else if(searching&&index>=0&&index<messages.size()&&index!=list.getSelectedItemPosition()){
            list.setSelection(index);
        }
    }

    private final class MessageAdapter extends BaseAdapter {
        @Override public int getCount(){return messages.size();}
        @Override public TgMessage getItem(int position){return messages.get(position);}
        @Override public long getItemId(int position){return getItem(position).getId();}
        @Override public boolean hasStableIds(){return true;}
        @Override public View getView(int position,View convertView,android.view.ViewGroup parent){
            MessageRow row=convertView instanceof MessageRow?(MessageRow)convertView:new MessageRow(getContext());
            row.update(position);
            return row;
        }
    }

    private final class MessageRow extends LinearLayout {
        private final FrameLayout dateHolder;
        private final View gap;
        private final NativeMessageBubbleView bubble;
        private int dateValue=Integer.MIN_VALUE;
        MessageRow(Context context){
            super(context);setOrientation(VERTICAL);
            setPadding(dp(10),0,dp(10),0);
            dateHolder=new FrameLayout(context);
            addView(dateHolder,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
            gap=new View(context);addView(gap,new LayoutParams(1,dp(6)));
            bubble=new NativeMessageBubbleView(context,messages.get(0),
                    textColor,mutedColor,accentColor,incomingColor,outgoingColor,panelColor,
                    bubbleRadius,false,false,null);
            addView(bubble,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        }
        void update(int position){
            TgMessage message=messages.get(position);
            TgMessage previous=position>0?messages.get(position-1):null;
            TgMessage next=position+1<messages.size()?messages.get(position+1):null;
            boolean startsDay=!searchMode&&(previous==null||!NativeTextRows.isSameMessageDay(previous.getDate(),message.getDate()));
            boolean groupedPrevious=previous!=null&&previous.getOutgoing()==message.getOutgoing()
                    &&NativeTextRows.isSameMessageDay(previous.getDate(),message.getDate())
                    &&message.getDate()-previous.getDate()<300;
            boolean groupedNext=next!=null&&next.getOutgoing()==message.getOutgoing()
                    &&NativeTextRows.isSameMessageDay(next.getDate(),message.getDate())
                    &&next.getDate()-message.getDate()<300;
            if(startsDay){
                dateHolder.setVisibility(VISIBLE);
                if(dateValue!=message.getDate()){
                    dateHolder.removeAllViews();
                    dateHolder.addView(NativeTextRows.messageDateDivider(getContext(),message.getDate(),panelColor));
                    dateValue=message.getDate();
                }
            }else{dateHolder.setVisibility(GONE);dateValue=Integer.MIN_VALUE;dateHolder.removeAllViews();}
            gap.setLayoutParams(new LayoutParams(1,dp(groupedPrevious?2:6)));
            NativeMessageBubbleView.Listener rowListener = new NativeMessageBubbleView.Listener() {
                @Override public void onLongPress() {
                    if (listener != null) listener.onLongPress(message);
                }
                @Override public void onReaction(String emoji) {
                    if (listener != null) listener.onReaction(message, emoji);
                }
            };
            bubble.update(message,textColor,mutedColor,accentColor,incomingColor,outgoingColor,
                    panelColor,bubbleRadius,groupedPrevious,groupedNext,rowListener);
        }
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
