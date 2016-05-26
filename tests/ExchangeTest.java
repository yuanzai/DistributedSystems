import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;


/**
 * Created by junyuanlau on 14/5/16.
 */
public class ExchangeTest extends TestCase{
    ExchangeNode exchange;
    Engine engine;

    protected void setUp(){
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        engine = Engine.launchEngineThread();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        engine.dataCube.generateCompanyStaticData("testStockQty");
        engine.dataCube.generateMarketStaticData("testStockPrice");
        engine.dataCube.generateCashBalances(20);
        exchange = new ExchangeNode();
        exchange.datetime = 1451656800000L;
        exchange.name = "France";
        exchange.inventory = new Inventory(exchange.name);
        exchange.holdings = new Holdings(exchange.name);
        exchange.prices = engine.dataCube.getPriceData(exchange.datetime,exchange.name);
        exchange.inventory.updateIssue(engine.dataCube.getIssueQuantity(exchange.datetime, exchange.name));

    }

    @Test
    public void testBuyFromInventory() {
        // CHECK HOLDINGS
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));
        // TEST TRADE 1 - no part fill, GS SENDS ORDER for ACCOR
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,exchange.datetime, UUID.randomUUID(), false);
        assertEquals(0, exchange.holdings.checkHoldings("1","ACCOR"));
        assertEquals(0, buy.filledQuantity);
        assertEquals(100, buy.remainingQuantity);
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));


        // EXCHANGES EXECUTES WITH NO SELL ORDER, BUYS FROM INVENTORY
        ArrayList<Order> orders = exchange.executeBuyOrder(buy);
        assertEquals(100, buy.filledQuantity);
        assertEquals(0, buy.remainingQuantity);
        assertEquals(0, exchange.inventory.checkBalance("ACCOR"));
        assertEquals(100, exchange.holdings.checkHoldings("1","ACCOR"));

    }

    @Test
    public void testBuyFromCpty(){
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,exchange.datetime, UUID.randomUUID(), false);
        ArrayList<Order> orders = exchange.executeBuyOrder(buy);


        // TEST TRADE 2 - no partfill, GS SELLS, MS BUYS
        assertEquals(0, exchange.holdings.checkHoldings("2","ACCOR"));
        Order sell = new Order("1", "ACCOR", "","", Order.OrderType.SELL,100,exchange.datetime, UUID.randomUUID(), false);
        assertTrue(exchange.receiveSellOrders(sell));
        assertEquals(exchange.orderList.checkSellQueue(sell.counterparty,sell.ticker),100);

        Order buy2 = new Order("2", "ACCOR", "","", Order.OrderType.BUY,100,exchange.datetime, UUID.randomUUID(), false);
        orders = exchange.executeBuyOrder(buy2);
        assertEquals(100, buy2.filledQuantity);
        assertEquals(0, buy2.remainingQuantity);
        assertEquals(100, sell.filledQuantity);
        assertEquals(0, sell.remainingQuantity);

        assertEquals(0, exchange.inventory.checkBalance("ACCOR"));
        assertEquals(0, exchange.holdings.checkHoldings("1","ACCOR"));
        assertEquals(100, exchange.holdings.checkHoldings("2","ACCOR"));


    }
    @Test
    public void testOverSell() {
        // TEST TRADE 3 - no partfill, GS OVER SELLS, no holdings
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));
        Order sell = new Order("1", "ACCOR", "", "", Order.OrderType.SELL, 100, exchange.datetime, UUID.randomUUID(), false);
        assertTrue(!exchange.receiveSellOrders(sell));
        assertEquals(0, exchange.holdings.checkHoldings("1", "ACCOR"));
        assertEquals(sell.status, Order.OrderStatus.REJECT);
    }

    @Test
    public void testWrongTicker() {
        // TEST TRADE 4 - no inventory or sell orders
        assertEquals(0, exchange.inventory.checkBalance("Random"));
        Order buy = new Order("1", "Random", "", "", Order.OrderType.BUY, 100, exchange.datetime, UUID.randomUUID(), false);
        ArrayList<Order> orders = exchange.executeBuyOrder(buy);
        assertEquals(buy.status, Order.OrderStatus.ERROR);
        assertEquals(0, exchange.holdings.checkHoldings("1", "Random"));
    }

    @Test
    public void testBuyingFromNothing() {
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,exchange.datetime, UUID.randomUUID(), false);
        exchange.executeBuyOrder(buy);

        Order buy2 = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,exchange.datetime, UUID.randomUUID(), false);
        ArrayList<Order> orders  = exchange.executeBuyOrder(buy2);
        assertEquals(0, orders.size());

        assertEquals(0, exchange.inventory.checkBalance("ACCOR"));
        assertEquals(100, exchange.holdings.checkHoldings("1","ACCOR"));

    }

    @Test
    public void testPartfillBuyFromInventory() {
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,200,exchange.datetime, UUID.randomUUID(), false);
        ArrayList<Order> orders = exchange.executeBuyOrder(buy);
        assertEquals(buy.status, Order.OrderStatus.PARTFILL);
        assertEquals(buy.remainingQuantity, 100);
        assertEquals(buy.filledQuantity, 100);
        assertEquals(0, exchange.inventory.checkBalance("ACCOR"));
    }

    @Test
    public void testPartfillBuyFromCPTY() {
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,exchange.datetime, UUID.randomUUID(), false);
        ArrayList<Order> orders = exchange.executeBuyOrder(buy);


        assertEquals(0, exchange.holdings.checkHoldings("2","ACCOR"));
        Order sell = new Order("1", "ACCOR", "","", Order.OrderType.SELL,100,exchange.datetime, UUID.randomUUID(), false);
        assertTrue(exchange.receiveSellOrders(sell));
        assertEquals(exchange.orderList.checkSellQueue(sell.counterparty,sell.ticker),100);

        Order buy2 = new Order("2", "ACCOR", "","", Order.OrderType.BUY,200,exchange.datetime, UUID.randomUUID(), false);
        orders = exchange.executeBuyOrder(buy2);
        assertEquals(100, buy2.filledQuantity);
        assertEquals(100, buy2.remainingQuantity);
        assertEquals(100, sell.filledQuantity);
        assertEquals(0, sell.remainingQuantity);

        assertEquals(0, exchange.inventory.checkBalance("ACCOR"));
        assertEquals(0, exchange.holdings.checkHoldings("1","ACCOR"));
        assertEquals(100, exchange.holdings.checkHoldings("2","ACCOR"));
        assertEquals(buy2.status, Order.OrderStatus.PARTFILL);


    }

    @Test
    public void testPartfillSellFromCPTY() {
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,exchange.datetime, UUID.randomUUID(), false);
        ArrayList<Order> orders = exchange.executeBuyOrder(buy);

        assertEquals(0, exchange.holdings.checkHoldings("2","ACCOR"));
        Order sell = new Order("1", "ACCOR", "","", Order.OrderType.SELL,100,exchange.datetime, UUID.randomUUID(), false);
        assertTrue(exchange.receiveSellOrders(sell));
        assertEquals(exchange.orderList.checkSellQueue(sell.counterparty,sell.ticker),100);

        Order buy2 = new Order("2", "ACCOR", "","", Order.OrderType.BUY,50,exchange.datetime, UUID.randomUUID(), false);
        orders = exchange.executeBuyOrder(buy2);
        assertEquals(50, buy2.filledQuantity);
        assertEquals(0, buy2.remainingQuantity);
        assertEquals(50, sell.filledQuantity);
        assertEquals(50, sell.remainingQuantity);

        assertEquals(0, exchange.inventory.checkBalance("ACCOR"));
        assertEquals(50, exchange.holdings.checkHoldings("1","ACCOR"));
        assertEquals(50, exchange.holdings.checkHoldings("2","ACCOR"));


        assertEquals(orders.get(0).status, Order.OrderStatus.PARTFILL);
        assertEquals(sell.status, Order.OrderStatus.PENDING);
    }

    @Test
    public void testNoCash() {

        // NO CASH
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));

        exchange.prices.put("ACCOR",101.0);

        // TEST TRADE 1 - no part fill, GS SENDS ORDER for ACCOR
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,exchange.datetime, UUID.randomUUID(), false);
        assertEquals(buy.status, Order.OrderStatus.CREATED);

        ArrayList<Order> orders = exchange.executeBuyOrder(buy);

        assertEquals(buy.status, Order.OrderStatus.NOCASH);
        assertEquals(0, exchange.holdings.checkHoldings("1","ACCOR"));

    }


    @Test
    public void testCashBalance() {


        // NO CASH
        assertEquals(100, exchange.inventory.checkBalance("ACCOR"));

        exchange.prices.put("ACCOR",99.0);

        // TEST TRADE 1 - no part fill, GS SENDS ORDER for ACCOR
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,exchange.datetime, UUID.randomUUID(), false);

        ArrayList<Order> orders = exchange.executeBuyOrder(buy);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(100.0, engine.dataCube.cashBalance.get("1"));
        assertEquals(100, exchange.holdings.checkHoldings("1","ACCOR"));

    }

    public void tearDown(){

        Message message = new Message(Message.MSGTYPE.TERMINATE,null,Engine.engineAddress,"");
        Message.sendMessageToLocal(message);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
