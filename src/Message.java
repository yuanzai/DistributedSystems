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
    public enum MSGTYPE {TICK, TICKPRICES, TICKISSUE, TICKTIME,
        ORDER, ORDERFILLED, CASHBALANCE, PING,  TERMINATE, UPDATECASH,
        NODE_RESTART, NODE_NEW_LOCAL, NODE_SUPER, NODE_SETBACKUPSUPER,
        PAXOS,
        DATA_INVENTORY, DATA_HOLDINGS, GET_DATA_INVENTORY, GET_DATA_HOLDINGS, DATA_SUPERNODE,
        ERROR
    }
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

        try {
            return gson.fromJson(data, Message.class);
        } catch (com.google.gson.JsonSyntaxException e){
            e.printStackTrace();

        }
        return null;
    }

    public Message(String msgString, ExchangeNode node) {

    }

    public static String sendMessageToLocal(Message message) {
        return sendMessage(message, message.receiver);
    }

    public static String sendMessage(Message message, Address address) {
        if (message.msgtype == MSGTYPE.ORDERFILLED)
            System.out.println(message.receiver.port);
        if (message == null)
            return "";
        if (address == null)
            return "";

        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        int failCount = 0;
        while(failCount<5) {
            try {
                echoSocket = new Socket(address.host, address.port);
                out = new PrintWriter(echoSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
                failCount = 5;
            } catch (IOException e) {
                if (failCount >= 5)
                    return "FAIL";

                String name = "";
                if (message != null) {
                    if (message.sender != null) {
                        if (message.sender.name != null)
                            name = message.sender.name;
                    }
                }

                System.out.println(name + " CONNECT FAIL " + address.name + " ON " + address.port);
                failCount++;
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                message.msgtype = MSGTYPE.ERROR;
                return message.getMessage();

            }
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

    public static void ping(int port){
        Message message = new Message(MSGTYPE.PING,null,new Address("localhost", port),"");
        sendMessageToLocal(message);
    }

}
