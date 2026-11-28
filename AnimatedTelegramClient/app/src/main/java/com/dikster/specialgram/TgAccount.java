package com.dikster.specialgram;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable current-account profile model. */
public final class TgAccount {
    private final String name,username,phone,bio,avatarPath,firstName,lastName;
    private final boolean premium,verified;
    private final List<String> photoPaths;
    public TgAccount(String name,String username,String phone,boolean premium,boolean verified,
                     String bio,String avatarPath,String firstName,String lastName){
        this(name,username,phone,premium,verified,bio,avatarPath,firstName,lastName,Collections.emptyList());
    }
    public TgAccount(String name,String username,String phone,boolean premium,boolean verified,
                     String bio,String avatarPath,String firstName,String lastName,List<String> photoPaths){
        this.name=safe(name);this.username=safe(username);this.phone=safe(phone);this.premium=premium;
        this.verified=verified;this.bio=safe(bio);this.avatarPath=safe(avatarPath);
        this.firstName=safe(firstName);this.lastName=safe(lastName);
        this.photoPaths=Collections.unmodifiableList(photoPaths==null?new ArrayList<>():new ArrayList<>(photoPaths));
    }
    public String getName(){return name;}public String getUsername(){return username;}public String getPhone(){return phone;}
    public boolean isPremium(){return premium;}public boolean isVerified(){return verified;}
    public String getBio(){return bio;}public String getAvatarPath(){return avatarPath;}
    public String getFirstName(){return firstName;}public String getLastName(){return lastName;}
    public List<String> getPhotoPaths(){return photoPaths;}
    public TgAccount withPhotoPaths(List<String> paths){
        return new TgAccount(name,username,phone,premium,verified,bio,avatarPath,firstName,lastName,paths);
    }
    public TgAccount withPhotoPath(int index,String path){
        List<String> paths=new ArrayList<>(photoPaths);
        while(paths.size()<=index)paths.add("");
        paths.set(index,safe(path));
        return withPhotoPaths(paths);
    }
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof TgAccount))return false;
        TgAccount v=(TgAccount)o;return premium==v.premium&&verified==v.verified&&name.equals(v.name)
                &&username.equals(v.username)&&phone.equals(v.phone)&&bio.equals(v.bio)&&avatarPath.equals(v.avatarPath)
                &&firstName.equals(v.firstName)&&lastName.equals(v.lastName)&&photoPaths.equals(v.photoPaths);}
    @Override public int hashCode(){return Objects.hash(name,username,phone,premium,verified,bio,avatarPath,firstName,lastName,photoPaths);}
    private static String safe(String v){return v==null?"":v;}
}
