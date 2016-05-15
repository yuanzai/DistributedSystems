import junit.framework.TestCase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Formatter;
import java.util.logging.*;

/**
 * Created by junyuanlau on 15/5/16.
 */
public class TradeManager {
    Engine engine;
    LinkedList<Order> orderList;
    HashSet<String> clientList = new HashSet<String>();
    HashMap<String, String> clientCountry = new HashMap<String, String>();
    HashMap<String, Address> exchangeAddresses = new HashMap<String, Address>();

    LinkedHashMap<String, Order> orderLinkedHashMap = new LinkedHashMap<String, Order>();

    TableView newContentPane;

    //public final static Logger Log = Logger.getLogger("my.logger");
    public final static Logger log = Logger.getLogger("my.logger");


    public static void main(String[] args) {

        log.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new MyFormatter());
        handler.setLevel(Level.INFO);

        //log.removeHandler(log.getHandlers()[0]);
        log.setUseParentHandlers(false);
        log.addHandler(handler);

        TradeManager tm = new TradeManager("testStockQty", "testStockPrice", "testTrades");
        //TradeManager tm = new TradeManager("testStockQty", "testStockPrice", "testTrades");

        JFrame frame = new JFrame("low frequency trading");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        tm.newContentPane  = new TableView();
        tm.newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(tm.newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);



        tm.simulateTrades(1000);
/*
        JFrame frame = new JFrame("low frequency trading");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        tm.newContentPane  = new TableView();
        tm.newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(tm.newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);

        tm.simulateTrades(1000);


        JFrame control = new JFrame("low frequency trading");
        control.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setOpaque(true);

        JButton startTrading = new JButton();
        startTrading.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                tm.simulateTrades(1000);
            }
        });

        panel.add(startTrading);


        control.setContentPane(panel);
        control.pack();
        control.setVisible(true);
        */
    }

    public TradeManager(String issueFile, String priceFile, String tradeFile){
        this.engine = Engine.launchEngineThread();
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.engine.init();
        this.engine.dataCube.generateCompanyStaticData(issueFile);
        this.engine.dataCube.generateMarketStaticData(priceFile);

        generateOrders(tradeFile);

        this.engine.dataCube.generateCashBalances(clientList);

        generateRandomCountryForClient();
        this.engine.clientCountry = clientCountry;
        startExchanges();

        engine.tick();
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // book trade
    // play trade
    // dns exchange-ish
    public void generateOrders(String path) {
        orderList = new LinkedList<Order>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));

            String line;
            while ((line = in.readLine()) != null){
                String[] words = line.split(",");
                if (words[0].equals("Date"))
                    continue;
                Order.OrderType type;
                if (words[3].equals("B")){
                    type = Order.OrderType.BUY;
                } else if (words[3].equals("S")){
                    type = Order.OrderType.SELL;
                } else {
                    throw new Exception("Cannot find order type");
                }
                String country = engine.getTickerCountry(words[4]);
                String region = engine.getCountryRegion(country);
                Order order = new Order(words[2], words[4],country,region,type,Integer.parseInt(words[5]),DataCube.dateFormat(words[0], words[1]), UUID.randomUUID(),false);
                clientList.add(words[2]);
                orderList.add(order);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateRandomCountryForClient(){
        ArrayList<String> countryList = new ArrayList<String>(engine.dataCube.countryToContinent.keySet());
        int len = countryList.size();
        Random random = new Random();

        for (String client : clientList){
            int r = random.nextInt(len);
            clientCountry.put(client, countryList.get(r));
        }
    }

    public void startExchanges(){
        HashMap<String, Address> regionsSuperNode = new HashMap<String, Address>();
        HashMap<String, Address> allNodes = new HashMap<String, Address>();
        ArrayList<String> countryList = new ArrayList<String>(engine.dataCube.countryToContinent.keySet());
        int port = 5000;
        for (int i = 0; i < engine.dataCube.countryToContinent.size(); i++){
            String country = countryList.get(i);
            String region = engine.dataCube.countryToContinent.get(countryList.get(i));

            Address add = new Address("localhost", port++, country, region);
            if (regionsSuperNode.containsKey(region)){
                ExchangeNode node = new ExchangeNode(add, regionsSuperNode.get(region));

                node.start();
                System.out.println(node.supernode.name);
            } else {
                regionsSuperNode.put(region, add);
                ExchangeSuperNode node = new ExchangeSuperNode(add);
                node.start();
                System.out.println(node.name);
            }
            exchangeAddresses.put(country,add);

        }
    }

    public void simulateTrades(int delay){
        while (!orderList.isEmpty()){
            Order o = orderList.removeFirst();
            TradeManager.log.info("[SEND ORDER] CPTY" + o.counterparty + " " + o.orderType + " " + o.quantity + " " + o.ticker);
            orderLinkedHashMap.put(o.orderID.toString(), o);
            updateTable();
            sendOrder(o);


            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateTable(){


        int i = 0;

        for (Map.Entry<String, Order> entry : orderLinkedHashMap.entrySet()) {
            newContentPane.table.setValueAt(entry.getKey(),i,0);
            newContentPane.table.setValueAt(entry.getValue().counterparty,i,1);
            newContentPane.table.setValueAt(entry.getValue().ticker,i,2);
            newContentPane.table.setValueAt(entry.getValue().quantity,i,3);
            newContentPane.table.setValueAt(entry.getValue().orderType,i,4);
            i++;
            /*
            data[0][i] = entry.getKey();
            data[1][i] = entry.getValue().counterparty;
            data[2][i] = entry.getValue().ticker;
            data[3][i] = entry.getValue().quantity;
            data[3][i] = entry.getValue().orderType;
            */
        }
    }

    public void sendOrder(Order order){
        String clientCountry = this.clientCountry.get(order.counterparty);
        String clientRegion = engine.getCountryRegion(clientCountry);
        String tickerCountry = engine.getTickerCountry(order.ticker);
        String tickerRegion = engine.getCountryRegion(tickerCountry);
        Address sender = exchangeAddresses.get(tickerCountry);
        Message message = new Message(Message.MSGTYPE.ORDER,sender , new Address(tickerCountry, tickerRegion),order.getPayload());
        Message response = Message.readMessage(Message.sendMessage(message, sender));
        Order o = Order.readOrder(response.payload);
        TradeManager.log.info("[ACKD RECVD] CPTY" + o.counterparty + " " + o.orderType + " " + o.quantity + " " + o.ticker + " " + o.status);
        //System.out.println(response.getMessage());
    }

}
class MyFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return "" + record.getLevel() + ": " + record.getMessage() + "\r\n";

    }
}
