package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** TDLib chat-history and message-search request coordinator. */
public final class TdChatHistoryController {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }

    private final Requester requester;

    public TdChatHistoryController(Requester requester) {
        this.requester = requester;
    }

    public void loadChat(long chatId, Callback callback) {
        requester.send(new TdApi.GetChat(chatId), callback);
    }

    public void loadInitial(long chatId, Callback callback) {
        requester.send(new TdApi.GetChatHistory(chatId, 0, 0, 50, false), callback);
    }

    public void search(long chatId, String query, Callback callback) {
        requester.send(new TdApi.SearchChatMessages(chatId, null, query, null,
                0L, 0, 50, null), callback);
    }

    public void searchProfileSection(long chatId,String tab,Callback callback) {
        TdApi.SearchMessagesFilter filter;
        switch(tab){
            case "Медиа": filter=new TdApi.SearchMessagesFilterPhotoAndVideo();break;
            case "Файлы": filter=new TdApi.SearchMessagesFilterDocument();break;
            case "Ссылки": filter=new TdApi.SearchMessagesFilterUrl();break;
            case "Голосовые": filter=new TdApi.SearchMessagesFilterVoiceAndVideoNote();break;
            case "GIF": filter=new TdApi.SearchMessagesFilterAnimation();break;
            case "Избранное": filter=new TdApi.SearchMessagesFilterPinned();break;
            default: filter=new TdApi.SearchMessagesFilterEmpty();break;
        }
        requester.send(new TdApi.SearchChatMessages(chatId,null,"",null,0L,0,50,filter),callback);
    }

    public void loadOlder(long chatId, long oldestMessageId, Callback callback) {
        requester.send(new TdApi.GetChatHistory(chatId, oldestMessageId, 0, 50, false), callback);
    }
}
