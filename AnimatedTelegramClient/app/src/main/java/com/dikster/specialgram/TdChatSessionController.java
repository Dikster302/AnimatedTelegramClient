package com.dikster.specialgram;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Owns active-chat identity and generation-safe history/search session bookkeeping. */
public final class TdChatSessionController {
    private volatile Long activeChat;
    private volatile int searchGeneration;
    private volatile int sessionGeneration;
    private final java.util.concurrent.ConcurrentMap<Long, Integer> loadingSessions = new ConcurrentHashMap<>();
    private final Set<Long> exhaustedChats = ConcurrentHashMap.newKeySet();

    public Long getActiveChat() { return activeChat; }
    public void setActiveChat(Long chatId) { activeChat = chatId; }
    public int getSearchGeneration() { return searchGeneration; }
    public int getSessionGeneration() { return sessionGeneration; }

    public synchronized int open(long chatId) {
        activeChat = chatId;
        sessionGeneration++;
        searchGeneration++;
        loadingSessions.clear();
        exhaustedChats.remove(chatId);
        return sessionGeneration;
    }
    public synchronized void close() {
        Long previous = activeChat;
        activeChat = null;
        sessionGeneration++;
        searchGeneration++;
        loadingSessions.clear();
    }
    public synchronized int nextSearchGeneration() { return ++searchGeneration; }
    public boolean isLoading(long chatId) { return loadingSessions.containsKey(chatId); }
    public boolean isExhausted(long chatId) { return exhaustedChats.contains(chatId); }
    public void markLoading(long chatId) { markLoading(chatId, sessionGeneration); }
    public void markLoading(long chatId, int generation) { loadingSessions.put(chatId, generation); }
    public void clearLoading(long chatId) { loadingSessions.remove(chatId); }
    public void clearLoading(long chatId, int generation) { loadingSessions.remove(chatId, generation); }
    public void markExhausted(long chatId) { exhaustedChats.add(chatId); }
    public void clearExhausted(long chatId) { exhaustedChats.remove(chatId); }
}
