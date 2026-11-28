package com.dikster.specialgram;

/** Sendable Telegram gift metadata used by the native gift catalog. */
public final class TgGiftOption {
    private final long id,starCount,upgradeStarCount;
    private final String emoji;
    private final boolean premium;
    public TgGiftOption(long id,String emoji,long starCount,long upgradeStarCount,boolean premium){
        this.id=id;this.emoji=emoji==null||emoji.trim().isEmpty()?"🎁":emoji;
        this.starCount=starCount;this.upgradeStarCount=upgradeStarCount;this.premium=premium;
    }
    public long getId(){return id;}public String getEmoji(){return emoji;}
    public long getStarCount(){return starCount;}public long getUpgradeStarCount(){return upgradeStarCount;}
    public boolean isPremium(){return premium;}
}
