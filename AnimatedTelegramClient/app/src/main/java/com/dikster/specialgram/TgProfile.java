package com.dikster.specialgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable profile model used by native profile UI. */
public final class TgProfile {
    private final long chatId,userId;
    private final String name,kind,phone,bio,avatarPath,username,status,birthdate;
    private final boolean online,premium,verified,canBeCalled,supportsVideoCalls;
    private final int commonGroups,giftCount;
    private final List<String> gifts,photoPaths;

    public TgProfile(long chatId,String name,String kind,String phone,String bio,String avatarPath,
                     String username,String status,boolean online,boolean premium,boolean verified,int commonGroups){
        this(chatId,name,kind,phone,bio,avatarPath,username,status,online,premium,verified,commonGroups,
                "",0,false,false,Collections.emptyList(),0L);
    }

    public TgProfile(long chatId,String name,String kind,String phone,String bio,String avatarPath,
                     String username,String status,boolean online,boolean premium,boolean verified,int commonGroups,
                     String birthdate,int giftCount,boolean canBeCalled,boolean supportsVideoCalls,List<String> gifts){
        this(chatId,name,kind,phone,bio,avatarPath,username,status,online,premium,verified,commonGroups,
                birthdate,giftCount,canBeCalled,supportsVideoCalls,gifts,0L,Collections.emptyList());
    }

    public TgProfile(long chatId,String name,String kind,String phone,String bio,String avatarPath,
                     String username,String status,boolean online,boolean premium,boolean verified,int commonGroups,
                     String birthdate,int giftCount,boolean canBeCalled,boolean supportsVideoCalls,List<String> gifts,long userId){
        this(chatId,name,kind,phone,bio,avatarPath,username,status,online,premium,verified,commonGroups,
                birthdate,giftCount,canBeCalled,supportsVideoCalls,gifts,userId,Collections.emptyList());
    }

    public TgProfile(long chatId,String name,String kind,String phone,String bio,String avatarPath,
                     String username,String status,boolean online,boolean premium,boolean verified,int commonGroups,
                     String birthdate,int giftCount,boolean canBeCalled,boolean supportsVideoCalls,
                     List<String> gifts,long userId,List<String> photoPaths){
        this.chatId=chatId;this.userId=userId;this.name=safe(name);this.kind=safe(kind);this.phone=safe(phone);
        this.bio=safe(bio);this.avatarPath=safe(avatarPath);this.username=safe(username);
        this.status=safe(status);this.online=online;this.premium=premium;this.verified=verified;
        this.commonGroups=commonGroups;this.birthdate=safe(birthdate);this.giftCount=Math.max(0,giftCount);
        this.canBeCalled=canBeCalled;this.supportsVideoCalls=supportsVideoCalls;
        this.gifts=Collections.unmodifiableList(gifts==null?new ArrayList<>():new ArrayList<>(gifts));
        this.photoPaths=Collections.unmodifiableList(photoPaths==null?new ArrayList<>():new ArrayList<>(photoPaths));
    }

    public TgProfile withGifts(List<String> values){
        return new TgProfile(chatId,name,kind,phone,bio,avatarPath,username,status,online,premium,verified,
                commonGroups,birthdate,giftCount,canBeCalled,supportsVideoCalls,values,userId,photoPaths);
    }

    public TgProfile withPhotoPaths(List<String> values){
        return new TgProfile(chatId,name,kind,phone,bio,avatarPath,username,status,online,premium,verified,
                commonGroups,birthdate,giftCount,canBeCalled,supportsVideoCalls,gifts,userId,values);
    }
    public TgProfile withPhotoPath(int index,String path){
        List<String> values=new ArrayList<>(photoPaths);
        while(values.size()<=index)values.add("");
        values.set(index,safe(path));
        return withPhotoPaths(values);
    }
    public List<String> getPhotoPaths(){return photoPaths;}

    public long getChatId(){return chatId;}public long getUserId(){return userId;}public String getName(){return name;}public String getKind(){return kind;}
    public String getPhone(){return phone;}public String getBio(){return bio;}public String getAvatarPath(){return avatarPath;}
    public String getUsername(){return username;}public String getStatus(){return status;}public String getBirthdate(){return birthdate;}
    public boolean isOnline(){return online;}public boolean isPremium(){return premium;}
    public boolean isVerified(){return verified;}public int getCommonGroups(){return commonGroups;}
    public int getGiftCount(){return giftCount;}public boolean canBeCalled(){return canBeCalled;}
    public boolean supportsVideoCalls(){return supportsVideoCalls;}public List<String> getGifts(){return gifts;}

    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof TgProfile))return false;
        TgProfile v=(TgProfile)o;return chatId==v.chatId&&userId==v.userId&&online==v.online&&premium==v.premium&&verified==v.verified
                &&commonGroups==v.commonGroups&&giftCount==v.giftCount&&canBeCalled==v.canBeCalled
                &&supportsVideoCalls==v.supportsVideoCalls&&name.equals(v.name)&&kind.equals(v.kind)
                &&phone.equals(v.phone)&&bio.equals(v.bio)&&avatarPath.equals(v.avatarPath)
                &&username.equals(v.username)&&status.equals(v.status)&&birthdate.equals(v.birthdate)&&gifts.equals(v.gifts)&&photoPaths.equals(v.photoPaths);}
    @Override public int hashCode(){return Objects.hash(chatId,userId,name,kind,phone,bio,avatarPath,username,status,
            online,premium,verified,commonGroups,birthdate,giftCount,canBeCalled,supportsVideoCalls,gifts,photoPaths);}
    private static String safe(String v){return v==null?"":v;}
}
