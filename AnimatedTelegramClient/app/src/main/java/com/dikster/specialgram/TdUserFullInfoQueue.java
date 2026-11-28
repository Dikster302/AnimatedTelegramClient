package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Serializes GetUserFullInfo requests and backs off on TDLib 429 responses. */
public final class TdUserFullInfoQueue implements AutoCloseable {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }
    public interface Listener { void onUserFullInfo(long userId,TdApi.UserFullInfo info); }

    private static final long GAP_MS=300L,MAX_BACKOFF_MS=30_000L;
    private static final int MAX_RETRIES=5;
    private static final Pattern RETRY_AFTER=Pattern.compile("retry after\\s+(\\d+)",Pattern.CASE_INSENSITIVE);
    private final Requester requester;
    private final Listener listener;
    private final ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor(r->{
        Thread thread=new Thread(r,"specialgram-user-full-info");thread.setDaemon(true);return thread;
    });
    private final ArrayDeque<Long> pending=new ArrayDeque<>();
    private final Set<Long> queued=new HashSet<>(),completed=new HashSet<>();
    private final Map<Long,Integer> retries=new HashMap<>();
    private boolean inFlight,closed;

    public TdUserFullInfoQueue(Requester requester,Listener listener){
        this.requester=requester;this.listener=listener;
    }
    public void enqueue(long userId){
        synchronized(this){
            if(closed||userId<=0||completed.contains(userId)||!queued.add(userId))return;
            pending.addLast(userId);
            if(inFlight||pending.size()>1)return;
            inFlight=true;
        }
        schedule(0L);
    }
    private void schedule(long delay){
        synchronized(this){if(closed)return;}
        scheduler.schedule(this::requestNext,delay,TimeUnit.MILLISECONDS);
    }
    private void requestNext(){
        final Long userId;
        synchronized(this){
            if(closed){inFlight=false;return;}
            userId=pending.pollFirst();
            if(userId==null){inFlight=false;return;}
        }
        try {
            requester.send(new TdApi.GetUserFullInfo(userId),new Callback(){
                @Override public void onResult(TdApi.Object result){
                    synchronized(TdUserFullInfoQueue.this){
                        retries.remove(userId);
                        queued.remove(userId);
                        if(result instanceof TdApi.UserFullInfo)completed.add(userId);
                    }
                    if(result instanceof TdApi.UserFullInfo&&listener!=null)
                        listener.onUserFullInfo(userId,(TdApi.UserFullInfo)result);
                    complete(GAP_MS);
                }
                @Override public void onError(TdApi.Error error){
                    long delay=GAP_MS;
                    synchronized(TdUserFullInfoQueue.this){
                        if(error!=null&&error.code==429){
                            int attempt=retries.getOrDefault(userId,0)+1;
                            if(attempt<=MAX_RETRIES){
                                retries.put(userId,attempt);pending.addFirst(userId);
                                delay=retryDelay(error.message,attempt);
                            }else { retries.remove(userId); queued.remove(userId); }
                        }else { retries.remove(userId); queued.remove(userId); }
                    }
                    complete(delay);
                }
            });
        } catch (RuntimeException failure) {
            synchronized (this) { retries.remove(userId); queued.remove(userId); }
            complete(GAP_MS);
        }
    }
    private void complete(long delay){
        synchronized(this){
            if(closed){inFlight=false;return;}
            if(pending.isEmpty()){inFlight=false;return;}
        }
        schedule(delay);
    }
    private long retryDelay(String message,int attempt){
        if(message!=null){
            Matcher matcher=RETRY_AFTER.matcher(message);
            if(matcher.find())try{
                return Math.min(MAX_BACKOFF_MS,Math.max(1_000L,
                        Long.parseLong(matcher.group(1))*1_000L));
            }catch(NumberFormatException ignored){}
        }
        return Math.min(MAX_BACKOFF_MS,1_000L<<Math.min(attempt,4));
    }
    @Override public synchronized void close(){
        closed=true;inFlight=false;pending.clear();queued.clear();completed.clear();
        retries.clear();scheduler.shutdownNow();
    }
}
