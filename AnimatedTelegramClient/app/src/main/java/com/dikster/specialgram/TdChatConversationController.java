package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns generation-safe chat opening, history pagination and in-chat search. */
public final class TdChatConversationController {
    public interface CacheCallback { void onLoaded(List<TgMessage> messages); }

    public interface Listener {
        List<TgChat> chats();
        void setChats(List<TgChat> chats);
        List<TgMessage> messages();
        void setMessages(List<TgMessage> messages);
        void loadCachedMessages(long chatId, CacheCallback callback);
        void setSearchResults(List<TgMessage> messages);
        TgMessage mapMessage(TdApi.Message message, long readMarker);
        void enqueueViewedMessages(long chatId, long[] messageIds);
    }

    private final TdChatSessionController session;
    private final TdChatHistoryController history;
    private final Listener listener;

    public TdChatConversationController(TdChatSessionController session,
                                        TdChatHistoryController history,
                                        Listener listener) {
        this.session = session;
        this.history = history;
        this.listener = listener;
    }

    public void openChat(long chatId) {
        final int generation = session.open(chatId);
        listener.setSearchResults(new ArrayList<>());
        listener.setMessages(new ArrayList<>());
        listener.loadCachedMessages(chatId, cached -> {
            if (isCurrent(chatId, generation) && listener.messages().isEmpty() && !cached.isEmpty())
                listener.setMessages(new ArrayList<>(cached));
        });

        history.loadChat(chatId, new TdChatHistoryController.Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.Chat) || !isCurrent(chatId, generation)) return;
                TdApi.Chat chat = (TdApi.Chat) result;
                List<TgChat> updatedChats = new ArrayList<>();
                for (TgChat item : listener.chats()) updatedChats.add(item.getId() == chatId
                        ? copyChatReadState(item, chat.lastReadOutboxMessageId, chat.unreadCount) : item);
                listener.setChats(updatedChats);
                List<TgMessage> updatedMessages = new ArrayList<>();
                for (TgMessage item : listener.messages()) updatedMessages.add(item.getOutgoing()
                        ? copyMessage(item, item.getId() <= chat.lastReadOutboxMessageId, item.getReplyToText())
                        : item);
                listener.setMessages(updatedMessages);
            }
            @Override public void onError(TdApi.Error error) { }
        });

        history.loadInitial(chatId, new TdChatHistoryController.Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.Messages) || !isCurrent(chatId, generation)) return;
                TdApi.Message[] loaded = ((TdApi.Messages) result).messages;
                if (loaded.length < 50) session.markExhausted(chatId);
                long readMarker = readMarker(chatId);
                List<TgMessage> mapped = new ArrayList<>(loaded.length);
                for (TdApi.Message message : loaded) mapped.add(listener.mapMessage(message, readMarker));
                // Preserve live UpdateNewMessage events that may arrive while history is loading.
                Map<Long, TgMessage> byId = new LinkedHashMap<>();
                for (TgMessage item : mapped) byId.put(item.getId(), item);
                for (TgMessage item : listener.messages()) byId.put(item.getId(), item);
                List<TgMessage> combined = new ArrayList<>(byId.values());
                Map<Long, TgMessage> allById = indexById(combined);
                List<TgMessage> resolved = new ArrayList<>(combined.size());
                for (TgMessage item : combined) {
                    String reply = item.getReplyToMessageId() == 0L ? item.getReplyToText()
                            : textFor(allById, item.getReplyToMessageId(), item.getReplyToText());
                    resolved.add(copyMessage(item, item.isRead(), reply));
                }
                resolved.sort((a, b) -> Integer.compare(a.getDate(), b.getDate()));
                listener.setMessages(resolved);

                List<Long> incoming = new ArrayList<>();
                for (TdApi.Message message : loaded) if (!message.isOutgoing) incoming.add(message.id);
                if (!incoming.isEmpty()) {
                    long[] ids = new long[incoming.size()];
                    for (int i = 0; i < incoming.size(); i++) ids[i] = incoming.get(i);
                    listener.enqueueViewedMessages(chatId, ids);
                }
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public void closeChat() {
        session.close();
        listener.setSearchResults(new ArrayList<>());
    }

    public void searchChatMessages(long chatId, String query) {
        String value = query == null ? "" : query.trim();
        int generation = session.nextSearchGeneration();
        if (value.isEmpty()) {
            listener.setSearchResults(new ArrayList<>());
            return;
        }
        if (!isActive(chatId)) return;
        listener.setSearchResults(new ArrayList<>());
        history.search(chatId, value, new TdChatHistoryController.Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (generation != session.getSearchGeneration() || !isActive(chatId)
                        || !(result instanceof TdApi.FoundChatMessages)) return;
                TdApi.Message[] found = ((TdApi.FoundChatMessages) result).messages;
                long readMarker = readMarker(chatId);
                List<TgMessage> mapped = new ArrayList<>(found.length);
                for (TdApi.Message message : found) mapped.add(listener.mapMessage(message, readMarker));
                mapped.sort((a, b) -> Integer.compare(a.getDate(), b.getDate()));
                listener.setSearchResults(mapped);
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public interface ProfileSectionCallback { void onResult(List<TgMessage> messages); }

    public void searchProfileSection(long chatId,String tab,ProfileSectionCallback callback) {
        if(!isActive(chatId)||callback==null)return;
        final int generation=session.getSessionGeneration();
        history.searchProfileSection(chatId,tab,new TdChatHistoryController.Callback(){
            @Override public void onResult(TdApi.Object result){
                if(!isCurrent(chatId,generation)||!(result instanceof TdApi.FoundChatMessages))return;
                TdApi.Message[] found=((TdApi.FoundChatMessages)result).messages;
                long marker=readMarker(chatId);
                List<TgMessage> mapped=new ArrayList<>(found.length);
                for(TdApi.Message message:found)mapped.add(listener.mapMessage(message,marker));
                mapped.sort((a,b)->Integer.compare(b.getDate(),a.getDate()));
                callback.onResult(mapped);
            }
            @Override public void onError(TdApi.Error error){if(isCurrent(chatId,generation))callback.onResult(new ArrayList<>());}
        });
    }

    public void loadOlderMessages(long chatId) {
        if (!isActive(chatId) || session.isLoading(chatId) || session.isExhausted(chatId)) return;
        List<TgMessage> previous = listener.messages();
        if (previous.isEmpty()) return;
        TgMessage oldest = previous.get(0);
        for (TgMessage item : previous) if (item.getId() < oldest.getId()) oldest = item;
        final int generation = session.getSessionGeneration();
        session.markLoading(chatId, generation);
        history.loadOlder(chatId, oldest.getId(), new TdChatHistoryController.Callback() {
            @Override public void onResult(TdApi.Object result) {
                session.clearLoading(chatId, generation);
                if (session.getSessionGeneration() != generation) return;
                if (!isActive(chatId) || !(result instanceof TdApi.Messages)) return;
                TdApi.Message[] loaded = ((TdApi.Messages) result).messages;
                if (loaded.length == 0) {
                    session.markExhausted(chatId);
                    return;
                }

                List<TgMessage> current = listener.messages();
                long readMarker = readMarker(chatId);
                List<TgMessage> combined = new ArrayList<>();
                for (TdApi.Message message : loaded) combined.add(listener.mapMessage(message, readMarker));
                combined.addAll(current);
                Map<Long, TgMessage> unique = new LinkedHashMap<>();
                for (TgMessage item : combined) unique.putIfAbsent(item.getId(), item);
                combined = new ArrayList<>(unique.values());
                combined.sort((a, b) -> Integer.compare(a.getDate(), b.getDate()));
                Map<Long, TgMessage> byId = indexById(combined);
                List<TgMessage> resolved = new ArrayList<>(combined.size());
                for (TgMessage item : combined) {
                    String reply = item.getReplyToMessageId() != 0L && item.getReplyToText().trim().isEmpty()
                            ? textFor(byId, item.getReplyToMessageId(), item.getReplyToText())
                            : item.getReplyToText();
                    resolved.add(copyMessage(item, item.isRead(), reply));
                }
                listener.setMessages(resolved);
                if (loaded.length < 50 || resolved.size() == current.size()) session.markExhausted(chatId);
            }
            @Override public void onError(TdApi.Error error) {
                session.clearLoading(chatId, generation);
            }
        });
    }

    private long readMarker(long chatId) {
        for (TgChat chat : listener.chats()) if (chat.getId() == chatId)
            return chat.getLastReadOutboxMessageId();
        return 0L;
    }

    private boolean isCurrent(long chatId, int generation) {
        return isActive(chatId) && session.getSessionGeneration() == generation;
    }

    private boolean isActive(long chatId) {
        Long active = session.getActiveChat();
        return active != null && active == chatId;
    }

    private static Map<Long, TgMessage> indexById(List<TgMessage> messages) {
        Map<Long, TgMessage> result = new LinkedHashMap<>();
        for (TgMessage message : messages) result.put(message.getId(), message);
        return result;
    }

    private static String textFor(Map<Long, TgMessage> messages, long id, String fallback) {
        TgMessage reply = messages.get(id);
        return reply == null ? fallback : reply.getText();
    }

    private static TgChat copyChatReadState(TgChat chat, long readMarker, int unreadCount) {
        return new TgChat(chat.getId(), chat.getTitle(), chat.getPreview(), chat.getLastDate(),
                unreadCount, chat.getAvatarPath(), chat.getKind(), chat.getArchived(), chat.getStatus(),
                chat.isOnline(), chat.isPremium(), readMarker, chat.getHasStory(), chat.getHasUnviewedStory(),
                chat.isPinned(), chat.isMarkedAsUnread(), chat.isMuted());
    }

    private static TgMessage copyMessage(TgMessage message, boolean read, String replyText) {
        return new TgMessage(message.getId(), message.getText(), message.getOutgoing(), message.getDate(),
                read, message.getVideoShape(), message.getVideoPath(), message.getReplyToMessageId(),
                replyText, message.getReactions(), message.getPhotoPath(), message.getMediaKind(),
                message.getMediaPath(), message.getMediaName(), message.getMediaMimeType(),
                message.getMediaDuration(), message.getMediaSize(), message.isPinned());
    }
}
