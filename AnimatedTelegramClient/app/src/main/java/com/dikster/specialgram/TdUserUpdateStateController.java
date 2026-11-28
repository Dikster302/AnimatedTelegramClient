package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.List;
import java.util.Map;

/** Applies user, presence and privacy updates to the Java-owned application state. */
public final class TdUserUpdateStateController implements TdUserUpdateRouter.Listener {
    public interface State {
        List<TgChat> chats();
        void setChats(List<TgChat> chats);
        List<TgContact> contacts();
        void setContacts(List<TgContact> contacts);
        TgProfile profile();
        void setProfile(TgProfile profile);
        Long myUserId();
        void setLastSeenPrivacy(String value);
        void setPhonePrivacy(String value);
        void setPhotoPrivacy(String value);
    }

    private final State state;
    private final Map<Long, Long> userToChat;
    private final TdProfileController profileController;
    private final TdAccountStateController accountController;

    public TdUserUpdateStateController(State state, Map<Long, Long> userToChat,
                                       TdProfileController profileController,
                                       TdAccountStateController accountController) {
        this.state = state;
        this.userToChat = userToChat;
        this.profileController = profileController;
        this.accountController = accountController;
    }

    @Override public void onUserStatus(long userId, TdApi.UserStatus status) {
        TdUserStateController.State updated = TdUserStateController.refreshStatus(
                state.chats(), state.contacts(), state.profile(), userToChat, userId, status);
        state.setChats(updated.chats);
        state.setContacts(updated.contacts);
        state.setProfile(updated.profile);
    }

    @Override public void onPrivacyRules(TdApi.UserPrivacySetting setting,
                                         TdApi.UserPrivacySettingRules rules) {
        String value = TdPrivacyController.label(rules);
        if (setting instanceof TdApi.UserPrivacySettingShowStatus) state.setLastSeenPrivacy(value);
        else if (setting instanceof TdApi.UserPrivacySettingShowPhoneNumber) state.setPhonePrivacy(value);
        else if (setting instanceof TdApi.UserPrivacySettingShowProfilePhoto) state.setPhotoPrivacy(value);
    }

    @Override public void onUser(TdApi.User user) {
        profileController.updateUser(user);
        state.setChats(TdUserStateController.updateChatUser(state.chats(), userToChat, user));
        state.setContacts(TdUserStateController.updateContact(state.contacts(), user));
        Long myUserId = state.myUserId();
        if (myUserId != null && myUserId == user.id) accountController.updateAccount(user);
    }
}
