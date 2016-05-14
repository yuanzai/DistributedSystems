import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by junyuanlau on 28/4/16.
 */
public class ExchangeNode {
    HashMap<String, Integer> inventory = new HashMap<String, Integer>();
    HashMap<String, LinkedList<Order>> liveOrders = new HashMap<String, LinkedList<Order>>();
    OrderList orderList = new OrderList();
    Inventory inventory = new Inventory();
    HashMap<String, Double> prices;
    Address supernode;
    String region;
    String name;
    Address local;
    ServerSocket serverSocket = null;

    public void start(String name, String region, Address localAddress, Address entryReferenceNode, boolean isSupernode){
        this.local = localAddress;
        this.name = name;
        this.region = region;

        try {
            serverSocket = new ServerSocket(localAddress.port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(true) {

            try (
                Socket clientSocket = serverSocket.accept();
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            ) {
                StringBuilder builder = new StringBuilder();
                String line;
                do
                {
                    line = in.readLine();
                    if (line.equals("")) break;

                    builder.append(line + "\r\n");
                }
                while (true);

                String request = builder.toString();
                System.out.println("[SERVER] " + request);

                Message received = new Message(request, this);
                Message response = received.response();
                out.write(response.payload);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void endOfDay(){

    }

    public void receiveSellOrders(Order order) {
        // Queues sell orders
        if (liveOrders.get(order.ticker) != null){
            LinkedList<Order> list = new LinkedList<Order>();
            list.add(order);
            liveOrders.put(order.ticker, list);
        } else {
            liveOrders.get(order.ticker).add(order);
        }
    }

    public ArrayList<Trade> executeBuyOrder(Order order){
        double price = prices.get(order.ticker);
        ArrayList<Trade> trades = new ArrayList<Trade>();
        int buyQuantity = order.quantity;

        orderList.executeBuyOrder(order, prices);

        return trades;
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
}
