import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by junyuanlau on 28/4/16.
 */
public class ExchangeNode {
    OrderList orderList = new OrderList();
    Inventory inventory = new Inventory();
    Holdings holdings = new Holdings();
    HashMap<String, Double> prices;
    Address supernode;
    String region;
    String name;
    Address local;
    ServerSocket serverSocket = null;
    long datetime;

    boolean isRunning = false;
    boolean terminate = false;

    public ExchangeNode(Address localAddress, Address supernode) {
        this(localAddress.name,localAddress.region,localAddress,supernode,false);
    }

    public ExchangeNode(String name, String region, Address localAddress, Address supernode, boolean isSupernode) {
        this.local = localAddress;
        this.name = name;
        this.region = region;
        this.supernode = supernode;
        if (!isSupernode) {
            Message message = new Message(Message.MSGTYPE.LOCAL, localAddress, supernode, "");
            Message.sendMessageToLocal(message);
        }
    }
    public ExchangeNode(){}

    public void start(){
        NodeThread nodeThread = new NodeThread(name, this);
        nodeThread.start();
    }

    public void startServer(){
        serverSocket = null;

        try {
            serverSocket = new ServerSocket(local.port);
        } catch (IOException e) {
            System.out.println("[SOCKET IN USE] " + local.port+" " + name + " "+ region);
            e.printStackTrace();
        }
        isRunning = true;
        while (!terminate) {
            isRunning = true;
            Socket clientSocket = null;
            PrintWriter out = null;
            BufferedReader in = null;

            try {
                clientSocket = serverSocket.accept();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            } catch (IOException e) {
                isRunning = false;
                e.printStackTrace();
            }

            try {
                String data = in.readLine();
                Message received = Message.readMessage(data);
                System.out.println("[" + local.port + "] RECV " +received.msgtype);
                Message response = processMessage(received);
                if (response != null)
                    out.println(response.getMessage());
                else
                    out.println("");
            } catch (IOException e) {
                isRunning = false;
                e.printStackTrace();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("TERMINATED " + local.port);
    }

    public Message processMessage(Message message){
        if (message.msgtype == Message.MSGTYPE.TICK) {
            tick();
            return null;
        } else if (message.msgtype == Message.MSGTYPE.TERMINATE){
            terminate = true;
            System.out.println("STOPPING " + local.port);
            return null;
        }

        if (!message.receiver.name.equals(name)) {
            return Message.readMessage(Message.sendMessage(message, supernode));
        }

        if (message.msgtype == Message.MSGTYPE.ORDER){
            Order order = Order.readOrder(message.payload);
            if (order.orderType == Order.OrderType.BUY) {
                executeBuyOrder(order);
                message.payload = order.getPayload();
            } else {
                receiveSellOrders(order);
            }
        } else if (message.msgtype == Message.MSGTYPE.PING){

        } else if (message.msgtype == Message.MSGTYPE.NODE){

        } else {
        }
        return message;
    }

    public void tick(){
        Message message = new Message(Message.MSGTYPE.TICK,local,Engine.engineAddress,"DATETIME");
        Message response = Message.readMessage(Message.sendMessageToLocal(message));
        datetime = Long.parseLong( response.payload );

        message = new Message(Message.MSGTYPE.TICK,local,Engine.engineAddress,"PRICES");
        response = Message.readMessage(Message.sendMessageToLocal(message));
        Gson gson = new Gson();
        Type typeOfHashMap = new TypeToken<HashMap<String, Double>>() { }.getType();
        prices = gson.fromJson(response.payload, typeOfHashMap);



        message = new Message(Message.MSGTYPE.TICK,local,Engine.engineAddress,"ISSUE");
        response = Message.readMessage(Message.sendMessageToLocal(message));
        typeOfHashMap = new TypeToken<HashMap<String, Integer>>() { }.getType();
        HashMap<String, Integer> issues = gson.fromJson(response.payload, typeOfHashMap);
        inventory.updateIssue(issues);
        System.out.println("TICKDONE " + local.port);

    }

    public void endOfDay(){
        // get next date time
        // retrieve price data
        // retrieve issue data
        // update inventory with issue data
    }

    public boolean receiveSellOrders(Order order) {
        // Queues sell orders
        // local atomic operation

        // check holdings
        int current = holdings.checkHoldings(order.counterparty, order.ticker);
        int currentQueue = orderList.checkSellQueue(order.counterparty, order.ticker);
        if (current >= order.remainingQuantity + currentQueue) {
            orderList.queueSellOrder(order);
            return true;
        }
        order.status = Order.OrderStatus.REJECT;
        return false;
    }

    public ArrayList<Order> executeBuyOrder(Order order){
        // local atomic operation

        if (prices.get(order.ticker) == null) {
            order.status = Order.OrderStatus.ERROR;
            return null;
        }

        double price = prices.get(order.ticker);

        if (checkBalance(order.counterparty)<price*order.remainingQuantity){
            order.status = Order.OrderStatus.NOCASH;
            return null;
        }

        ArrayList<Order> ordersFilled = orderList.executeBuyOrder(order, price, datetime);
        for (Order sell : ordersFilled){
            updateBalance(sell.counterparty, sell.filledQuantity*price);
        }
        if (order.remainingQuantity > 0) {
            Order inventoryFill = inventory.fillBuyOrder(order, price, datetime);
            if (inventoryFill!= null)
                ordersFilled.add(inventoryFill);
        }

        if (order.remainingQuantity > 0)
            order.status = Order.OrderStatus.PARTFILL;
        else
            order.status = Order.OrderStatus.FILLED;
        if (order.quantity == order.remainingQuantity)
            order.status = Order.OrderStatus.NOINVENTORY;

        updateBalance(order.counterparty, -order.filledQuantity*price);
        return ordersFilled;
    }

    public void updateHoldings(Order order, ArrayList<Order> orders){
        holdings.updateHoldings(order);
        if (orders != null){
            for (Order o : orders){
                holdings.updateHoldings(o);
            }
        }
    }

    public double checkBalance(String counterparty){
        Message message = new Message(Message.MSGTYPE.CASHBALANCE,local,Engine.engineAddress,counterparty);
        Message response = Message.readMessage(Message.sendMessageToLocal(message));

        if (response.payload.equals("NOTFOUND"))
            return 0;
        return Double.parseDouble(response.payload);
    }

    public void updateBalance(String counterparty, double amount){
        Message message = new Message(Message.MSGTYPE.UPDATECASH,local,Engine.engineAddress,DataCube.updateCashBalancePayload(counterparty,amount));
        Message.sendMessageToLocal(message);
    }
}
class NodeThread implements Runnable {
    private Thread t;
    private String threadName;
    private ExchangeNode node;
    NodeThread(String name, ExchangeNode node){
        threadName = name;
        this.node = node;
    }

    public void run() {
        System.out.println("[" + node.local.port + "] RUNNING " +  node.local.name );
        node.startServer();
    }

    public void start() {
        if (t == null)
        {
            t = new Thread (this, threadName);
            t.start ();
        }
    }

}
