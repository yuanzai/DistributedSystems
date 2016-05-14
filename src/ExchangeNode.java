import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
        return false;
    }

    public ArrayList<Order> executeBuyOrder(Order order){
        // local atomic operation
        if (prices.get(order.ticker) == null) {
            order.status = Order.OrderStatus.ERROR;
            return null;
        }
        double price = prices.get(order.ticker);
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
}
