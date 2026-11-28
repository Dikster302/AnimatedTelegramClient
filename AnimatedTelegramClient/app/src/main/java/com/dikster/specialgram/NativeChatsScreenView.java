package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Native Java coordinator for chats, folders, stories, contacts and settings. */
public final class NativeChatsScreenView extends FrameLayout {
    public static final class State {
        public final List<TgChat> chats;
        public final List<TgContact> contacts;
        public final List<TgStory> stories;
        public final String query,contactsQuery,privacyLastSeen,privacyPhone,privacyPhoto;
        public final boolean sortNewest,compactChats;
        public final TgAccount account;
        public final String operationError,themeName,accentName,wallpaperName,headerWallpaper;
        public final float glass,fontScale,bubbleRadius;
        public final int background,panel,text,muted,accent,panelLight;

        public State(List<TgChat> chats,List<TgContact> contacts,List<TgStory> stories,
                String query,String contactsQuery,boolean sortNewest,boolean compactChats,TgAccount account,
                String operationError,String privacyLastSeen,String privacyPhone,String privacyPhoto,
                String themeName,float glass,float fontScale,String accentName,float bubbleRadius,
                String wallpaperName,String headerWallpaper,int background,int panel,int text,
                int muted,int accent,int panelLight){
            this.chats=copy(chats);this.contacts=copy(contacts);this.stories=copy(stories);
            this.query=safe(query);this.contactsQuery=safe(contactsQuery);this.sortNewest=sortNewest;
            this.compactChats=compactChats;this.account=account;this.operationError=operationError;
            this.privacyLastSeen=safe(privacyLastSeen);this.privacyPhone=safe(privacyPhone);
            this.privacyPhoto=safe(privacyPhoto);this.themeName=safe(themeName);this.glass=glass;
            this.fontScale=fontScale;this.accentName=safe(accentName);this.bubbleRadius=bubbleRadius;
            this.wallpaperName=safe(wallpaperName);this.headerWallpaper=headerWallpaper;
            this.background=background;this.panel=panel;this.text=text;this.muted=muted;
            this.accent=accent;this.panelLight=panelLight;
        }
    }

    public interface Listener {
        void onOpenStory(TgStory story);
        void onCloseStory(TgStory story);
        void onReactToStory(TgStory story);
        void onPostStory();
        void onPostVideoStory();
        void onReplyToStory(TgStory story,String text);
        void onQuery(String query);
        void onContactsQuery(String query);
        void onSort(boolean newestFirst);
        void onCompactChats(boolean compact);
        void onSaveMyProfile(String firstName,String lastName,String bio);
        void onPickProfilePhoto();
        void onDismissOperationError();
        void onPrivacyLastSeen(String value);
        void onPrivacyPhone(String value);
        void onPrivacyPhoto(String value);
        void onTheme(String value);
        void onGlass(float value);
        void onFontScale(float value);
        void onAccent(String value);
        void onBubbleRadius(float value);
        void onWallpaper(String value);
        void onPickHeaderWallpaper();
        void onClearHeaderWallpaper();
        void onSelectCosmicCover();
        void onOpen(TgChat chat);
        void onOpenContact(TgContact contact);
        void onArchive(TgChat chat);
        void onTogglePinned(TgChat chat);
        void onToggleUnread(TgChat chat);
        void onToggleMute(TgChat chat);
    }

    private final LinearLayout root;
    private final FrameLayout headerHost,contentHost;
    private final LinearLayout headerColumn;
    private State state;
    private Listener listener;
    private int selectedTab,renderedTab=-1;
    private String folder="Все",renderedFolder="";
    private boolean unreadOnly;
    private NativeHeaderWallpaperView wallpaperView;
    private NativeChatsHeaderView toolbar;
    private NativeChatSearchBarView chatSearch;
    private LinearLayout listColumn,heading;
    private TextView headingTitle,headingSubtitle;
    private NativeStoryStripView storyStrip;
    private NativeChatFoldersView folders;
    private NativeChatListView chatList;
    private NativeChatEmptyStateView chatEmpty;
    private NativeContactsPanel contactsPanel;
    private NativeSettingsPanelView settingsPanel;
    private NativeStoryViewerDialogHost storyViewer;
    private TgStory viewingStory;
    private List<TgStory> orderedStories=Collections.emptyList();

    public NativeChatsScreenView(Context context,State state,Listener listener){
        super(context);
        root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);
        addView(root,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        headerHost=new FrameLayout(context);
        headerColumn=new LinearLayout(context);headerColumn.setOrientation(LinearLayout.VERTICAL);
        headerHost.addView(headerColumn,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        root.addView(headerHost,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        contentHost=new FrameLayout(context);
        root.addView(contentHost,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,0,1f));
        update(state,listener);
    }

    public void update(State state,Listener listener){
        this.state=state;this.listener=listener;
        orderedStories=TdStoryListFilter.ordered(state.stories);
        if(viewingStory!=null){
            TgStory refreshed=findStory(viewingStory.getChatId(),viewingStory.getStoryId());
            if(refreshed==null)closeStoryViewer();else viewingStory=refreshed;
        }
        renderHeader();
        if(selectedTab!=renderedTab||(selectedTab==0&&!folder.equals(renderedFolder))){
            contentHost.removeAllViews();listColumn=null;storyStrip=null;folders=null;heading=null;
            chatList=null;chatEmpty=null;contactsPanel=null;settingsPanel=null;
            renderedTab=selectedTab;renderedFolder=folder;
        }
        if(selectedTab==1)renderContacts();
        else if(selectedTab==2)renderSettings();
        else renderChats();
        renderStoryViewer();
    }

    private void renderHeader(){
        if(wallpaperView==null){
            wallpaperView=new NativeHeaderWallpaperView(getContext(),selectedTab==0?state.headerWallpaper:null,state.panel);
            // A MATCH_PARENT child inside a WRAP_CONTENT FrameLayout can consume
            // the remaining screen height and leave contentHost with zero height.
            // Keep the wallpaper constrained to the header area only.
            headerHost.addView(wallpaperView,0,new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,dp(136)));
        }else wallpaperView.update(selectedTab==0?state.headerWallpaper:null,state.panel);
        if(toolbar==null){
            toolbar=new NativeChatsHeaderView(getContext(),selectedTab,mainChatCount(),unreadOnly,
                    state.sortNewest,Color.WHITE,new NativeChatsHeaderView.Listener(){
                @Override public void onSelectTab(int tab){selectedTab=tab;update(state,listener);}
                @Override public void onSort(boolean newestFirst){if(listener!=null)listener.onSort(newestFirst);selectedTab=0;update(state,listener);}
                @Override public void onToggleUnread(){unreadOnly=!unreadOnly;update(state,listener);}
                @Override public void onSearch(){
                    if(selectedTab!=0){selectedTab=0;update(state,listener);}
                    else if(chatSearch!=null)chatSearch.focusSearch();
                }
            });
            headerColumn.addView(toolbar,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        }else toolbar.update(selectedTab,mainChatCount(),unreadOnly,state.sortNewest,Color.WHITE,toolbarListener());
        if(selectedTab==0){
            if(chatSearch==null){
                chatSearch=new NativeChatSearchBarView(getContext(),state.query,value->{if(listener!=null)listener.onQuery(value);});
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(50));
                p.setMargins(dp(14),dp(7),dp(14),dp(7));headerColumn.addView(chatSearch,p);
            }else chatSearch.update(state.query,value->{if(listener!=null)listener.onQuery(value);});
        }else if(chatSearch!=null&&chatSearch.getParent()==headerColumn){headerColumn.removeView(chatSearch);chatSearch=null;}
    }

    private NativeChatsHeaderView.Listener toolbarListener(){
        return new NativeChatsHeaderView.Listener(){
            @Override public void onSelectTab(int tab){selectedTab=tab;update(state,listener);}
            @Override public void onSort(boolean newestFirst){if(listener!=null)listener.onSort(newestFirst);selectedTab=0;update(state,listener);}
            @Override public void onToggleUnread(){unreadOnly=!unreadOnly;update(state,listener);}
            @Override public void onSearch(){if(selectedTab!=0){selectedTab=0;update(state,listener);}else if(chatSearch!=null)chatSearch.focusSearch();}
        };
    }

    private void renderChats(){
        if(listColumn==null){
            listColumn=new LinearLayout(getContext());listColumn.setOrientation(LinearLayout.VERTICAL);
            contentHost.addView(listColumn,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        }
        if(!"Архив".equals(folder)){
            if(storyStrip==null){
                storyStrip=new NativeStoryStripView(getContext(),state.stories,state.chats,state.text,state.accent,
                        state.panelLight,new NativeStoryStripView.Listener(){
                    @Override public void onPostStory(){NativeStoryComposerDialog.show(getContext(),
                            ()->{if(listener!=null)listener.onPostStory();},
                            ()->{if(listener!=null)listener.onPostVideoStory();});}
                    @Override public void onOpenStory(TgStory story){openStory(story);}
                });
                listColumn.addView(storyStrip,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(92)));
            }else storyStrip.update(state.stories,state.chats,state.text,state.accent,state.panelLight,storyStripListener());
        }else if(storyStrip!=null){listColumn.removeView(storyStrip);storyStrip=null;}
        if(folders==null){
            folders=new NativeChatFoldersView(getContext(),folder,state.text,state.muted,state.accent,
                    value->{folder=value;update(state,listener);});
            listColumn.addView(folders,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(48)));
        }else folders.update(folder,state.text,state.muted,state.accent,value->{folder=value;update(state,listener);});
        if(heading==null){
            heading=new LinearLayout(getContext());heading.setGravity(Gravity.CENTER_VERTICAL);
            heading.setPadding(dp(16),dp(7),dp(16),dp(7));
            headingTitle=new TextView(getContext());headingTitle.setTextSize(14);headingTitle.setTypeface(null,android.graphics.Typeface.BOLD);
            headingSubtitle=new TextView(getContext());headingSubtitle.setTextSize(12);
            heading.addView(headingTitle,new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1f));
            heading.addView(headingSubtitle,new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
            listColumn.addView(heading,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        }
        headingTitle.setText("Все".equals(folder)?"Все чаты":folder);headingTitle.setTextColor(state.accent);
        headingSubtitle.setText(unreadOnly?"Непрочитанные":state.sortNewest?"По активности":"А — Я");
        headingSubtitle.setTextColor(state.muted);
        List<TgChat> filtered=TdChatListFilter.filter(state.chats,state.query,state.sortNewest,unreadOnly,folder);
        if(filtered.isEmpty()){
            if(chatList!=null){listColumn.removeView(chatList);chatList=null;}
            NativeChatEmptyStateView.ListState emptyState=new NativeChatEmptyStateView.ListState(
                    state.chats.isEmpty()&&state.query.trim().isEmpty()&&unreadOnly==false&&"Все".equals(folder),
                    !state.query.trim().isEmpty()||unreadOnly||!"Все".equals(folder));
            if(chatEmpty==null){
                chatEmpty=new NativeChatEmptyStateView(getContext(),emptyState,state.text,state.muted);
                listColumn.addView(chatEmpty,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,0,1f));
            }else chatEmpty.update(emptyState,state.text,state.muted);
        }else{
            if(chatEmpty!=null){listColumn.removeView(chatEmpty);chatEmpty=null;}
            if(chatList==null){
                chatList=new NativeChatListView(getContext(),filtered,state.compactChats,state.text,state.muted,
                        state.accent,state.panel,chatListListener());
                listColumn.addView(chatList,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,0,1f));
            }else chatList.update(filtered,state.compactChats,state.text,state.muted,state.accent,state.panel,chatListListener());
        }
    }

    private void renderContacts(){
        List<TgContact> filtered=new ArrayList<>();
        String q=state.contactsQuery.toLowerCase(java.util.Locale.ROOT);
        for(TgContact contact:state.contacts){
            if(contact.getName().toLowerCase(java.util.Locale.ROOT).contains(q)
                    ||contact.getUsername().toLowerCase(java.util.Locale.ROOT).contains(q)
                    ||contact.getPhone().contains(state.contactsQuery))filtered.add(contact);
        }
        if(contactsPanel==null){
            contactsPanel=new NativeContactsPanel(getContext(),filtered,state.contactsQuery,state.background,
                    state.text,state.muted,state.accent,state.panelLight,contactsListener());
            contentHost.addView(contactsPanel,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        }else contactsPanel.update(filtered,state.contactsQuery,state.background,state.text,state.muted,
                state.accent,state.panelLight,contactsListener());
    }

    private void renderSettings(){
        if(settingsPanel==null){
            settingsPanel=new NativeSettingsPanelView(getContext(),state.account,state.operationError,
                    state.privacyLastSeen,state.privacyPhone,state.privacyPhoto,state.themeName,state.glass,
                    state.fontScale,state.accentName,state.bubbleRadius,state.wallpaperName,state.compactChats,
                    state.headerWallpaper,state.background,state.panel,state.text,state.muted,state.accent,
                    state.panelLight,settingsListener());
            contentHost.addView(settingsPanel,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        }else settingsPanel.update(state.account,state.operationError,state.privacyLastSeen,state.privacyPhone,
                state.privacyPhoto,state.themeName,state.glass,state.fontScale,state.accentName,state.bubbleRadius,
                state.wallpaperName,state.compactChats,state.headerWallpaper,state.background,state.panel,
                state.text,state.muted,state.accent,state.panelLight,settingsListener());
    }

    private NativeStoryStripView.Listener storyStripListener(){
        return new NativeStoryStripView.Listener(){
            @Override public void onPostStory(){NativeStoryComposerDialog.show(getContext(),
                    ()->{if(listener!=null)listener.onPostStory();},
                    ()->{if(listener!=null)listener.onPostVideoStory();});}
            @Override public void onOpenStory(TgStory story){openStory(story);}
        };
    }
    private NativeChatListView.Listener chatListListener(){
        return new NativeChatListView.Listener(){
            @Override public void onOpen(TgChat chat){if(listener!=null)listener.onOpen(chat);}
            @Override public void onArchive(TgChat chat){if(listener!=null)listener.onArchive(chat);}
            @Override public void onTogglePinned(TgChat chat){if(listener!=null)listener.onTogglePinned(chat);}
            @Override public void onToggleUnread(TgChat chat){if(listener!=null)listener.onToggleUnread(chat);}
            @Override public void onToggleMute(TgChat chat){if(listener!=null)listener.onToggleMute(chat);}
        };
    }
    private NativeContactsPanel.Listener contactsListener(){
        return new NativeContactsPanel.Listener(){
            @Override public void onQuery(String query){if(listener!=null)listener.onContactsQuery(query);}
            @Override public void onOpen(TgContact contact){if(listener!=null)listener.onOpenContact(contact);}
        };
    }
    private NativeSettingsPanelView.Listener settingsListener(){
        return new NativeSettingsPanelView.Listener(){
            @Override public void onSaveMyProfile(String first,String last,String bio){if(listener!=null)listener.onSaveMyProfile(first,last,bio);}
            @Override public void onPickProfilePhoto(){if(listener!=null)listener.onPickProfilePhoto();}
            @Override public void onDismissOperationError(){if(listener!=null)listener.onDismissOperationError();}
            @Override public void onPrivacyLastSeen(String value){if(listener!=null)listener.onPrivacyLastSeen(value);}
            @Override public void onPrivacyPhone(String value){if(listener!=null)listener.onPrivacyPhone(value);}
            @Override public void onPrivacyPhoto(String value){if(listener!=null)listener.onPrivacyPhoto(value);}
            @Override public void onTheme(String value){if(listener!=null)listener.onTheme(value);}
            @Override public void onGlass(float value){if(listener!=null)listener.onGlass(value);}
            @Override public void onFontScale(float value){if(listener!=null)listener.onFontScale(value);}
            @Override public void onAccent(String value){if(listener!=null)listener.onAccent(value);}
            @Override public void onBubbleRadius(float value){if(listener!=null)listener.onBubbleRadius(value);}
            @Override public void onWallpaper(String value){if(listener!=null)listener.onWallpaper(value);}
            @Override public void onCompactChats(boolean value){if(listener!=null)listener.onCompactChats(value);}
            @Override public void onPickHeaderWallpaper(){if(listener!=null)listener.onPickHeaderWallpaper();}
            @Override public void onClearHeaderWallpaper(){if(listener!=null)listener.onClearHeaderWallpaper();}
            @Override public void onSelectCosmicCover(){if(listener!=null)listener.onSelectCosmicCover();}
        };
    }

    private void openStory(TgStory story){
        viewingStory=story;if(listener!=null)listener.onOpenStory(story);renderStoryViewer();
    }
    private TgStory findStory(long chatId,int storyId){
        for(TgStory story:orderedStories)if(story.getChatId()==chatId&&story.getStoryId()==storyId)return story;
        return null;
    }
    private void renderStoryViewer(){
        if(viewingStory==null)return;
        int index=-1;for(int i=0;i<orderedStories.size();i++)if(orderedStories.get(i).getChatId()==viewingStory.getChatId()
                &&orderedStories.get(i).getStoryId()==viewingStory.getStoryId()){index=i;break;}
        if(index<0){closeStoryViewer();return;}
        viewingStory=orderedStories.get(index);
        if(storyViewer==null){
            storyViewer=new NativeStoryViewerDialogHost(getContext(),viewingStory,index+1,orderedStories.size(),storyViewerListener());
            addView(storyViewer,new FrameLayout.LayoutParams(dp(1),dp(1)));
        }else storyViewer.update(viewingStory,index+1,orderedStories.size(),storyViewerListener());
    }
    private NativeStoryViewerDialogHost.Listener storyViewerListener(){
        return new NativeStoryViewerDialogHost.Listener(){
            @Override public void onDismiss(){closeStoryViewer();}
            @Override public void onPrevious(){moveStory(-1);}
            @Override public void onNext(){moveStory(1);}
            @Override public void onReact(){if(listener!=null&&viewingStory!=null)listener.onReactToStory(viewingStory);}
            @Override public void onReply(String text){if(listener!=null&&viewingStory!=null)listener.onReplyToStory(viewingStory,text);}
        };
    }
    private void moveStory(int delta){
        if(viewingStory==null)return;
        int index=-1;for(int i=0;i<orderedStories.size();i++)if(orderedStories.get(i).getChatId()==viewingStory.getChatId()
                &&orderedStories.get(i).getStoryId()==viewingStory.getStoryId()){index=i;break;}
        int next=index+delta;
        if(index<0||next<0||next>=orderedStories.size()){closeStoryViewer();return;}
        TgStory previous=viewingStory;viewingStory=orderedStories.get(next);
        if(listener!=null){listener.onCloseStory(previous);listener.onOpenStory(viewingStory);}
        renderStoryViewer();
    }
    private void closeStoryViewer(){
        TgStory previous=viewingStory;viewingStory=null;
        NativeStoryViewerDialogHost old=storyViewer;storyViewer=null;
        if(old!=null&&old.getParent()==this)removeView(old);
        if(previous!=null&&listener!=null)listener.onCloseStory(previous);
    }
    private int mainChatCount(){int count=0;for(TgChat chat:state.chats)if(!chat.getArchived())count++;return count;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private static String safe(String value){return value==null?"":value;}
    private static <T> List<T> copy(List<T> values){return values==null?Collections.emptyList():new ArrayList<>(values);}
}
