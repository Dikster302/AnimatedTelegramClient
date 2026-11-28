package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
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
        long anchorId=oldFirst>=0&&oldFirst<chats.size()?chats.get(oldFirst).getId():0L;
        View firstChild=list.getChildAt(0);
        int oldTop=firstChild==null?0:firstChild.getTop();
        boolean wasAtBottom=chats.isEmpty()||list.getLastVisiblePosition()>=chats.size()-2;
        this.compact=compact;textColor=text;mutedColor=muted;accentColor=accent;panelColor=panel;
        if(dataChanged)chats=next;
        if(dataChanged||styleChanged){
            adapter.notifyDataSetChanged();
            list.post(()->{
                if(chats.isEmpty())return;
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
        ChatItemView(Context context,TgChat initial){
            super(context);setOrientation(VERTICAL);
            row=new NativeChatRowView(context,initial,compact,textColor,mutedColor,accentColor,panelColor,
                    null,null,null,null,null);
            addView(row,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
            divider=new View(context);
            LayoutParams dividerParams=new LayoutParams(LayoutParams.MATCH_PARENT,dp(1));
            dividerParams.leftMargin=dp(82);
            addView(divider,dividerParams);
        }
        void update(TgChat chat){
            row.update(chat,compact,textColor,mutedColor,accentColor,panelColor,
                    ()->{if(listener!=null)listener.onOpen(chat);},
                    ()->{if(listener!=null)listener.onArchive(chat);},
                    ()->{if(listener!=null)listener.onTogglePinned(chat);},
                    ()->{if(listener!=null)listener.onToggleUnread(chat);},
                    ()->{if(listener!=null)listener.onToggleMute(chat);});
            divider.setBackgroundColor((mutedColor&0x00FFFFFF)|0x29000000);
        }
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
