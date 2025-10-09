package com.chat;

import java.io.Serializable;

public class ChatMessage implements Serializable //to use Outputobject stream the object must implement serializable
 {
    static final int WHOISIN=0,MESSAGE=1,LOGOUT=2;
    private int type;
    private String message;
    ChatMessage(int type, String message)
    {
        this.type=type;
        this.message=message;
    }
    String getMessage(){
        return message;
    }
    int getType()
    {
        return type;
    }
    
}
