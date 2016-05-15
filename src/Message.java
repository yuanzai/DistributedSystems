/**
 * Created by junyuanlau on 28/4/16.
 */
import com.google.gson.Gson;
import com.sun.javafx.collections.MappingChange;

import java.util.HashMap;

public class Message {
    public enum MSGTYPE {ORDER, NODE, DATA, CASHBALANCE, PING}
    public MSGTYPE msgtype;
    public Address sender;
    public Address receiver;
    public String payload;

    public Message(){}

    public Message(MSGTYPE msgtype, Address sender, Address receiver, String payload) {
        this.msgtype = msgtype;
        this.payload = payload;
        this.sender = sender;
        this.receiver = receiver;
    }


    public String getMessage() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public byte[] getMessageByteArray(){
        return getMessage().getBytes();
    }

    public static Message readMessage(byte[] data) {
        Gson gson = new Gson();
        return gson.fromJson(new String(data), Message.class);
    }

    public static Message readMessage(String data) {
        Gson gson = new Gson();
        return gson.fromJson(data, Message.class);
    }

    public Message(String msgString, ExchangeNode node) {

    }
}
