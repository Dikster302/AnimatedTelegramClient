package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies message-related TDLib updates and reconciles animated-message send IDs. */
public final class TdMessageStateController {
    public interface Listener {
        Long activeChatId();
        List<TgChat> chats();
        List<TgMessage> messages();
        void setMessages(List<TgMessage> messages);
        void deleteMessages(long chatId, List<Long> messageIds);
        TgMessage mapMessage(TdApi.Message message, long readMarker);
        void enqueueViewedMessages(long chatId, long[] messageIds);
    }

    private final Listener listener;
    private final TdAnimatedMessageController animatedMessages;
    private final Map<Long, Long> confirmedIds;
    private final Map<Long, List<String>> pendingEdits;

    public TdMessageStateController(Listener listener,
                                    TdAnimatedMessageController animatedMessages,
                                    Map<Long, Long> confirmedIds,
                                    Map<Long, List<String>> pendingEdits) {
        this.listener = listener;
        this.animatedMessages = animatedMessages;
        this.confirmedIds = confirmedIds;
        this.pendingEdits = pendingEdits;
    }

    public boolean handle(TdApi.Object update) {
        if (update instanceof TdApi.UpdateDeleteMessages) {
            TdApi.UpdateDeleteMessages value = (TdApi.UpdateDeleteMessages) update;
            if (isActive(value.chatId)) {
                Set<Long> deleted = new HashSet<>();
                for (long id : value.messageIds) deleted.add(id);
                List<TgMessage> messages = new ArrayList<>();
                for (TgMessage message : listener.messages())
                    if (!deleted.contains(message.getId())) messages.add(message);
                listener.deleteMessages(value.chatId, new ArrayList<>(deleted));
                listener.setMessages(messages);
            }
            return true;
        }
        if (update instanceof TdApi.UpdateNewMessage) {
            TdApi.Message message = ((TdApi.UpdateNewMessage) update).message;
            if (isActive(message.chatId)) {
                TgMessage mapped = listener.mapMessage(message, readMarker(message.chatId));
                String replyText = mapped.getReplyToText();
                for (TgMessage item : listener.messages()) {
                    if (item.getId() == mapped.getReplyToMessageId()) {
                        replyText = item.getText();
                        break;
                    }
                }
                List<TgMessage> messages = new ArrayList<>(listener.messages());
                messages.add(copy(mapped, mapped.getText(), mapped.isRead(), replyText,
                        mapped.isPinned(), mapped.getReactions()));
                LinkedHashMap<Long, TgMessage> unique = new LinkedHashMap<>();
                for (TgMessage item : messages) unique.putIfAbsent(item.getId(), item);
                messages = new ArrayList<>(unique.values());
                messages.sort((a, b) -> Integer.compare(a.getDate(), b.getDate()));
                listener.setMessages(messages);
                if (!message.isOutgoing) listener.enqueueViewedMessages(message.chatId,
                        new long[]{message.id});
            }
            return true;
        }
        if (update instanceof TdApi.UpdateMessageContent) {
            TdApi.UpdateMessageContent value = (TdApi.UpdateMessageContent) update;
            if (isActive(value.chatId)) {
                String text = TdMessageTextFormatter.display(value.newContent);
                updateMessage(value.chatId, value.messageId, text, null, null, null);
            }
            return true;
        }
        if (update instanceof TdApi.UpdateMessageIsPinned) {
            TdApi.UpdateMessageIsPinned value = (TdApi.UpdateMessageIsPinned) update;
            if (isActive(value.chatId))
                updateMessage(value.chatId, value.messageId, null, null, value.isPinned, null);
            return true;
        }
        if (update instanceof TdApi.UpdateMessageInteractionInfo) {
            TdApi.UpdateMessageInteractionInfo value = (TdApi.UpdateMessageInteractionInfo) update;
            if (isActive(value.chatId)) {
                List<TgReaction> reactions = TdMessageMapper.mapReactions(value.interactionInfo);
                updateMessage(value.chatId, value.messageId, null, null, null, reactions);
            }
            return true;
        }
        if (update instanceof TdApi.UpdateMessageSendSucceeded) {
            TdApi.UpdateMessageSendSucceeded value = (TdApi.UpdateMessageSendSucceeded) update;
            long oldId = value.oldMessageId;
            long newId = value.message.id;
            confirmedIds.put(oldId, newId);
            List<String> characters = pendingEdits.remove(oldId);
            if (characters != null)
                animatedMessages.schedule(value.message.chatId, newId, characters);
            return true;
        }
        return false;
    }

    private void updateMessage(long chatId, long messageId, String text, Boolean read,
                               Boolean pinned, List<TgReaction> reactions) {
        List<TgMessage> updated = new ArrayList<>(listener.messages().size());
        for (TgMessage message : listener.messages()) {
            if (message.getId() != messageId) {
                updated.add(message);
            } else {
                updated.add(copy(message,
                        text == null ? message.getText() : text,
                        read == null ? message.isRead() : read,
                        message.getReplyToText(),
                        pinned == null ? message.isPinned() : pinned,
                        reactions == null ? message.getReactions() : reactions));
            }
        }
        listener.setMessages(updated);
    }

    private long readMarker(long chatId) {
        for (TgChat chat : listener.chats())
            if (chat.getId() == chatId) return chat.getLastReadOutboxMessageId();
        return 0L;
    }

    private boolean isActive(long chatId) {
        Long active = listener.activeChatId();
        return active != null && active == chatId;
    }

    private static TgMessage copy(TgMessage message, String text, boolean read, String replyText,
                                  boolean pinned, List<TgReaction> reactions) {
        return new TgMessage(message.getId(), text, message.getOutgoing(), message.getDate(), read,
                message.getVideoShape(), message.getVideoPath(), message.getReplyToMessageId(),
                replyText, reactions, message.getPhotoPath(), message.getMediaKind(),
                message.getMediaPath(), message.getMediaName(), message.getMediaMimeType(),
                message.getMediaDuration(), message.getMediaSize(), pinned);
    }
}
