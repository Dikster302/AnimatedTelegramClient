package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Coordinates user profile and private-chat creation requests. */
public final class TdUserActionsController {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }
    public interface ProfileListener {
        void onChat(TdApi.Chat chat);
        void onUser(TdApi.User user);
        void onFullInfo(long userId, TdApi.UserFullInfo info);
        void onPhotos(long userId,TdApi.ChatPhotos photos);
    }
    public interface GiftsListener {
        void onGifts(TdApi.ReceivedGifts gifts);
        void onError(TdApi.Error error);
    }
    public interface AvailableGiftsListener {
        void onGifts(java.util.List<TgGiftOption> gifts);
        void onError(TdApi.Error error);
    }

    private final Requester requester;

    public TdUserActionsController(Requester requester) {
        this.requester = requester;
    }

    public void loadProfile(long chatId, ProfileListener listener) {
        requester.send(new TdApi.GetChat(chatId), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.Chat)) return;
                TdApi.Chat chat = (TdApi.Chat) result;
                listener.onChat(chat);
                if (!(chat.type instanceof TdApi.ChatTypePrivate)) return;
                long userId = ((TdApi.ChatTypePrivate) chat.type).userId;
                requester.send(new TdApi.GetUser(userId), new Callback() {
                    @Override public void onResult(TdApi.Object user) {
                        if (user instanceof TdApi.User) listener.onUser((TdApi.User) user);
                    }
                    @Override public void onError(TdApi.Error error) { }
                });
                requester.send(new TdApi.GetUserProfilePhotos(userId,0,100),new Callback(){
                    @Override public void onResult(TdApi.Object photos){
                        if(photos instanceof TdApi.ChatPhotos)listener.onPhotos(userId,(TdApi.ChatPhotos)photos);
                    }
                    @Override public void onError(TdApi.Error error){ }
                });
                requester.send(new TdApi.GetUserFullInfo(userId), new Callback() {
                    @Override public void onResult(TdApi.Object info) {
                        if (info instanceof TdApi.UserFullInfo)
                            listener.onFullInfo(userId, (TdApi.UserFullInfo) info);
                    }
                    @Override public void onError(TdApi.Error error) { }
                });
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public void loadProfileGifts(long userId, GiftsListener listener) {
        TdApi.GetReceivedGifts request = new TdApi.GetReceivedGifts();
        request.businessConnectionId = "";
        request.ownerId = new TdApi.MessageSenderUser(userId);
        request.collectionId = 0;
        request.excludeUnsaved = true;
        request.excludeSaved = false;
        request.excludeUnlimited = false;
        request.excludeUpgradable = false;
        request.excludeNonUpgradable = false;
        request.excludeUpgraded = false;
        request.excludeWithoutColors = false;
        request.excludeHosted = false;
        request.sortByPrice = false;
        request.offset = "";
        request.limit = 50;
        requester.send(request, new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.ReceivedGifts) listener.onGifts((TdApi.ReceivedGifts) result);
            }
            @Override public void onError(TdApi.Error error) {
                if (listener != null) listener.onError(error);
            }
        });
    }

    public void loadAvailableGifts(AvailableGiftsListener listener) {
        requester.send(new TdApi.GetAvailableGifts(), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.AvailableGifts)) return;
                java.util.List<TgGiftOption> options = new java.util.ArrayList<>();
                TdApi.AvailableGift[] available = ((TdApi.AvailableGifts) result).gifts;
                long now = System.currentTimeMillis() / 1000L;
                if (available != null) for (TdApi.AvailableGift item : available) {
                    if (item == null || item.gift == null) continue;
                    TdApi.Gift gift = item.gift;
                    if (gift.auctionInfo != null || (gift.nextSendDate > now && gift.nextSendDate != 0)) continue;
                    String emoji = gift.sticker == null ? "🎁" : gift.sticker.emoji;
                    options.add(new TgGiftOption(gift.id,emoji,gift.starCount,gift.upgradeStarCount,gift.isPremium));
                }
                if (listener != null) listener.onGifts(options);
            }
            @Override public void onError(TdApi.Error error) {
                if (listener != null) listener.onError(error);
            }
        });
    }

    public void sendGift(long userId,long giftId,Callback callback) {
        TdApi.SendGift request = new TdApi.SendGift(giftId,new TdApi.MessageSenderUser(userId),
                new TdApi.FormattedText("",new TdApi.TextEntity[0]),false,false);
        requester.send(request,callback);
    }

    public void createPrivateChat(long userId, Callback callback) {
        requester.send(new TdApi.CreatePrivateChat(userId, false), callback);
    }

    public void createBasicGroup(long[] userIds, String title, Callback callback) {
        requester.send(new TdApi.CreateNewBasicGroupChat(userIds, title, 0), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.CreatedBasicGroupChat)) {
                    if (callback != null) callback.onError(new TdApi.Error(500, "Unexpected group creation response"));
                    return;
                }
                long chatId = ((TdApi.CreatedBasicGroupChat) result).chatId;
                requester.send(new TdApi.GetChat(chatId), callback);
            }
            @Override public void onError(TdApi.Error error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
}
