package com.dikster.specialgram;

import java.util.Objects;

/** Immutable story metadata used by story strip and viewer. */
public final class TgStory {
    private final long chatId;private final int storyId,date;
    private final String ownerName,caption,path;private final boolean video,viewed;
    public TgStory(long chatId,int storyId,String ownerName,int date,String caption,
                   boolean video,String path,boolean viewed){
        this.chatId=chatId;this.storyId=storyId;this.ownerName=safe(ownerName);this.date=date;
        this.caption=safe(caption);this.video=video;this.path=safe(path);this.viewed=viewed;
    }
    public long getChatId(){return chatId;} public int getStoryId(){return storyId;}
    public String getOwnerName(){return ownerName;} public int getDate(){return date;}
    public String getCaption(){return caption;} public boolean isVideo(){return video;}
    public String getPath(){return path;} public boolean isViewed(){return viewed;}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof TgStory))return false;
        TgStory v=(TgStory)o;return chatId==v.chatId&&storyId==v.storyId&&date==v.date&&video==v.video
                &&viewed==v.viewed&&ownerName.equals(v.ownerName)&&caption.equals(v.caption)&&path.equals(v.path);}
    @Override public int hashCode(){return Objects.hash(chatId,storyId,ownerName,date,caption,video,path,viewed);}
    private static String safe(String v){return v==null?"":v;}
}
