package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Account name, bio and profile-photo operations. */
public final class TdAccountActionsController {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }
    public interface Listener {
        void onAccountUser(TdApi.User user);
        void onBioUpdated(String bio);
        void onValidationError(String message);
    }

    private final Requester requester;
    private final Listener listener;

    public TdAccountActionsController(Requester requester, Listener listener) {
        this.requester = requester;
        this.listener = listener;
    }

    public void updateProfile(String firstName, String lastName, String bio, long userId) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String about = bio == null ? "" : bio.trim().replace('\n', ' ');
        if (first.isEmpty()) {
            listener.onValidationError("Имя не может быть пустым");
            return;
        }
        if (first.length() > 64 || last.length() > 64) {
            listener.onValidationError("Имя и фамилия должны быть не длиннее 64 символов");
            return;
        }

        requester.send(new TdApi.SetName(first, last), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.Ok) refreshAccount();
            }
            @Override public void onError(TdApi.Error error) { }
        });

        requester.send(new TdApi.SetBio(about), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.Ok) || userId <= 0) return;
                requester.send(new TdApi.GetUserFullInfo(userId), new Callback() {
                    @Override public void onResult(TdApi.Object info) {
                        if (info instanceof TdApi.UserFullInfo)
                            TdApi.FormattedText value = ((TdApi.UserFullInfo) info).bio;
                        listener.onBioUpdated(value == null ? "" : value.text);
                    }
                    @Override public void onError(TdApi.Error error) { }
                });
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public void setProfilePhoto(String path, long userId) {
        requester.send(new TdApi.SetProfilePhoto(
                new TdApi.InputChatPhotoStatic(new TdApi.InputFileLocal(path)), false),
                new Callback() {
                    @Override public void onResult(TdApi.Object result) {
                        if (result instanceof TdApi.Ok && userId > 0) refreshAccount();
                    }
                    @Override public void onError(TdApi.Error error) { }
                });
    }

    private void refreshAccount() {
        requester.send(new TdApi.GetMe(), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.User) listener.onAccountUser((TdApi.User) result);
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }
}
