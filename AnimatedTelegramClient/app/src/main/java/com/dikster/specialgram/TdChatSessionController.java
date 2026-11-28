package com.dikster.specialgram;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Owns active-chat identity and generation-safe history/search session bookkeeping. */
public final class TdChatSessionController {
    private volatile Long activeChat;
    private volatile int searchGeneration;
    private volatile int sessionGeneration;
    private final Set<Long> loadingChats = ConcurrentHashMap.newKeySet();
    private final Set<Long> exhaustedChats = ConcurrentHashMap.newKeySet();

    public Long getActiveChat() { return activeChat; }
    public void setActiveChat(Long chatId) { activeChat = chatId; }
    public int getSearchGeneration() { return searchGeneration; }
    public int getSessionGeneration() { return sessionGeneration; }

    public synchronized int open(long chatId) {
        activeChat = chatId;
        sessionGeneration++;
        searchGeneration++;
        loadingChats.clear();
        exhaustedChats.remove(chatId);
        return sessionGeneration;
    }
    public synchronized void close() {
        Long previous = activeChat;
        activeChat = null;
        sessionGeneration++;
        searchGeneration++;
        loadingChats.clear();
    }
    public synchronized int nextSearchGeneration() { return ++searchGeneration; }
    public boolean isLoading(long chatId) { return loadingChats.contains(chatId); }
    public boolean isExhausted(long chatId) { return exhaustedChats.contains(chatId); }
    public void markLoading(long chatId) { loadingChats.add(chatId); }
    public void clearLoading(long chatId) { loadingChats.remove(chatId); }
    public void markExhausted(long chatId) { exhaustedChats.add(chatId); }
    public void clearExhausted(long chatId) { exhaustedChats.remove(chatId); }
}
