package com.dikster.specialgram;

/** TDLib authorization states exposed as Java types to the native application layer. */
public class Auth {
    public static final Auth Loading=new Auth();
    public static final Auth Phone=new Auth();
    public static final Auth Code=new Auth();
    public static final Auth Password=new Auth();
    public static final Auth Ready=new Auth();

    private Auth(){}

    public static final class Error extends Auth {
        private final String text;
        public Error(String text){this.text=text==null?"Неизвестная ошибка":text;}
        public String getText(){return text;}
    }
}
