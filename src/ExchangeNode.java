import java.io.*;
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



    public ExchangeNode(String name, String region, Address localAddress, Address entryReferenceNode, boolean isSupernode) {
        this.local = localAddress;
        this.name = name;
        this.region = region;
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
            e.printStackTrace();
        }
        isRunning = true;
        while (true) {
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

            System.out.println("LISTENING ON " + local.port);
            try {
                String data = in.readLine();
                Message received = Message.readMessage(data);
                Message response = processMessage(received);

                out.println(response.getMessage());
            } catch (IOException e) {
                isRunning = false;
                e.printStackTrace();
            }
        }
    }

    public Message processMessage(Message message){
        if (message.msgtype == Message.MSGTYPE.ORDER){
            Order order = Order.readOrder(message.payload);
            if (order.orderType == Order.OrderType.BUY) {
                executeBuyOrder(order);
            } else {
                receiveSellOrders(order);
            }
        } else if (message.msgtype == Message.MSGTYPE.PING){

        } else if (message.msgtype == Message.MSGTYPE.NODE){

        }
        return message;
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
        if (order.remainingQuantity > 0) {
            Order inventoryFill = inventory.fillBuyOrder(order, price, datetime);
            if (inventoryFill!= null)
                ordersFilled.add(inventoryFill);
        }
        if (order.remainingQuantity > 0) {
            order.status = Order.OrderStatus.PARTFILL;
        }
        return ordersFilled;
    }

    public void updateHoldings(Order order, ArrayList<Order> orders){
        holdings.updateHoldings(order);
        //holdings.updateHoldings(order2);
        if (orders != null){
            for (Order o : orders){
                holdings.updateHoldings(o);
            }
        }
    }

    public void bookTradeOnLocalStore(Trade trade){

    }

    public void bookTradeOnRemoteStoreViaPaxos(Trade trade){

    }

    public void backupTradeForRemoteNode(Trade trade){

    }

    public ArrayList<Address> retrieveAllLocalAddressesFromSuperNode(Address a){
        return null;
    }

    public void sendMessage(Message message){

    }

    public double checkBalance(String counterparty){
        Message message = new Message(Message.MSGTYPE.CASHBALANCE,local,Engine.engineAddress,counterparty);
        Message response = Message.readMessage(Engine.sendMessage(message));

        if (response.payload.equals("NOTFOUNT"))
            return 0;
        return Double.parseDouble(response.payload);
    }
}
class NodeThread implements Runnable {
    private Thread t;
    private String threadName;
    private ExchangeNode node;
    NodeThread(String name, ExchangeNode node){
        threadName = name;
        this.node = node;
        System.out.println("Creating " +  threadName );
    }

    public void run() {
        System.out.println("Running " +  threadName );
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
