package com.dikster.specialgram;

/** Java callback used when a contact has been resolved to a private chat. */
public interface TdContactReadyListener {
    void onReady(TgChat chat);
}
