package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Native Java conversation screen coordinator; all visible conversation controls are Android Views. */
public final class NativeConversationScreenView extends FrameLayout {
    public interface Listener {
        void onSearchMessages(String query);
        void onOpenProfile();
        void onCloseProfile();
        void onToggleMute(TgChat chat, boolean muted);
        void onBack();
        void onSend(String text,boolean animated,Long replyToMessageId);
        void onDeleteMessage(TgMessage message,boolean forEveryone);
        void onClearHistory(boolean forEveryone);
        void onEditMessage(TgMessage message,String text);
        void onReactToMessage(TgMessage message,String emoji);
        void onForwardMessage(TgMessage message,TgChat destination);
        void onPinMessage(TgMessage message);
        void onVideoNoteShape(String shape);
        void onRecordVideo();
        void onDismissOperationError();
        void onLoadOlderMessages();
        void onSendPhoto();
        void onSendDocument();
    }

    private final LinearLayout column;
    private final FrameLayout messageHost;
    private TgChat chat;
    private List<TgChat> chats=Collections.emptyList();
    private List<TgMessage> messages=Collections.emptyList(),searchResults=Collections.emptyList(),visibleMessages=Collections.emptyList();
    private TgProfile profile;
    private String videoNoteShape="circle",operationError,draft="",searchQuery="",lastShownError;
    private TgMessage replyingTo,selectedMessage;
    private boolean animatedMode=true,profileVisible,searchOpen;
    private int searchIndex;
    private int chatBackgroundColor,panelColor,panelLightColor,textColor,mutedColor,accentColor,incomingColor,outgoingColor;
    private float bubbleRadius;
    private Listener listener;
    private NativeConversationHeaderView header;
    private NativeMessageSearchBarView searchBar;
    private NativeMessageListView messageList;
    private NativeEmptyMessagesView emptyMessages;
    private NativeMessageComposerView composer;
    private NativeProfileDialogHost profileHost;

    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable pendingSearch=()->{
        if(listener!=null&&searchOpen&&chat!=null&&!"Секретный чат".equals(chat.getKind())
                &&!searchQuery.trim().isEmpty())listener.onSearchMessages(searchQuery.trim());
    };

    private final NativeConversationHeaderView.Listener headerListener=new NativeConversationHeaderView.Listener(){
        @Override public void onBack(){if(listener!=null)listener.onBack();}
        @Override public void onOpenProfile(){openProfile();}
        @Override public void onSearch(){searchOpen=true;searchQuery="";searchIndex=0;requestSearch();render();}
        @Override public void onClearHistory(){
            NativeClearHistoryDialog.show(getContext(),chat==null?"":chat.getTitle(),
                    forEveryone->{if(listener!=null)listener.onClearHistory(forEveryone);});
        }
        @Override public void onToggleAnimated(){animatedMode=!animatedMode;render();}
    };
    private final NativeMessageSearchBarView.Listener searchListener=new NativeMessageSearchBarView.Listener(){
        @Override public void onQuery(String value){searchQuery=value==null?"":value;searchIndex=0;requestSearch();render();}
        @Override public void onPrevious(){if(!visibleMessages.isEmpty()){searchIndex=searchIndex<=0?visibleMessages.size()-1:searchIndex-1;render();}}
        @Override public void onNext(){if(!visibleMessages.isEmpty()){searchIndex=(searchIndex+1)%visibleMessages.size();render();}}
        @Override public void onClose(){searchOpen=false;searchQuery="";searchIndex=0;requestSearch();render();}
    };
    private final NativeMessageListView.Listener listListener=new NativeMessageListView.Listener(){
        @Override public void onLongPress(TgMessage message){showMessageActions(message);}
        @Override public void onReaction(TgMessage message,String emoji){if(listener!=null)listener.onReactToMessage(message,emoji);}
        @Override public void onLoadOlder(){if(listener!=null)listener.onLoadOlderMessages();}
    };
    private final NativeMessageComposerView.Listener composerListener=new NativeMessageComposerView.Listener(){
        @Override public void onDraftChanged(String value){draft=value==null?"":value;}
        @Override public void onSend(String value){
            String text=value==null?"":value.trim();if(text.isEmpty())return;
            Long replyId=replyingTo==null?null:replyingTo.getId();
            draft="";replyingTo=null;render();
            if(listener!=null)listener.onSend(text,animatedMode,replyId);
        }
        @Override public void onSendPhoto(){if(listener!=null)listener.onSendPhoto();}
        @Override public void onSendDocument(){if(listener!=null)listener.onSendDocument();}
        @Override public void onRecordVideo(){if(listener!=null)listener.onRecordVideo();}
        @Override public void onChooseVideoShape(){
            NativeVideoShapePickerDialog.show(getContext(),videoNoteShape,
                    shape->{if(listener!=null)listener.onVideoNoteShape(shape);});
        }
        @Override public void onClearReply(){replyingTo=null;render();}
    };

    public NativeConversationScreenView(Context context,TgChat chat,List<TgChat> chats,
            List<TgMessage> messages,List<TgMessage> searchResults,TgProfile profile,
            String videoNoteShape,String operationError,int background,int chatBackground,
            int panel,int panelLight,int text,int muted,int accent,int incoming,int outgoing,
            float bubbleRadius,Listener listener){
        super(context);
        column=new LinearLayout(context);column.setOrientation(LinearLayout.VERTICAL);
        messageHost=new FrameLayout(context);
        addView(column,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        update(chat,chats,messages,searchResults,profile,videoNoteShape,operationError,
                background,chatBackground,panel,panelLight,text,muted,accent,incoming,outgoing,
                bubbleRadius,listener);
    }

    public void update(TgChat chat,List<TgChat> chats,List<TgMessage> messages,
            List<TgMessage> searchResults,TgProfile profile,String videoNoteShape,String operationError,
            int background,int chatBackground,int panel,int panelLight,int text,int muted,int accent,
            int incoming,int outgoing,float bubbleRadius,Listener callback){
        this.listener=callback;this.chat=chat;
        this.chats=copy(chats);this.messages=copy(messages);this.searchResults=copy(searchResults);
        this.profile=profile;this.videoNoteShape=videoNoteShape==null?"circle":videoNoteShape;
        this.operationError=operationError;this.chatBackgroundColor=chatBackground;
        this.panelColor=panel;this.panelLightColor=panelLight;this.textColor=text;this.mutedColor=muted;
        this.accentColor=accent;this.incomingColor=incoming;this.outgoingColor=outgoing;
        this.bubbleRadius=bubbleRadius;
        render();
    }

    private void render(){
        setBackgroundColor(chatBackgroundColor);
        visibleMessages=computeVisibleMessages();
        if(!searchOpen||searchQuery.trim().isEmpty())searchIndex=0;
        else if(!visibleMessages.isEmpty())searchIndex=Math.max(0,Math.min(searchIndex,visibleMessages.size()-1));
        if(header==null){
            header=new NativeConversationHeaderView(getContext(),chat,profile,animatedMode,textColor,
                    mutedColor,accentColor,panelColor,headerListener);
            column.addView(header,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        }else header.update(chat,profile,animatedMode,textColor,mutedColor,accentColor,panelColor,headerListener);

        if(searchOpen){
            if(searchBar==null){
                searchBar=new NativeMessageSearchBarView(getContext(),searchQuery,searchIndex,
                        visibleMessages.size(),textColor,mutedColor,accentColor,panelColor,searchListener);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
                p.setMargins(dp(10),dp(6),dp(10),dp(6));column.addView(searchBar,1,p);
            }else{
                searchBar.update(searchQuery,searchIndex,visibleMessages.size(),textColor,mutedColor,
                        accentColor,panelColor,searchListener);
                if(searchBar.getParent()!=column){
                    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
                    p.setMargins(dp(10),dp(6),dp(10),dp(6));column.addView(searchBar,1,p);
                }
            }
        }else if(searchBar!=null&&searchBar.getParent()==column){column.removeView(searchBar);searchBar=null;}

        if(messageHost.getParent()!=column){
            LinearLayout.LayoutParams bodyParams=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,0,1f);
            column.addView(messageHost,bodyParams);
        }
        if(visibleMessages.isEmpty()){
            messageList=null;
            if(emptyMessages==null)emptyMessages=new NativeEmptyMessagesView(getContext(),searchOpen,searchQuery,
                    chat.getKind(),textColor,mutedColor);
            emptyMessages.update(searchOpen,searchQuery,chat.getKind(),textColor,mutedColor);
            if(emptyMessages.getParent()!=messageHost){messageHost.removeAllViews();messageHost.addView(emptyMessages,
                    new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));}
        }else{
            emptyMessages=null;
            if(messageList==null){
                messageList=new NativeMessageListView(getContext(),chat.getId(),visibleMessages,searchOpen,
                        searchIndex,textColor,mutedColor,accentColor,incomingColor,outgoingColor,panelColor,
                        bubbleRadius,listListener);
                messageHost.removeAllViews();messageHost.addView(messageList,
                        new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
            }else{
                messageList.update(chat.getId(),visibleMessages,searchOpen,searchIndex,textColor,mutedColor,
                        accentColor,incomingColor,outgoingColor,panelColor,bubbleRadius,listListener);
            }
        }
        if(composer==null){
            composer=new NativeMessageComposerView(getContext(),draft,replyingTo,chat.getTitle(),textColor,
                    mutedColor,accentColor,panelColor,panelLightColor,composerListener);
            column.addView(composer,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        }else composer.update(draft,replyingTo,chat.getTitle(),textColor,mutedColor,accentColor,
                panelColor,panelLightColor,composerListener);

        if(profileVisible)ensureProfileDialog();
        if(operationError==null||operationError.trim().isEmpty())lastShownError=null;
        else if(!operationError.equals(lastShownError)){
            lastShownError=operationError;
            NativeOperationErrorDialog.show(getContext(),operationError,
                    ()->{if(listener!=null)listener.onDismissOperationError();});
        }
    }

    private List<TgMessage> computeVisibleMessages(){
        List<TgMessage> ordered=new ArrayList<>(messages);
        ordered.sort((a,b)->Integer.compare(a.getDate(),b.getDate()));
        if(!searchOpen||searchQuery.trim().isEmpty())return ordered;
        if(chat!=null&&"Секретный чат".equals(chat.getKind())){
            List<TgMessage> matches=new ArrayList<>();String query=searchQuery.trim().toLowerCase(java.util.Locale.ROOT);
            for(TgMessage message:ordered)if(message.getText().toLowerCase(java.util.Locale.ROOT).contains(query))matches.add(message);
            return matches;
        }
        List<TgMessage> result=new ArrayList<>(searchResults);
        result.sort((a,b)->Integer.compare(a.getDate(),b.getDate()));
        return result;
    }

    private void requestSearch(){
        handler.removeCallbacks(pendingSearch);
        if(listener==null)return;
        if(!searchOpen||searchQuery.trim().isEmpty()||chat==null||"Секретный чат".equals(chat.getKind())){
            listener.onSearchMessages("");return;
        }
        handler.postDelayed(pendingSearch,350L);
    }

    private void openProfile(){
        profileVisible=true;
        if(listener!=null)listener.onOpenProfile();
        ensureProfileDialog();
    }

    private void ensureProfileDialog(){
        if(profileHost==null){
            profileHost=new NativeProfileDialogHost(getContext(),chat,profile,textColor,mutedColor,
                    accentColor,panelLightColor,new NativeProfileDialogHost.Listener(){
                @Override public void onClose(){
                profileVisible=false;
                NativeProfileDialogHost old=profileHost;profileHost=null;
                if(old!=null&&old.getParent()==this)removeView(old);
                if(listener!=null)listener.onCloseProfile();
                }
                @Override public void onToggleMute(TgChat value,boolean muted){
                    if(listener!=null)listener.onToggleMute(value,muted);
                }
            });
            addView(profileHost,new FrameLayout.LayoutParams(dp(1),dp(1)));
        }else profileHost.update(chat,profile,textColor,mutedColor,accentColor,panelLightColor);
    }

    private void showMessageActions(TgMessage message){
        selectedMessage=message;
        NativeMessageActionsDialog.show(getContext(),message,chats,chat.getId(),textColor,mutedColor,
                accentColor,new NativeMessageActionsDialog.Listener(){
            @Override public void onDismiss(){selectedMessage=null;}
            @Override public void onReply(TgMessage value){replyingTo=value;render();}
            @Override public void onReaction(TgMessage value,String emoji){if(listener!=null)listener.onReactToMessage(value,emoji);}
            @Override public void onForward(TgMessage value,TgChat destination){if(listener!=null)listener.onForwardMessage(value,destination);}
            @Override public void onPin(TgMessage value){if(listener!=null)listener.onPinMessage(value);}
            @Override public void onEdit(TgMessage value,String text){if(listener!=null)listener.onEditMessage(value,text);}
            @Override public void onDelete(TgMessage value,boolean forEveryone){if(listener!=null)listener.onDeleteMessage(value,forEveryone);}
        });
    }

    private static <T> List<T> copy(List<T> values){return values==null?Collections.emptyList():new ArrayList<>(values);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onDetachedFromWindow(){
        handler.removeCallbacks(pendingSearch);
        if(profileHost!=null){removeView(profileHost);profileHost=null;}
        super.onDetachedFromWindow();
    }
}
