package com.dikster.specialgram;

import java.util.List;
import java.util.Map;

/** Coordinates animated send acknowledgement with the incremental edit scheduler. */
public final class TdAnimatedMessageSendController {
    private final TdAnimatedMessageController animatedMessages;
    private final Map<Long, Long> confirmedIds;
    private final Map<Long, List<String>> pendingEdits;
    private final Runnable clearError;

    public TdAnimatedMessageSendController(TdAnimatedMessageController animatedMessages,
                                           Map<Long, Long> confirmedIds,
                                           Map<Long, List<String>> pendingEdits,
                                           Runnable clearError) {
        this.animatedMessages = animatedMessages;
        this.confirmedIds = confirmedIds;
        this.pendingEdits = pendingEdits;
        this.clearError = clearError;
    }

    public void send(long chatId, String text, Long replyToMessageId) {
        if (clearError != null) clearError.run();
        animatedMessages.sendAnimated(chatId, text, replyToMessageId,
                (message, characters) -> {
                    if (characters.size() <= 1) return;
                    Long serverId = confirmedIds.get(message.id);
                    if (serverId != null) animatedMessages.schedule(chatId, serverId, characters);
                    else pendingEdits.put(message.id, characters);
                });
    }
}
