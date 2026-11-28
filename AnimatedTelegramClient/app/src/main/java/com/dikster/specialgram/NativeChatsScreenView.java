package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
        void onSavedMessages();
        void onCreateGroup(String title,long[] userIds);
    }

    private final LinearLayout root;
    private final FrameLayout headerHost,contentHost;
    private final LinearLayout headerColumn,bottomNavigation;
    private State state;
    private Listener listener;
    private int selectedTab,renderedTab=-1;
    private String folder="Все",renderedFolder="";
    private boolean unreadOnly;
    private NativeHeaderWallpaperView wallpaperView;
    private NativeChatsHeaderView toolbar;
    private LinearLayout chatHeaderRow;
    private NativeChatSearchBarView chatSearch;
    private LinearLayout listColumn,heading;
    private TextView headingTitle,headingSubtitle;
    private NativeStoryStripView storyStrip;
    private NativeChatFoldersView folders;
    private NativeChatListView chatList;
    private NativeChatEmptyStateView chatEmpty;
    private NativeContactsPanel contactsPanel;
    private NativeSettingsPanelView settingsPanel;
    private LinearLayout profilePanel,callsPanel;
    private NativeStoryViewerDialogHost storyViewer;
    private TextView composeButton;
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
        bottomNavigation=new LinearLayout(context);
        bottomNavigation.setOrientation(LinearLayout.HORIZONTAL);
        bottomNavigation.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams navParams=new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,dp(66));
        navParams.setMargins(dp(10),dp(4),dp(10),dp(4));
        root.addView(bottomNavigation,navParams);
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
            chatList=null;chatEmpty=null;contactsPanel=null;settingsPanel=null;profilePanel=null;callsPanel=null;
            renderedTab=selectedTab;renderedFolder=folder;
        }
        if(selectedTab==1)renderContacts();
        else if(selectedTab==2)renderSettings();
        else if(selectedTab==3)renderProfile();
        else if(selectedTab==4)renderCalls();
        else renderChats();
        renderComposeButton();
        renderBottomNavigation();
        renderStoryViewer();
    }

    private void renderHeader(){
        if(wallpaperView==null){
            wallpaperView=new NativeHeaderWallpaperView(getContext(),selectedTab==0?state.headerWallpaper:null,state.panel);
            // Keep wallpaper constrained to the header area; MATCH_PARENT here can consume content height.
            headerHost.addView(wallpaperView,0,new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,dp(112)));
        }else wallpaperView.update(selectedTab==0?state.headerWallpaper:null,state.panel);
        wallpaperView.setVisibility(selectedTab==0?VISIBLE:GONE);

        if(toolbar==null){
            toolbar=new NativeChatsHeaderView(getContext(),selectedTab,mainChatCount(),unreadOnly,
                    state.sortNewest,state.text,state.account,state.themeName,toolbarListener());
        }
        toolbar.update(selectedTab,mainChatCount(),unreadOnly,state.sortNewest,state.text,
                state.account,state.themeName,toolbarListener());

        if(selectedTab==0){
            if(chatSearch==null){
                chatSearch=new NativeChatSearchBarView(getContext(),state.query,state.text,state.muted,
                        state.panelLight,value->{if(listener!=null)listener.onQuery(value);});
            }else chatSearch.update(state.query,state.text,state.muted,state.panelLight,
                    value->{if(listener!=null)listener.onQuery(value);});
            if(chatHeaderRow==null){
                chatHeaderRow=new LinearLayout(getContext());
                chatHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
                chatHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
            }
            if(toolbar.getParent()==headerColumn)headerColumn.removeView(toolbar);
            if(chatSearch.getParent()==headerColumn)headerColumn.removeView(chatSearch);
            if(chatHeaderRow.getParent()!=headerColumn){
                headerColumn.removeAllViews();
                headerColumn.addView(chatHeaderRow,new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,dp(56)));
            }
            if(chatSearch.getParent()!=chatHeaderRow){
                chatHeaderRow.addView(chatSearch,new LinearLayout.LayoutParams(0,dp(46),1f));
            }
            toolbar.setPadding(0,dp(5),0,dp(5));
            if(toolbar.getParent()!=chatHeaderRow){
                chatHeaderRow.addView(toolbar,new LinearLayout.LayoutParams(dp(48),dp(52)));
            }
        }else{
            if(chatHeaderRow!=null){
                if(chatHeaderRow.getParent()==headerColumn)headerColumn.removeView(chatHeaderRow);
                chatHeaderRow.removeAllViews();
                chatHeaderRow=null;
                chatSearch=null;
            }
            toolbar.setPadding(dp(16),dp(5),dp(10),dp(5));
            if(toolbar.getParent()!=headerColumn){
                headerColumn.removeAllViews();
                headerColumn.addView(toolbar,new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,dp(52)));
            }
        }
    }

    private void addContentWithTransition(View child){
        child.setAlpha(0f);
        child.setTranslationY(dp(10));
        contentHost.addView(child,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        child.animate().cancel();
        child.animate().alpha(1f).translationY(0f).setDuration(190L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f)).start();
    }

    private NativeChatsHeaderView.Listener toolbarListener(){
        return new NativeChatsHeaderView.Listener(){
            @Override public void onSelectTab(int tab){selectedTab=tab;update(state,listener);}
            @Override public void onSort(boolean newestFirst){if(listener!=null)listener.onSort(newestFirst);selectedTab=0;update(state,listener);}
            @Override public void onToggleUnread(){unreadOnly=!unreadOnly;update(state,listener);}
            @Override public void onSearch(){if(selectedTab!=0){selectedTab=0;update(state,listener);}else if(chatSearch!=null)chatSearch.focusSearch();}
            @Override public void onToggleTheme(){
                if(listener!=null)listener.onTheme("Midnight".equalsIgnoreCase(state.themeName)?"Светлая":"Midnight");
            }
            @Override public void onOpenCalls(){selectedTab=4;update(state,listener);}
            @Override public void onCreateGroup(){showCreateGroupDialog();}
            @Override public void onSavedMessages(){if(listener!=null)listener.onSavedMessages();}
        };
    }

    private void renderChats(){
        if(listColumn==null){
            listColumn=new LinearLayout(getContext());listColumn.setOrientation(LinearLayout.VERTICAL);
            addContentWithTransition(listColumn);
        }
        if(!"Архив".equals(folder)){
            if(storyStrip==null){
                storyStrip=new NativeStoryStripView(getContext(),state.stories,state.chats,state.text,state.accent,
                        state.panelLight,storyStripListener());
                listColumn.addView(storyStrip,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(92)));
            }else storyStrip.update(state.stories,state.chats,state.text,state.accent,state.panelLight,storyStripListener());
        }else if(storyStrip!=null){listColumn.removeView(storyStrip);storyStrip=null;}
        if(folders==null){
            folders=new NativeChatFoldersView(getContext(),folder,state.text,state.muted,state.accent,
                    value->{folder=value;update(state,listener);});
            listColumn.addView(folders,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(48)));
        }else folders.update(folder,state.text,state.muted,state.accent,value->{folder=value;update(state,listener);});
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
    /** Floating compose action opens the contact picker, like Telegram's new-message flow. */
    private void renderComposeButton(){
        if(selectedTab!=0||"Архив".equals(folder)){
            if(composeButton!=null&&composeButton.getParent()==contentHost)contentHost.removeView(composeButton);
            return;
        }
        if(composeButton==null){
            composeButton=new TextView(getContext());
            composeButton.setText("✎");
            composeButton.setTextSize(27);
            composeButton.setTextColor(Color.WHITE);
            composeButton.setGravity(Gravity.CENTER);
            composeButton.setContentDescription("Новое сообщение");
            composeButton.setFocusable(true);
            composeButton.setClickable(true);
            composeButton.setOnClickListener(v->{selectedTab=1;update(state,listener);});
        }
        GradientDrawable shape=new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(state.accent);
        composeButton.setBackground(shape);
        composeButton.setElevation(dp(8));
        if(composeButton.getParent()!=contentHost){
            FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(dp(56),dp(56),
                    Gravity.RIGHT|Gravity.BOTTOM);
            params.setMargins(0,0,dp(18),dp(18));
            contentHost.addView(composeButton,params);
            composeButton.setAlpha(0f);
            composeButton.setScaleX(.88f);
            composeButton.setScaleY(.88f);
            composeButton.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180L)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
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
            addContentWithTransition(contactsPanel);
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
            addContentWithTransition(settingsPanel);
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


    private void renderProfile(){
        contentHost.removeAllViews();
        profilePanel=new LinearLayout(getContext());
        profilePanel.setOrientation(LinearLayout.VERTICAL);
        profilePanel.setGravity(Gravity.CENTER_HORIZONTAL);
        profilePanel.setPadding(dp(24),dp(26),dp(24),dp(24));
        profilePanel.setBackgroundColor(state.background);
        String name=state.account==null?"Мой аккаунт":state.account.getName();
        NativeAvatarView avatar=new NativeAvatarView(getContext(),
                state.account==null?"":state.account.getAvatarPath(),first(name),0L,104,
                false,false,false,state.panelLight);
        profilePanel.addView(avatar,new LinearLayout.LayoutParams(dp(104),dp(104)));
        TextView accountName=new TextView(getContext());accountName.setText(name);
        accountName.setTextColor(state.text);accountName.setTextSize(22);
        accountName.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        np.topMargin=dp(14);profilePanel.addView(accountName,np);
        if(state.account!=null&&state.account.isPremium()){
            TextView premium=new TextView(getContext());premium.setText("✦ Telegram Premium");
            premium.setTextColor(0xFFB58CFF);premium.setTextSize(13);
            profilePanel.addView(premium);
        }
        String username=state.account==null?"":state.account.getUsername();
        String phone=state.account==null?"":state.account.getPhone();
        TextView details=new TextView(getContext());
        details.setText((username.isEmpty()?"":("@"+username+"\n"))+(phone.isEmpty()?"":phone));
        details.setTextColor(state.muted);details.setTextSize(14);details.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        dpv.topMargin=dp(6);profilePanel.addView(details,dpv);
        String bio=state.account==null?"":state.account.getBio();
        if(!bio.trim().isEmpty()){
            TextView about=new TextView(getContext());about.setText(bio);about.setTextColor(state.text);
            about.setTextSize(14);about.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
            bp.topMargin=dp(16);profilePanel.addView(about,bp);
        }
        TextView edit=new TextView(getContext());edit.setText("Редактировать профиль");
        edit.setTextColor(Color.WHITE);edit.setTextSize(15);edit.setGravity(Gravity.CENTER);
        GradientDrawable action=new GradientDrawable();action.setColor(state.accent);action.setCornerRadius(dp(14));
        edit.setBackground(action);
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(48));
        ep.topMargin=dp(22);profilePanel.addView(edit,ep);
        edit.setOnClickListener(v->{selectedTab=2;update(state,listener);if(settingsPanel!=null)settingsPanel.openAccountPage();});
        android.widget.ScrollView profileScroll=new android.widget.ScrollView(getContext());
        profileScroll.setFillViewport(true);profileScroll.addView(profilePanel);
        addContentWithTransition(profileScroll);
    }


    private void showCreateGroupDialog(){
        if(state.contacts.isEmpty()){
            new android.app.AlertDialog.Builder(getContext()).setTitle("Создать группу")
                    .setMessage("Сначала загрузи контакты Telegram, затем можно будет выбрать участников.")
                    .setPositiveButton("Понятно",null).show();
            return;
        }
        LinearLayout form=new LinearLayout(getContext());form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20),dp(8),dp(20),dp(8));
        android.widget.EditText titleInput=new android.widget.EditText(getContext());
        titleInput.setSingleLine(true);titleInput.setTextSize(16);titleInput.setHint("Название группы");
        form.addView(titleInput,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(52)));
        TextView choose=new TextView(getContext());choose.setText("Выбери участников");
        choose.setTextColor(state.muted);choose.setTextSize(13);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        cp.topMargin=dp(8);form.addView(choose,cp);
        android.widget.ScrollView scroll=new android.widget.ScrollView(getContext());
        LinearLayout choices=new LinearLayout(getContext());choices.setOrientation(LinearLayout.VERTICAL);
        List<android.widget.CheckBox> checks=new ArrayList<>();
        for(TgContact contact:state.contacts){
            android.widget.CheckBox check=new android.widget.CheckBox(getContext());
            check.setText(contact.getName());check.setTextColor(state.text);
            check.setButtonTintList(android.content.res.ColorStateList.valueOf(state.accent));
            checks.add(check);choices.addView(check,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(44)));
        }
        scroll.addView(choices);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(250));
        sp.topMargin=dp(4);form.addView(scroll,sp);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(getContext())
                .setTitle("Новая группа").setView(form)
                .setNegativeButton("Отмена",null).setPositiveButton("Создать",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v->{
                    String title=titleInput.getText().toString().trim();
                    if(title.isEmpty()){titleInput.setError("Укажи название группы");return;}
                    List<Long> ids=new ArrayList<>();
                    for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked())ids.add(state.contacts.get(i).getId());
                    if(ids.isEmpty()){choose.setText("Выбери хотя бы одного участника");choose.setTextColor(0xFFFF6B6B);return;}
                    long[] userIds=new long[ids.size()];for(int i=0;i<ids.size();i++)userIds[i]=ids.get(i);
                    if(listener!=null)listener.onCreateGroup(title,userIds);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void renderCalls(){
        if(callsPanel!=null)return;
        callsPanel=new LinearLayout(getContext());callsPanel.setOrientation(LinearLayout.VERTICAL);
        callsPanel.setGravity(Gravity.CENTER);callsPanel.setPadding(dp(28),dp(24),dp(28),dp(24));
        TextView icon=new TextView(getContext());icon.setText("☎");icon.setTextSize(46);
        icon.setTextColor(state.muted);callsPanel.addView(icon);
        TextView title=new TextView(getContext());title.setText("Звонки");
        title.setTextColor(state.text);title.setTextSize(20);
        title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        callsPanel.addView(title);
        TextView message=new TextView(getContext());
        message.setText("История звонков пока не подключена.");
        message.setTextColor(state.muted);message.setTextSize(14);message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        mp.topMargin=dp(8);callsPanel.addView(message,mp);
        addContentWithTransition(callsPanel);
    }

    private void renderBottomNavigation(){
        bottomNavigation.removeAllViews();
        String[] labels={"Чаты","Контакты","Звонки","Настройки"};
        int[] tabs={0,1,4,2};
        for(int i=0;i<labels.length;i++){
            final int tab=tabs[i];boolean active=selectedTab==tab;
            LinearLayout item=new LinearLayout(getContext());item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);item.setPadding(0,dp(5),0,dp(4));
            NavIconView icon=new NavIconView(getContext(),i,active?state.accent:state.muted);
            item.addView(icon,new LinearLayout.LayoutParams(dp(24),dp(24)));
            TextView label=new TextView(getContext());label.setText(labels[i]);label.setTextSize(10);
            label.setTextColor(active?state.accent:state.muted);label.setGravity(Gravity.CENTER);
            item.addView(label);
            if(active){GradientDrawable selected=new GradientDrawable();selected.setColor((state.accent&0x00FFFFFF)|0x22000000);
                selected.setCornerRadius(dp(14));item.setBackground(selected);}
            item.setOnClickListener(v->{selectedTab=tab;update(state,listener);});
            bottomNavigation.addView(item,new LinearLayout.LayoutParams(0,LayoutParams.MATCH_PARENT,1f));
        }
        GradientDrawable bg=new GradientDrawable();bg.setColor(state.panel);
        bg.setCornerRadius(dp(20));bottomNavigation.setBackground(bg);
        bottomNavigation.setElevation(dp(8));
    }


    private final class NavIconView extends View {
        private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final int kind,color;
        NavIconView(Context context,int kind,int color){super(context);this.kind=kind;this.color=color;}
        @Override protected void onDraw(android.graphics.Canvas canvas){
            super.onDraw(canvas);
            paint.setColor(color);paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            float cx=getWidth()/2f,cy=getHeight()/2f,d=dp(20);
            android.graphics.RectF rect=new android.graphics.RectF(cx-d*.42f,cy-d*.34f,cx+d*.42f,cy+d*.30f);
            if(kind==0){
                canvas.drawRoundRect(rect,dp(4),dp(4),paint);
                android.graphics.Path tail=new android.graphics.Path();
                tail.moveTo(cx-d*.20f,cy+d*.30f);tail.lineTo(cx-d*.32f,cy+d*.47f);
                tail.lineTo(cx+d*.02f,cy+d*.30f);canvas.drawPath(tail,paint);
            }else if(kind==1||kind==3){
                canvas.drawCircle(cx,cy-d*.18f,d*.19f,paint);
                android.graphics.RectF shoulders=new android.graphics.RectF(cx-d*.42f,cy+d*.03f,cx+d*.42f,cy+d*.65f);
                canvas.drawArc(shoulders,195,150,false,paint);
                if(kind==1){
                    canvas.drawCircle(cx-d*.37f,cy-d*.15f,d*.12f,paint);
                    android.graphics.RectF side=new android.graphics.RectF(cx-d*.64f,cy+d*.10f,cx-d*.18f,cy+d*.58f);
                    canvas.drawArc(side,205,135,false,paint);
                }
            }else if(kind==4){
                android.graphics.Path receiver=new android.graphics.Path();
                receiver.moveTo(cx-d*.30f,cy-d*.38f);
                receiver.cubicTo(cx-d*.42f,cy-d*.30f,cx-d*.25f,cy+d*.18f,cx+d*.24f,cy+d*.34f);
                receiver.cubicTo(cx+d*.38f,cy+d*.39f,cx+d*.45f,cy+d*.22f,cx+d*.33f,cy+d*.12f);
                canvas.drawPath(receiver,paint);
                canvas.drawLine(cx-d*.28f,cy-d*.35f,cx-d*.12f,cy-d*.18f,paint);
                canvas.drawLine(cx+d*.12f,cy+d*.18f,cx+d*.30f,cy+d*.30f,paint);
            }else{
                canvas.drawCircle(cx,cy,d*.38f,paint);
                canvas.drawCircle(cx,cy,d*.15f,paint);
                for(int i=0;i<8;i++){
                    double a=Math.PI*i/4.0;
                    float x1=cx+(float)Math.cos(a)*d*.43f,y1=cy+(float)Math.sin(a)*d*.43f;
                    float x2=cx+(float)Math.cos(a)*d*.53f,y2=cy+(float)Math.sin(a)*d*.53f;
                    canvas.drawLine(x1,y1,x2,y2,paint);
                }
            }
            paint.setStyle(android.graphics.Paint.Style.FILL);
        }
    }

    private String first(String value){
        return value==null||value.trim().isEmpty()?"S":value.trim().substring(0,1).toUpperCase(java.util.Locale.ROOT);
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
