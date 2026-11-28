package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Synchronizes the current Telegram account model and its profile-photo download. */
public final class TdAccountStateController {
    public interface Listener {
        TgAccount account();
        void setAccount(TgAccount account);
        void setMyUserId(long userId);
        void setMyPhotoFileId(Integer fileId);
        void enqueueUserFullInfo(long userId);
        void enqueueFileDownload(int fileId);
    }

    private final Listener listener;

    public TdAccountStateController(Listener listener) {
        this.listener = listener;
    }

    public void updateAccount(TdApi.User user) {
        listener.setMyUserId(user.id);
        listener.setAccount(TdUserMapper.toAccount(user, listener.account()));
        listener.enqueueUserFullInfo(user.id);

        int photoId = user.profilePhoto == null || user.profilePhoto.small == null
                ? 0 : user.profilePhoto.small.id;
        listener.setMyPhotoFileId(photoId > 0 ? photoId : null);
        if (photoId > 0) {
            listener.enqueueFileDownload(photoId);
        } else {
            TgAccount account = listener.account();
            if (account != null) listener.setAccount(new TgAccount(account.getName(),
                    account.getUsername(), account.getPhone(), account.isPremium(),
                    account.isVerified(), account.getBio(), "", account.getFirstName(),
                    account.getLastName()));
        }
    }
}
