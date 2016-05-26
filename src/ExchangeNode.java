import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by junyuanlau on 28/4/16.
 */
public class ExchangeNode {
    OrderList orderList = new OrderList();
    Inventory inventory;
    Holdings holdings;
    Paxos paxos;
    HashMap<String, Inventory> replicatedInventory = new HashMap<String, Inventory>();
    HashMap<String, Holdings> replicatedHoldings = new HashMap<String, Holdings>();

    //HashSet<String> backUpNodes = new HashSet<String>();

    HashMap<String, Double> prices;
    HashMap<String, String> clientCountry;
    HashMap<String, String> countryToContinent;

    Address supernode;
    //Address backupSupernode;
    String region;
    String name;
    Address local;
    ServerSocket serverSocket = null;
    NodeThread nodeThread;
    Gson gson = new Gson();

    //SuperNode superNode;

    long datetime;

    boolean isRunning = false;
    boolean terminate = false;

    public ExchangeNode(Address localAddress) {
        this(localAddress.name,localAddress.region,localAddress,null,true);
    }
    public ExchangeNode(Address localAddress, Address supernode) {
        this(localAddress.name,localAddress.region,localAddress,supernode,false);
    }

    public ExchangeNode(String name, String region, Address localAddress, Address supernode, boolean isSupernode) {
        this.local = localAddress;
        this.name = name;
        this.region = region;
        this.supernode = supernode;
        if (!isSupernode) {
            Message message = new Message(Message.MSGTYPE.NODE_NEW_LOCAL, localAddress, supernode, "");
            Message response = Message.readMessage(Message.sendMessageToLocal(message));
            //backupSupernode = response.sender;
            //if (backupSupernode.equals(local))
                //becomeBackup();
        } else {
            //superNode = new SuperNode(this);
        }
        this.inventory = new Inventory(name);
        this.holdings = new Holdings(name);
        this.paxos = new Paxos(this);
    }

    public ExchangeNode(){}

    public void restartExchange() {
        start();
        Message message = new Message(Message.MSGTYPE.NODE_RESTART, local, supernode, "");
        Message.sendMessageToLocal(message);
    }

    public void start(){
        nodeThread = new NodeThread(name, this);
        nodeThread.start();
    }

    public void startServer(){

        TradeManager.log.info("["+local.port+"] "+name + " - " + region + " - SUPERNODE: " + supernode);
        serverSocket = null;

        try {
            serverSocket = new ServerSocket(local.port);
        } catch (IOException e) {
            TradeManager.log.info("[" + local.port + "] SOCKET IN USE");
            e.printStackTrace();
        }
        isRunning = true;

            Socket clientSocket = null;

        while (!terminate) {

            try {
                clientSocket = serverSocket.accept();

            } catch (IOException e) {
                TradeManager.log.info("[" + local.port + "] NODE ACCEPT FAIL");
                isRunning = false;
                e.printStackTrace();
            }

            NodeSocketThread socketThread = new NodeSocketThread(name + " Socket Thread", clientSocket, this);
            socketThread.start();
            if (terminate) break;
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRunning = false;
        TradeManager.log.info("[" + local.port + "] TERMINATED");
    }

    public Message processMessage(Message message){
        if (!isRunning)
            return null;
        if (message.msgtype == Message.MSGTYPE.TICK) {
            datetime = Long.parseLong(message.payload);
            tick();
            return null;
        } else if (message.msgtype == Message.MSGTYPE.TERMINATE){
            terminate = true;
            TradeManager.log.info("[" + local.port + "] STOP INITIATED");
            Message.ping(local.port);
            return null;
        } else if (message.msgtype == Message.MSGTYPE.NODE_SETBACKUPSUPER){
            /*
            backupSupernode = message.sender;
            if (backupSupernode.equals(local)){
                becomeBackup();
            }
            */
        }

        if (!message.receiver.name.equals(name)) {
            return Message.readMessage(Message.sendMessage(message, supernode));
        }


        if (message.msgtype == Message.MSGTYPE.ORDER) {
            Order order = Order.readOrder(message.payload);
            if (order.status != Order.OrderStatus.CREATED) {
                receiveSellFilled(order);
                return null;
            } else {
                if (order.orderType == Order.OrderType.BUY) {
                    if (orderList.checkValid(order)) {
                        executeBuyOrder(order);
                    } else {
                        order.status = Order.OrderStatus.CONFLICT;
                    }
                    message.payload = order.getPayload();
                } else {
                    receiveSellOrders(order);
                    message.payload = order.getPayload();
                }
            }


        } else if (message.msgtype == Message.MSGTYPE.PING) {
        } else if (message.msgtype == Message.MSGTYPE.PAXOS) {
            return paxos.processMessage(message);

        } else if (message.msgtype == Message.MSGTYPE.TICKISSUE) {
            tickIssue(message);
        } else if (message.msgtype == Message.MSGTYPE.TICKPRICES) {
            tickPrices(message);
        } else if (message.msgtype == Message.MSGTYPE.TICKTIME) {
            tickTime(message);
        } else if (message.msgtype == Message.MSGTYPE.GET_DATA_INVENTORY) {
            message.payload = replicatedInventory.get(message.payload).getPayload();
        } else if (message.msgtype == Message.MSGTYPE.GET_DATA_HOLDINGS) {
            message.payload = replicatedHoldings.get(message.payload).getPayload();
        } else if (message.msgtype == Message.MSGTYPE.DATA_INVENTORY) {
            Inventory inventory = Inventory.fromPayload(message);
            if (inventory.name.equals(name)) {
                this.inventory = inventory;
            } else {
                replicatedInventory.put(inventory.name, inventory);
            }
        } else if (message.msgtype == Message.MSGTYPE.DATA_HOLDINGS) {
            Holdings holdings = Holdings.fromPayload(message);
            if (holdings.name.equals(name)) {
                this.holdings = holdings;
            } else {
                replicatedHoldings.put(holdings.name, holdings);
            }
        } else {

        }

        return message;
    }
/*
    public void becomeBackup(){
        TradeManager.log.info("[" + local.port + "] SET AS BACKUP SUPERNODE");
        superNode = new SuperNode(this);
        superNode.isPrimary = false;

    }
    */
    public void tick(){
        /*
        SqliteDB db = new SqliteDB();
        db.open();
        prices = db.getPriceTable(name,datetime);
        inventory.updateIssue(db.getIssueTable(name,datetime));
        db.close();


        */
        //Old socket based code
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

        if (clientCountry == null) {
            message = new Message(Message.MSGTYPE.TICK, local, Engine.engineAddress, "CLIENT");
            response = Message.readMessage(Message.sendMessageToLocal(message));
            typeOfHashMap = new TypeToken<HashMap<String, String>>() {
            }.getType();
            clientCountry = gson.fromJson(response.payload, typeOfHashMap);
        }
        if (countryToContinent == null) {
            message = new Message(Message.MSGTYPE.TICK, local, Engine.engineAddress, "REGION");
            response = Message.readMessage(Message.sendMessageToLocal(message));
            typeOfHashMap = new TypeToken<HashMap<String, String>>() {
            }.getType();
            countryToContinent = gson.fromJson(response.payload, typeOfHashMap);
            TradeManager.log.fine("[" + local.port + "] TICK DONE");
        }

    }

    private void tickTime(Message message){
        datetime = Long.parseLong( message.payload );
    }

    private void tickIssue(Message message){
        Gson gson = new Gson();
        Type typeOfHashMap = new TypeToken<HashMap<String, Integer>>() { }.getType();
        HashMap<String, Integer> issues = gson.fromJson(message.payload, typeOfHashMap);
        inventory.updateIssue(issues);
        broadcastHoldings(this.holdings,local);
    }

    private void tickPrices(Message message){
        Gson gson = new Gson();
        Type typeOfHashMap = new TypeToken<HashMap<String, Double>>() { }.getType();
        prices = gson.fromJson(message.payload, typeOfHashMap);
    }

    public void broadcastHoldings(Holdings holdings, Address exAddress){
        Message send = new Message(Message.MSGTYPE.DATA_HOLDINGS, local, supernode, holdings.getPayload());
        Message.sendMessageToLocal(send);
    }

    public void broadcastInventory(Inventory inventory, Address exAddress){
        Message send = new Message(Message.MSGTYPE.DATA_INVENTORY, local, supernode, inventory.getPayload());
        Message.sendMessageToLocal(send);

    }

    synchronized public boolean receiveSellOrders(Order order) {
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

    synchronized public ArrayList<Order> executeBuyOrder(Order order){
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

        updateHoldings(order,ordersFilled);

        responseSellFills(ordersFilled);

        if (ordersFilled.size() > 0){
            broadcastInventory(inventory, local);
        }
        return ordersFilled;
    }

    private void updateHoldings(Order order, ArrayList<Order> orders){
        if (order!= null)
            holdings.updateHoldings(order);
        if (orders != null){
            for (Order o : orders){
                holdings.updateHoldings(o);
            }
        }
    }

    public void responseSellFills(ArrayList<Order> orders){
        if (orders == null)
            return;
        if (orders.size() == 0)
            return;
        for (Order order : orders){
            if (order.isInventorySale)
                continue;
            responseSellFilled(order);
        }
    }

    public void responseSellFilled(Order order){
        String country = order.origin;
        String region = countryToContinent.get(country);

        if (country.equals(local.name)) {
            receiveSellFilled(order);
        } else {
            Message message = new Message(Message.MSGTYPE.ORDER, local, new Address(country, region), order.getPayload());
            Message.sendMessage(message, supernode);
        }
    }
    public void receiveSellFilled(Order order){
        TradeManager.log.info("[ACKD RECVD] CPTY" + order.counterparty + " " + order.orderType + " " + order.quantity + " " + order.ticker + " " + order.status);
        TradeManager tm = TradeManager.getInstance();
        tm.tradeUpdateUI(order);
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
        if (node.supernode == null)
            TradeManager.log.fine("[" + node.local.port + "] RUNNING SUPERNODE " +  node.local.name );
        else
            TradeManager.log.fine("[" + node.local.port + "] RUNNING " +  node.local.name );
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

class NodeSocketThread implements Runnable {
    private Thread t;
    private String threadName;
    private ExchangeNode node;
    private Socket socket;

    NodeSocketThread( String name, Socket socket, ExchangeNode node){
        this.threadName = name;
        this.node = node;
        this.socket = socket;
        TradeManager.log.fine("[ENGINE] Creating " +  threadName );
    }

    public void run() {

        Message received = null;
        Message response = null;
        String data = null;
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            data = in.readLine();

            if (data != null) {
                received = Message.readMessage(data);
                TradeManager.log.fine("[" + node.local.port + "] RECV " + received.msgtype);
                response = node.processMessage(received);
                /*
                if (node.superNode != null){
                    response = node.superNode.processMessage(received);
                } else {
                    response = node.processMessage(received);
                }
                */

            }

            if (response != null) {
                out.println(response.getMessage());
                TradeManager.log.fine("[" + node.local.port + "] SEND " +  response.msgtype );
            }
            else
                out.println("");
        } catch (IOException e) {
            TradeManager.log.info("[" + node.local.port + "] NODE REPLY FAIL");
            node.isRunning = false;
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
