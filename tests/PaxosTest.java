import com.google.gson.Gson;
import junit.framework.TestCase;
import junit.framework.TestResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by junyuanlau on 25/5/16.
 */
public class PaxosTest extends TestCase {
    Engine engine;

    protected void setUp(){
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        engine = Engine.launchEngineThread();
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        engine.dataCube.generateCompanyStaticData("testStockQty");
        engine.dataCube.generateMarketStaticData("testStockPrice");
        engine.dataCube.generateCashBalances(20);

    }

    @Test
    public void testGSON() {
        Holdings holdings = new Holdings("Test");
        holdings.put("test",new HashMap<String, Integer>(){{put("test",1);}});
        String json = holdings.getPayload();

        Holdings.fromPayload(new Message(null,null,null,json));

    }

    @Test
    public void testPaxos(){
        Address superAdd = new Address("localhost", 4444,"UK", "Europe");
        ExchangeSuperNode superNode = new ExchangeSuperNode(superAdd);
        superNode.start();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        superNode.holdings.put("test",new HashMap<String, Integer>(){{put("test",1);}});
        superNode.inventory.put("test",1);


        Address localAdd = new Address("localhost", 4445,"France", "Europe");
        ExchangeNode exchangeNode = new ExchangeNode(localAdd,superAdd);
        exchangeNode.start();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exchangeNode.holdings.put("test",new HashMap<String, Integer>(){{put("test",1);}});
        exchangeNode.inventory.put("test",1);


        Address localAdd2 = new Address("localhost", 4446,"Germany", "Europe");
        ExchangeNode exchangeNode2 = new ExchangeNode(localAdd2,superAdd);
        exchangeNode2.start();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exchangeNode2.holdings.put("test",new HashMap<String, Integer>(){{put("test",1);}});
        exchangeNode2.inventory.put("test",1);

        Address localAdd3 = new Address("localhost", 4447,"Belgium", "Europe");
        ExchangeNode exchangeNode3 = new ExchangeNode(localAdd3,superAdd);
        exchangeNode3.start();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exchangeNode3.holdings.put("test",new HashMap<String, Integer>(){{put("test",1);}});
        exchangeNode3.inventory.put("test",1);

        superNode.broadcastHoldings(superNode.holdings, superNode.local);
        exchangeNode.broadcastHoldings(exchangeNode.holdings, exchangeNode.local);
        exchangeNode2.broadcastHoldings(exchangeNode2.holdings, exchangeNode2.local);
        exchangeNode3.broadcastHoldings(exchangeNode3.holdings, exchangeNode3.local);
        superNode.broadcastInventory(superNode.inventory, superNode.local);
        exchangeNode.broadcastInventory(exchangeNode.inventory, exchangeNode.local);
        exchangeNode2.broadcastInventory(exchangeNode2.inventory, exchangeNode2.local);
        exchangeNode3.broadcastInventory(exchangeNode3.inventory, exchangeNode3.local);

        engine.tick();
        //exchangeNode.prices = engine.dataCube.getPriceData(exchangeNode.datetime,exchangeNode.name);

        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(exchangeNode.replicatedHoldings.size(),3);
        assertEquals(exchangeNode2.replicatedHoldings.size(),3);

        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,exchangeNode.datetime, UUID.randomUUID(), false);

        // EXCHANGES EXECUTES WITH NO SELL ORDER, BUYS FROM INVENTORY
        ArrayList<Order> orders = exchangeNode.executeBuyOrder(buy);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Message.sendMessageToLocal(new Message(Message.MSGTYPE.TERMINATE,null,localAdd2,""));

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exchangeNode2.restartExchange();
    }

}
