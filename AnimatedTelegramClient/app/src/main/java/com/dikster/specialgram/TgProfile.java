package com.dikster.specialgram;

import java.util.Objects;

/** Immutable profile model used by native profile UI. */
public final class TgProfile {
    private final long chatId;private final String name,kind,phone,bio,avatarPath,username,status;
    private final boolean online,premium,verified;private final int commonGroups;
    public TgProfile(long chatId,String name,String kind,String phone,String bio,String avatarPath,
                     String username,String status,boolean online,boolean premium,boolean verified,int commonGroups){
        this.chatId=chatId;this.name=safe(name);this.kind=safe(kind);this.phone=safe(phone);
        this.bio=safe(bio);this.avatarPath=safe(avatarPath);this.username=safe(username);
        this.status=safe(status);this.online=online;this.premium=premium;this.verified=verified;
        this.commonGroups=commonGroups;
    }
    public long getChatId(){return chatId;}public String getName(){return name;}public String getKind(){return kind;}
    public String getPhone(){return phone;}public String getBio(){return bio;}public String getAvatarPath(){return avatarPath;}
    public String getUsername(){return username;}public String getStatus(){return status;}
    public boolean isOnline(){return online;}public boolean isPremium(){return premium;}
    public boolean isVerified(){return verified;}public int getCommonGroups(){return commonGroups;}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof TgProfile))return false;
        TgProfile v=(TgProfile)o;return chatId==v.chatId&&online==v.online&&premium==v.premium&&verified==v.verified
                &&commonGroups==v.commonGroups&&name.equals(v.name)&&kind.equals(v.kind)&&phone.equals(v.phone)
                &&bio.equals(v.bio)&&avatarPath.equals(v.avatarPath)&&username.equals(v.username)&&status.equals(v.status);}
    @Override public int hashCode(){return Objects.hash(chatId,name,kind,phone,bio,avatarPath,username,status,online,premium,verified,commonGroups);}
    private static String safe(String v){return v==null?"":v;}
}
