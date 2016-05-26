import javax.swing.*;
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
    HashMap<String, ExchangeNode> exchanges = new HashMap<String, ExchangeNode>();


    HashMap<UUID, Integer> orderTradeViewPosition = new HashMap<UUID, Integer>();

    TableView tradesView;
    NodesUI nodesUI;
    JLabel time;
    HoldingsUI holdingsUI;
    InventoryUI inventoryUI;
    ClientUI clientUI;
    LogUI logUI;

    boolean logMode = true;
    boolean isQ5 = false;

    public final static Logger log = Logger.getLogger("my.logger");

    private static TradeManager instance = null;
    protected TradeManager() {
        // Exists only to defeat instantiation.
    }
    public static TradeManager getInstance() {
        if(instance == null) {
            instance = new TradeManager();
        }
        return instance;
    }


    public static void main(String[] args) {

        log.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new MyFormatter());
        handler.setLevel(Level.INFO);

        //log.removeHandler(log.getHandlers()[0]);
        log.setUseParentHandlers(false);
        log.addHandler(handler);

        TradeManager tm = TradeManager.getInstance();
        //tm.start("qty_stocks.csv", "price_stocks.csv", "testTrades2");
        tm.start("liveDemoQty.csv", "liveDemoPrice.csv", "testTrades2");


        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(tm);
            }
        });
    }
    public void start(String issueFile, String priceFile, String tradeFile){
        this.engine = Engine.launchEngineThread();
        try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
        this.engine.init();
        this.engine.dataCube.generateCompanyStaticData(issueFile);
        this.engine.dataCube.generateMarketStaticData(priceFile);

        log.info("Generating Orders");
        generateOrders(tradeFile);

        this.engine.dataCube.generateCashBalances(10);

        generateRandomCountryForClient();
        this.engine.clientCountry = clientCountry;

        log.info("Starting Exchanges");
        startExchanges();

        engine.tick();
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        for (ExchangeNode node : exchanges.values()){
            node.broadcastHoldings(node.holdings, node.local);
            node.broadcastInventory(node.inventory, node.local);
        }

    }

    private static void createAndShowGUI(TradeManager tm) {

        TradesUI tradesUI = new TradesUI(tm);
        tm.nodesUI = new NodesUI(tm);
        tm.holdingsUI = new HoldingsUI(tm);
        tm.inventoryUI = new InventoryUI(tm);
        tm.clientUI = new ClientUI(tm);
        tm.logUI = new LogUI(tm);
        Handler logUIHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                 tm.logUI.update("\n" + "" + record.getLevel() + ": " + record.getMessage());

            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        };
        log.addHandler(logUIHandler);

        tm.inventoryUI.refresh();
        tm.holdingsUI.refresh();
        tm.nodesUI.refresh();
        tm.clientUI.refresh();

    }



    public void tick(){
        engine.tick();
    }

    /******************************************************************
     * Trade / Order related methods
     *
     *****************************************************************/
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

    synchronized public void executeManualOrder(String cpty, String ticker, int size, String bs){
        String exchange = engine.dataCube.tickerToCountry.get(ticker);
        String region = engine.dataCube.countryToContinent.get(exchange);
        String cptyLoc = clientCountry.get(cpty);
        Order.OrderType type;
        if (bs.toUpperCase().equals("B")){
            type = Order.OrderType.BUY;
        } else {
            type = Order.OrderType.SELL;
        }
        Order order = new Order(cpty, ticker, exchange, region,type, size,engine.datetime,UUID.randomUUID(),false);

        log.info("[MANUAL ORDER ROUTE] Querying for ticker exchange: " + exchange);
        log.info("[MANUAL ORDER ROUTE] Querying for ticker exchange region: " + region);
        log.info("[MANUAL ORDER ROUTE] Querying for cpty home exchange: " + cptyLoc);
        Order send = sendOrder(order);
        tradeUpdateUI(order);
    }

    synchronized public void tradeUpdateUI(Order order){
        if (logMode) {
            tradesView.model.setValueAt(order.status, TradeManager.getInstance().orderTradeViewPosition.get(order.orderID), 5);
            inventoryUI.refresh();
            holdingsUI.refresh();
            clientUI.refresh();
        }
    }

    public void simulateTrades(int delay, int count){
        while (!orderList.isEmpty() && (count > 0 || count == -1)){
            Order order = orderList.peekFirst();
            if (logMode)
                time.setText(DataCube.dateFormat(engine.datetime));

            while (order.datetime>engine.datetime) {
                try { Thread.sleep(delay); } catch (InterruptedException e) { e.printStackTrace(); }
                tick();
                count--;
                if (logMode)
                    time.setText(DataCube.dateFormat(engine.datetime));
            }

            TradeManager.log.info("[SEND ORDER] CPTY" + order.counterparty + " " + order.orderType + " " + order.quantity + " " + order.ticker);


            Order returnOrder = sendOrder(order);
            try { Thread.sleep(delay); } catch (InterruptedException e) { e.printStackTrace(); }
            if (logMode)
                tradeUpdateUI(order);

            orderList.removeFirst();
        }

        while (engine.hasNextTick() && (count > 0 || count == -1)){
            try { Thread.sleep(delay); } catch (InterruptedException e) { e.printStackTrace(); }
            tick();
            count--;
            if (logMode)
                time.setText(DataCube.dateFormat(engine.datetime));
        }
    }

    public Order sendOrder(Order order){
        String clientCountry = this.clientCountry.get(order.counterparty);
        order.origin = clientCountry;
        String clientRegion = engine.getCountryRegion(clientCountry);
        String tickerCountry = engine.getTickerCountry(order.ticker);
        String tickerRegion = engine.getCountryRegion(tickerCountry);
        Address sender = exchangeAddresses.get(tickerCountry);
        Message message = new Message(Message.MSGTYPE.ORDER,sender , new Address(tickerCountry, tickerRegion),order.getPayload());
        Message response = Message.readMessage(Message.sendMessage(message, sender));
        if (logMode) {
            tradesView.model.addRow(new Object[]{order.orderID, order.counterparty, order.ticker, order.quantity, order.orderType, order.status});
            orderTradeViewPosition.put(order.orderID, tradesView.model.getRowCount() - 1);
        }

        Order o = Order.readOrder(response.payload);
        if (response.msgtype == Message.MSGTYPE.ERROR){
            o.status = Order.OrderStatus.EXCHANGEDOWN;
            TradeManager.log.info("[EXCH ERROR] CPTY" + o.counterparty + " " + o.orderType + " " + o.quantity + " " + o.ticker + " " + o.status);
        } else {
            TradeManager.log.info("[ACKD RECVD] CPTY" + o.counterparty + " " + o.orderType + " " + o.quantity + " " + o.ticker + " " + o.status);
        }
        return o;
        //System.out.println(response.getMessage());
    }

    /******************************************************************
     * Node related methods
     *
     *****************************************************************/

    public void terminateNode(String name){
        Message message = new Message(Message.MSGTYPE.TERMINATE,null,exchanges.get(name).local,"");
        Message.sendMessageToLocal(message);
        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
        nodesUI.refresh();
    }

    public void restartNode(String name){
        exchanges.get(name).terminate = false;
        exchanges.get(name).restartExchange();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        nodesUI.refresh();
    }

    public void startNode(Address address, Address supernode){
        if (supernode == null){
            ExchangeSuperNode node = new ExchangeSuperNode(address);
            node.start();
            exchanges.put(address.name, node);

        } else {
            ExchangeNode node = new ExchangeNode(address, supernode);
            node.start();
            exchanges.put(address.name, node);
        }
        exchangeAddresses.put(address.name,address);
        try { Thread.sleep(10); } catch (InterruptedException e) { e.printStackTrace();}
    }

    public void startExchanges(){
        HashMap<String, Address> regionsSuperNode = new HashMap<String, Address>();
        ArrayList<String> countryList = new ArrayList<String>(engine.dataCube.countryToContinent.keySet());
        int port = 50000;
        for (int i = 0; i < engine.dataCube.countryToContinent.size(); i++){
            String country = countryList.get(i);
            String region = engine.dataCube.countryToContinent.get(countryList.get(i));

            Address add = new Address("localhost", port++, country, region);
            if (regionsSuperNode.containsKey(region)){
                startNode(add, regionsSuperNode.get(region));

            } else {
                startNode(add, null);
                regionsSuperNode.put(region, add);
            }
        }
    }



    /******************************************************************
     * Client related methods
     *
     *****************************************************************/
    public void generateClient() {

    }

    public void q6(){
        engine.dataCube.generateCashBalances(1000);
        generateRandomCountryForClient();
        clientUI.refresh();
    }

    public void generateRandomCountryForClient(){

        ArrayList<String> countryList = new ArrayList<String>(engine.dataCube.countryToContinent.keySet());
        clientList = new HashSet<String>(engine.dataCube.cashBalance.keySet());
        int len = countryList.size();
        Random random = new Random();

        for (String client : clientList){
            int r = random.nextInt(len);
            clientCountry.put(client, countryList.get(r));
        }
    }
    /******************************************************************
     * Project related methods
     *
     *****************************************************************/

    public void question5(){
        TradeManager.log.info("START RANDOM BUY SELL");
        ConcurrentTrade concurrentTrade = new ConcurrentTrade(this);
    }

}

class MyFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return "" + record.getLevel() + ": " + record.getMessage() + "\r\n";

    }
}

class ConcurrentTrade implements Runnable {
    private Thread t;
    private TradeManager tm;
    ConcurrentTrade(TradeManager tm){
        this.tm = tm;

        this.start();
    }

    public void run() {

        ArrayList<String> list = new ArrayList<String>(tm.engine.dataCube.tickerToCountry.keySet());
        Random random = new Random();
        ArrayList<String> tickers = new ArrayList<String>();
        for (int i =0; i<3; i++){
            int r = random.nextInt(list.size());
            tickers.add(list.get(r));
        }
        ArrayList<String>clients = new ArrayList<>(tm.clientList);
        tm.isQ5 = true;
        while (tm.isQ5) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int x = 0; x < 10; x++) {
                String cpty = clients.get(random.nextInt(clients.size()));
                int qty = random.nextInt(4) + 1;
                String ticker = tickers.get(random.nextInt(3));
                String country = tm.engine.dataCube.tickerToCountry.get(ticker);
                String region = tm.engine.dataCube.countryToContinent.get(country);
                Order.OrderType type;
                if (random.nextBoolean()) {
                    type = Order.OrderType.BUY;
                } else {
                    type = Order.OrderType.SELL;
                }

                Order order = new Order(cpty, ticker, country, region, type, qty, tm.engine.datetime, UUID.randomUUID(), false);

                Order response = tm.sendOrder(order);
                tm.tradeUpdateUI(response);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


    }
    public void start() {
        if (t == null){
            t = new Thread (this);
            t.start ();
        }
    }
}



class SimulateTrades implements Runnable {
    private Thread t;
    private String threadName;
    private TradeManager tm;
    private int delay;
    private int count;
    SimulateTrades(String name, TradeManager tm, int delay, int count){
        threadName = name;
        this.tm = tm;
        this.delay = delay;
        this.count = count;
    }

    public void run() {
        tm.simulateTrades(delay, count);
    }

    public void start() {
        if (t == null){
            t = new Thread (this, threadName);
            t.start ();
        }
    }
}

