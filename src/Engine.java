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
        serverSocket = null;

        try {
            serverSocket = new ServerSocket(engineAddress.port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!terminate) {
            Socket clientSocket = null;
            PrintWriter out = null;
            BufferedReader in = null;

            try {
                clientSocket = serverSocket.accept();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                String data = in.readLine();
                Message received = Message.readMessage(data);
                Message response = processMessage(received);

                out.println(response.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("TERMINATED ENGINE");    }
    public void init() {
        dataCube = new DataCube();
    }

    public Message processMessage(Message message) {

        if (message.msgtype == Message.MSGTYPE.CASHBALANCE) {
            if (!dataCube.cashBalance.containsKey(message.payload))
                message.payload = "NOTFOUND";
            else
                message.payload = dataCube.cashBalance.get(message.payload).toString();

        } else if (message.msgtype == Message.MSGTYPE.SUPER) {
            superNodeAddress.put(message.sender.region, message.sender);
            Gson gson = new GsonBuilder().create();

            message.payload = gson.toJson(superNodeAddress);
            for (Map.Entry<String, Address> entry: superNodeAddress.entrySet()){
                if (!entry.getKey().equals(message.sender.region)) {
                    Message message1 = new Message(Message.MSGTYPE.SUPER,null,entry.getValue(),message.payload);
                    Message.sendMessageToLocal(message1);
                }
            }

        }else if (message.msgtype == Message.MSGTYPE.TERMINATE) {
            stopEngineThread();
        } else if (message.msgtype == Message.MSGTYPE.UPDATECASH) {
            dataCube.updateBalance(DataCube.getUpdateCashBalanceAmount(message.payload), DataCube.getUpdateCashBalanceCpty(message.payload));
        } else if (message.msgtype == Message.MSGTYPE.TICK) {
            if (message.payload.equals("DATETIME")){
                message.payload = String.valueOf(datetime);
            }else if (message.payload.equals("PRICES")){
                Gson gson = new Gson();
                message.payload = gson.toJson(getPriceData(datetime,message.sender.name));
            }else if (message.payload.equals("ISSUE")){
                Gson gson = new Gson();
                message.payload = gson.toJson(getIssueQuantity(datetime,message.sender.name));
            }
        }

        return message;
    }

    public void tick() {
        if (datetimeIterator == null)
            datetimeIterator = dataCube.timeMap.keySet().iterator();
        if (datetimeIterator.hasNext()){
            datetime = datetimeIterator.next();
        } else {
            System.out.println("END OF TIME");
        }
        for (Map.Entry<String, Address> entry: superNodeAddress.entrySet()){
            Message message = new Message(Message.MSGTYPE.TICK,null,entry.getValue(),"");
            Message.sendMessageToLocal(message);
        }
    }

    public HashMap<String, Integer> getIssueQuantity(long datetime, String country) {
        return dataCube.getIssueQuantity(datetime, country);
    }

    public HashMap<String, Double> getPriceData(long datetime, String country) {
        return dataCube.getPriceData(datetime, country);
    }


}
class EngineThread implements Runnable {
    private Thread t;
    private String threadName;
    private Engine eng;
    EngineThread( String name, Engine engine){
        threadName = name;
        this.eng = engine;
        System.out.println("Creating " +  threadName );
    }

    public void run() {
        System.out.println("Running " +  threadName );


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
