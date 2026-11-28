package com.dikster.specialgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable message view model shared by TDLib mapping and native message views. */
public final class TgMessage {
    private final long id,replyToMessageId,mediaSize;
    private final String text,videoShape,videoPath,replyToText,photoPath,mediaKind,mediaPath,mediaName,mediaMimeType;
    private final boolean outgoing,read,pinned;
    private final int date,mediaDuration;
    private final List<TgReaction> reactions;
    public TgMessage(long id,String text,boolean outgoing,int date,boolean read,String videoShape,String videoPath,
                     long replyToMessageId,String replyToText,List<TgReaction> reactions,String photoPath,
                     String mediaKind,String mediaPath,String mediaName,String mediaMimeType,int mediaDuration,
                     long mediaSize,boolean pinned){
        this.id=id;this.text=safe(text);this.outgoing=outgoing;this.date=date;this.read=read;
        this.videoShape=safe(videoShape);this.videoPath=safe(videoPath);this.replyToMessageId=replyToMessageId;
        this.replyToText=safe(replyToText);this.reactions=Collections.unmodifiableList(
                reactions==null?new ArrayList<>():new ArrayList<>(reactions));
        this.photoPath=safe(photoPath);this.mediaKind=safe(mediaKind);this.mediaPath=safe(mediaPath);
        this.mediaName=safe(mediaName);this.mediaMimeType=safe(mediaMimeType);this.mediaDuration=mediaDuration;
        this.mediaSize=mediaSize;this.pinned=pinned;
    }
    public long getId(){return id;}public String getText(){return text;}public boolean isOutgoing(){return outgoing;}
    public int getDate(){return date;}public boolean getOutgoing(){return outgoing;}public boolean isRead(){return read;}public String getVideoShape(){return videoShape;}
    public String getVideoPath(){return videoPath;}public long getReplyToMessageId(){return replyToMessageId;}
    public String getReplyToText(){return replyToText;}public List<TgReaction> getReactions(){return reactions;}
    public String getPhotoPath(){return photoPath;}public String getMediaKind(){return mediaKind;}
    public String getMediaPath(){return mediaPath;}public String getMediaName(){return mediaName;}
    public String getMediaMimeType(){return mediaMimeType;}public int getMediaDuration(){return mediaDuration;}
    public long getMediaSize(){return mediaSize;}public boolean isPinned(){return pinned;}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof TgMessage))return false;
        TgMessage v=(TgMessage)o;return id==v.id&&outgoing==v.outgoing&&date==v.date&&read==v.read
                &&replyToMessageId==v.replyToMessageId&&mediaDuration==v.mediaDuration&&mediaSize==v.mediaSize
                &&pinned==v.pinned&&text.equals(v.text)&&videoShape.equals(v.videoShape)&&videoPath.equals(v.videoPath)
                &&replyToText.equals(v.replyToText)&&reactions.equals(v.reactions)&&photoPath.equals(v.photoPath)
                &&mediaKind.equals(v.mediaKind)&&mediaPath.equals(v.mediaPath)&&mediaName.equals(v.mediaName)
                &&mediaMimeType.equals(v.mediaMimeType);}
    @Override public int hashCode(){return Objects.hash(id,text,outgoing,date,read,videoShape,videoPath,
            replyToMessageId,replyToText,reactions,photoPath,mediaKind,mediaPath,mediaName,mediaMimeType,
            mediaDuration,mediaSize,pinned);}
    private static String safe(String v){return v==null?"":v;}
}
