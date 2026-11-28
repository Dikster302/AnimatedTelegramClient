package com.dikster.specialgram;

import java.util.Objects;

/** Immutable contact row model shared by Java UI and TDLib mapping. */
public final class TgContact {
    private final long id;
    private final String name,username,phone,status;
    private final boolean online,premium;
    public TgContact(long id,String name,String username,String phone,String status,boolean online,boolean premium){
        this.id=id;this.name=safe(name);this.username=safe(username);this.phone=safe(phone);
        this.status=safe(status);this.online=online;this.premium=premium;
    }
    public long getId(){return id;} public String getName(){return name;}
    public String getUsername(){return username;} public String getPhone(){return phone;}
    public String getStatus(){return status;} public boolean isOnline(){return online;}
    public boolean isPremium(){return premium;}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof TgContact))return false;
        TgContact v=(TgContact)o;return id==v.id&&online==v.online&&premium==v.premium&&name.equals(v.name)
                &&username.equals(v.username)&&phone.equals(v.phone)&&status.equals(v.status);}
    @Override public int hashCode(){return Objects.hash(id,name,username,phone,status,online,premium);}
    private static String safe(String v){return v==null?"":v;}
}
