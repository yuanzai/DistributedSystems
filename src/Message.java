/**
 * Created by junyuanlau on 28/4/16.
 */
import com.google.gson.Gson;
import com.sun.javafx.collections.MappingChange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

public class Message {
    public enum MSGTYPE {ORDER, NODE, DATA, CASHBALANCE, PING, TICK, LOCAL, SUPER, TERMINATE, UPDATECASH}
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

    public static String sendMessageToLocal(Message message) {
        return sendMessage(message, message.receiver);
    }

    public static String sendMessage(Message message, Address address) {
        if (message == null)
            return "";
        if (address == null)
            return "";
        System.out.println("SENDING TO " + address.host + " " + address.port + " " + message.msgtype);

        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            echoSocket = new Socket(address.host, address.port);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        } catch (IOException e) {
            return "FAIL";
        }

        out.println(message.getMessage());

        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                return inputLine;
            }

        } catch (IOException e) {
            return "FAIL";
        }
        return "FAIL";
    }

    public static void sendTerminate(int port){
        Message message = new Message(Message.MSGTYPE.TERMINATE,null,new Address("localhost", port),"");
        sendMessageToLocal(message);
    }
}
