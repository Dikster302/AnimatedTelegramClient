package com.dikster.specialgram;

import java.util.Objects;

/** Immutable message reaction model shared by native Java views and TDLib mappers. */
public final class TgReaction {
    private final String emoji;
    private final int count;
    private final boolean chosen;
    public TgReaction(String emoji,int count,boolean chosen){
        this.emoji=emoji==null?"":emoji;this.count=count;this.chosen=chosen;
    }
    public String getEmoji(){return emoji;}
    public int getCount(){return count;}
    public boolean getChosen(){return chosen;}
    @Override public boolean equals(Object other){
        if(this==other)return true;
        if(!(other instanceof TgReaction))return false;
        TgReaction value=(TgReaction)other;
        return count==value.count&&chosen==value.chosen&&emoji.equals(value.emoji);
    }
    @Override public int hashCode(){return Objects.hash(emoji,count,chosen);}
}
