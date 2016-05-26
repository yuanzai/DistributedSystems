import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by junyuanlau on 9/5/16.
 */
public class Engine {

    DataCube dataCube;
    ServerSocket serverSocket;
    long datetime;
    Iterator<Long> datetimeIterator;
    public static Address engineAddress = new Address("localhost", 10000);
    HashMap<String, Address> superNodeAddress = new HashMap<String, Address>();
    HashMap<String, String> clientCountry;
    EngineThread engineThread;
    boolean terminate = false;

    public Engine() {

    }

    public static Engine launchEngineThread(){
        Engine engine = new Engine();
        engine.engineThread = new EngineThread("Engine", engine);
        engine.engineThread.start();
        return engine;
    }

    public void stopEngineThread(){
        System.out.println("STOPPING ENGINE");
        terminate = true;
    }

    public void start(){
        TradeManager.log.info("["+engineAddress.port+"] Engine listening");


        serverSocket = null;

        try {
            serverSocket = new ServerSocket(engineAddress.port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!terminate) {
            Socket clientSocket = null;

            try {
                clientSocket = serverSocket.accept();

            } catch (IOException e) {
                System.out.println("ENGINE ACCEPT FAIL");
                e.printStackTrace();
            }

            SocketThread socketThread = new SocketThread("Engine",clientSocket,this);
            socketThread.start();

        }


        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("TERMINATED ENGINE");
    }

    public void init() {
        dataCube = new DataCube();
    }

    public Message processMessage(Message message) {

        if (message.msgtype == Message.MSGTYPE.CASHBALANCE) {
            if (!dataCube.cashBalance.containsKey(message.payload))
                message.payload = "NOTFOUND";
            else
                message.payload = dataCube.cashBalance.get(message.payload).toString();

        } else if (message.msgtype == Message.MSGTYPE.NODE_SUPER) {
            if (message.payload.equals("SECONDARY")) {
                superNodeAddress.put(message.sender.region + "BACKUP", message.sender);
            } else {
                superNodeAddress.put(message.sender.region, message.sender);
            }

            Gson gson = new GsonBuilder().create();
            message.payload = gson.toJson(superNodeAddress);
            for (Map.Entry<String, Address> entry: superNodeAddress.entrySet()){
                if (!entry.getKey().equals(message.sender.region)) {
                    Message message1 = new Message(Message.MSGTYPE.NODE_SUPER,null,entry.getValue(),message.payload);
                    Message.sendMessageToLocal(message1);
                }
            }

        }else if (message.msgtype == Message.MSGTYPE.TERMINATE) {
            stopEngineThread();
        } else if (message.msgtype == Message.MSGTYPE.UPDATECASH) {
            dataCube.updateBalance(DataCube.getUpdateCashBalanceAmount(message.payload),
                    DataCube.getUpdateCashBalanceCpty(message.payload));
        } else if (message.msgtype == Message.MSGTYPE.TICK) {

            Gson gson = new Gson();
            if (message.payload.equals("DATETIME")){
                message.payload = String.valueOf(datetime);
            }else if (message.payload.equals("PRICES")){
                message.payload = gson.toJson(getPriceData(datetime,message.sender.name));
            }else if (message.payload.equals("ISSUE")){
                message.payload = gson.toJson(getIssueQuantity(datetime,message.sender.name));
            }else if (message.payload.equals("CLIENT")){
                message.payload = gson.toJson(clientCountry);
            }else if (message.payload.equals("REGION")){
                message.payload = gson.toJson(dataCube.countryToContinent);
            }

        }

        return message;
    }

    public boolean hasNextTick(){
        if (datetimeIterator == null)
            datetimeIterator = dataCube.timeMap.keySet().iterator();
        return datetimeIterator.hasNext();
    }

    public boolean tick() {
        if (datetimeIterator == null)
            datetimeIterator = dataCube.timeMap.keySet().iterator();
        if (datetimeIterator.hasNext()){
            datetime = datetimeIterator.next();
        } else {
            return false;
        }
        /*
        for (Map.Entry<String, String> entry: dataCube.countryToContinent.entrySet()){

            Message message = new Message(Message.MSGTYPE.TICKTIME,null,new Address(entry.getKey(),entry.getValue()),"");
            Gson gson = new Gson();
            message.payload = String.valueOf(datetime);
            Message.sendMessage(message, superNodeAddress.get(entry.getValue()));

            message.msgtype = Message.MSGTYPE.TICKPRICES;
            message.payload = gson.toJson(getPriceData(datetime,entry.getKey()));
            Message.sendMessage(message, superNodeAddress.get(entry.getValue()));

            message.msgtype = Message.MSGTYPE.TICKISSUE;
            message.payload = gson.toJson(getIssueQuantity(datetime,entry.getKey()));
            Message.sendMessage(message, superNodeAddress.get(entry.getValue()));
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

        for (Map.Entry<String, Address> entry: superNodeAddress.entrySet()){
            Message message = new Message(Message.MSGTYPE.TICK,null,entry.getValue(),String.valueOf(datetime));
            Message.sendMessageToLocal(message);
        }

        return true;
    }

    public HashMap<String, Integer> getIssueQuantity(long datetime, String country) {
        return dataCube.getIssueQuantity(datetime, country);
    }

    public HashMap<String, Double> getPriceData(long datetime, String country) {
        return dataCube.getPriceData(datetime, country);
    }

    public String getTickerCountry(String ticker){
        return dataCube.tickerToCountry.get(ticker);
    }

    public String getCountryRegion(String country){
        return dataCube.countryToContinent.get(country);
    }

}
class EngineThread implements Runnable {
    private Thread t;
    private String threadName;
    private Engine eng;
    EngineThread( String name, Engine engine){
        threadName = name;
        this.eng = engine;
        TradeManager.log.fine("[ENGINE] Creating " +  threadName );
    }

    public void run() {

        TradeManager.log.fine("[ENGINE] Running " +  threadName );

        eng.init();
        eng.dataCube.generateCashBalances(10);
        eng.start();
    }

    public void start() {
        if (t == null)
        {
            t = new Thread (this, threadName);
            t.start ();
        }
    }
}

class SocketThread implements Runnable {
    private Thread t;
    private String threadName;
    private Engine eng;
    private Socket socket;

    SocketThread( String name, Socket socket, Engine eng){
        this.threadName = name;
        this.eng = eng;
        this.socket = socket;
        TradeManager.log.fine("[ENGINE] Creating " +  threadName );
    }

    public void run() {

        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String data = in.readLine();
            Message received = Message.readMessage(data);
            TradeManager.log.fine("[ENGINE] RECV " +  received.msgtype );
            Message response = eng.processMessage(received);
            TradeManager.log.fine("[ENGINE] SEND " +  response.msgtype );
            if (response != null)
                out.println(response.getMessage());
        } catch (IOException e) {
            System.out.println("ENGINE REPLY FAIL");
            e.printStackTrace();
        }
    }

    public void start() {
        if (t == null)
        {
            t = new Thread (this, threadName);
            t.start ();
        }
    }
}
