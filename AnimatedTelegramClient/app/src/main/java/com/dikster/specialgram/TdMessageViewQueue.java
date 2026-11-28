package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Batches read acknowledgements to avoid one ViewMessages request per incoming update. */
public final class TdMessageViewQueue implements AutoCloseable {
    public interface Callback { void onResult(TdApi.Object result); void onError(TdApi.Error error); }
    public interface Requester { void send(TdApi.Function<?> function,Callback callback); }

    private static final int BATCH_SIZE=100,MAX_SEEN_PER_CHAT=2048,MAX_RETRIES=5;
    private static final long COALESCE_MS=180L,REQUEST_GAP_MS=300L,MAX_BACKOFF_MS=30_000L;
    private static final Pattern RETRY_AFTER=Pattern.compile("retry after\\s+(\\d+)",Pattern.CASE_INSENSITIVE);
    private final Requester requester;
    private final ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor(r->{
        Thread thread=new Thread(r,"specialgram-message-views");thread.setDaemon(true);return thread;
    });
    private final LinkedHashMap<Long,ArrayDeque<Long>> pending=new LinkedHashMap<>();
    private final Map<Long,LinkedHashSet<Long>> seen=new HashMap<>();
    private final Map<Long,ArrayDeque<Long>> seenOrder=new HashMap<>();
    private boolean scheduled,inFlight,closed;
    private final Map<String,Integer> retryCounts=new HashMap<>();
    private String activeKey;
    private long activeChat;
    private long[] activeIds;
    private int activeRetries;

    public TdMessageViewQueue(Requester requester){this.requester=requester;}

    public void enqueue(long chatId,long[] messageIds){
        if(messageIds==null||messageIds.length==0)return;
        synchronized(this){
            if(closed)return;
            LinkedHashSet<Long> chatSeen=seen.computeIfAbsent(chatId,k->new LinkedHashSet<>());
            ArrayDeque<Long> order=seenOrder.computeIfAbsent(chatId,k->new ArrayDeque<>());
            ArrayDeque<Long> queue=pending.computeIfAbsent(chatId,k->new ArrayDeque<>());
            for(long id:messageIds){
                if(id<=0||!chatSeen.add(id))continue;
                order.addLast(id);queue.addLast(id);
                while(order.size()>MAX_SEEN_PER_CHAT){Long expired=order.removeFirst();chatSeen.remove(expired);}
            }
            if(queue.isEmpty())pending.remove(chatId);
            if(!pending.isEmpty()&&!scheduled&&!inFlight){scheduled=true;scheduler.schedule(this::sendNext,COALESCE_MS,TimeUnit.MILLISECONDS);}
        }
    }

    private void sendNext(){
        final long chatId;final long[] ids;
        synchronized(this){
            scheduled=false;
            if(closed||inFlight||pending.isEmpty())return;
            Map.Entry<Long,ArrayDeque<Long>> entry=pending.entrySet().iterator().next();
            chatId=entry.getKey();ArrayDeque<Long> queue=entry.getValue();
            int count=Math.min(BATCH_SIZE,queue.size());ids=new long[count];
            for(int i=0;i<count;i++)ids[i]=queue.removeFirst();
            if(queue.isEmpty())pending.remove(chatId);
            inFlight=true;activeChat=chatId;activeIds=ids;activeKey=chatId+":"+ids[0]+":"+ids[ids.length-1];activeRetries=retryCounts.getOrDefault(activeKey,0);
        }
        sendActive();
    }

    private void sendActive(){
        final long chatId;final long[] ids;
        synchronized(this){if(closed||!inFlight)return;chatId=activeChat;ids=activeIds;}
        requester.send(new TdApi.ViewMessages(chatId,ids,null,true),new Callback(){
            @Override public void onResult(TdApi.Object result){complete(REQUEST_GAP_MS,false,false);}
            @Override public void onError(TdApi.Error error){
                boolean retry=error!=null&&error.code==429;
                long delay=retry?retryDelay(error.message):REQUEST_GAP_MS;
                complete(delay,retry,true);
            }
        });
    }

    private void complete(long delay,boolean retry,boolean failed){
        synchronized(this){
            if(closed){inFlight=false;return;}
            if(retry&&activeRetries<MAX_RETRIES){
                activeRetries++;retryCounts.put(activeKey,activeRetries);
                ArrayDeque<Long> queue=pending.computeIfAbsent(activeChat,k->new ArrayDeque<>());
                for(int i=activeIds.length-1;i>=0;i--)queue.addFirst(activeIds[i]);
            }else{
                if(retry||failed){
                    LinkedHashSet<Long> chatSeen=seen.get(activeChat);
                    if(chatSeen!=null)for(long id:activeIds)chatSeen.remove(id);
                    ArrayDeque<Long> order=seenOrder.get(activeChat);
                    if(order!=null)order.removeIf(id->{for(long active:activeIds)if(active==id)return true;return false;});
                }
                activeRetries=0;retryCounts.remove(activeKey);
            }
            inFlight=false;activeIds=null;activeKey=null;
            if(!pending.isEmpty()&&!scheduled){
                scheduled=true;scheduler.schedule(this::sendNext,delay,TimeUnit.MILLISECONDS);
            }
        }
    }

    private long retryDelay(String message){
        if(message!=null){
            Matcher matcher=RETRY_AFTER.matcher(message);
            if(matcher.find())try{
                return Math.min(MAX_BACKOFF_MS,Math.max(1_000L,Long.parseLong(matcher.group(1))*1_000L));
            }catch(NumberFormatException ignored){}
        }
        return 2_000L;
    }

    @Override public synchronized void close(){
        closed=true;pending.clear();seen.clear();seenOrder.clear();retryCounts.clear();activeIds=null;activeKey=null;
        scheduler.shutdownNow();
    }
}
