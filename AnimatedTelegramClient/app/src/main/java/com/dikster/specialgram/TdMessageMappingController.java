package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.Map;


/** Bridges TDLib message conversion to the application's current reply and download state. */
public final class TdMessageMappingController {
    public interface Listener {
        String replyText(long messageId);
        void enqueueMediaFile(TdApi.File file, long chatId, long messageId, String kind);
    }

    private final Listener listener;
    private final Map<Integer, TdPair<Long, Long>> videoFileToMessage;
    private final Map<Integer, TdPair<Long, Long>> mediaFileToMessage;

    public TdMessageMappingController(Listener listener,
                                      Map<Integer, TdPair<Long, Long>> videoFileToMessage,
                                      Map<Integer, TdPair<Long, Long>> mediaFileToMessage) {
        if (listener == null) throw new IllegalArgumentException("listener == null");
        this.listener = listener;
        this.videoFileToMessage = videoFileToMessage;
        this.mediaFileToMessage = mediaFileToMessage;
    }

    public TgMessage map(TdApi.Message message, long readMarker) {
        return TdMessageMapper.map(message, readMarker, listener::replyText,
                (file, messageId, kind) -> {
                    TdPair<Long, Long> key = new TdPair<>(message.chatId, messageId);
                    if ("video".equals(kind) || "videoNote".equals(kind))
                        videoFileToMessage.put(file.id, key);
                    mediaFileToMessage.put(file.id, key);
                    listener.enqueueMediaFile(file, message.chatId, messageId, kind);
                });
    }
}
