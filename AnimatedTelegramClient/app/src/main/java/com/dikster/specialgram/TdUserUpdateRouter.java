package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Routes TDLib user and privacy updates to the application state adapter. */
public final class TdUserUpdateRouter {
    public interface Listener {
        void onUserStatus(long userId, TdApi.UserStatus status);
        void onPrivacyRules(TdApi.UserPrivacySetting setting, TdApi.UserPrivacySettingRules rules);
        void onUser(TdApi.User user);
    }

    private final Listener listener;

    public TdUserUpdateRouter(Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener == null");
        this.listener = listener;
    }

    /** @return true when the update was handled by this router. */
    public boolean route(TdApi.Object update) {
        if (update instanceof TdApi.UpdateUserStatus) {
            TdApi.UpdateUserStatus value = (TdApi.UpdateUserStatus) update;
            listener.onUserStatus(value.userId, value.status);
            return true;
        }
        if (update instanceof TdApi.UpdateUserPrivacySettingRules) {
            TdApi.UpdateUserPrivacySettingRules value =
                    (TdApi.UpdateUserPrivacySettingRules) update;
            listener.onPrivacyRules(value.setting, value.rules);
            return true;
        }
        if (update instanceof TdApi.UpdateUser) {
            listener.onUser(((TdApi.UpdateUser) update).user);
            return true;
        }
        return false;
    }
}
