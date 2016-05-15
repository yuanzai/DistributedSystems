import junit.framework.TestCase;
import org.junit.Test;

import java.util.UUID;


/**
 * Created by junyuanlau on 14/5/16.
 */
public class NetworkTest extends TestCase {
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
    public void testNodeRun(){
        Address testAdd = new Address("localhost",9001,"France", "Europe");
        ExchangeNode node = new ExchangeNode("France", "Europe", testAdd,null,false);
        assertTrue(!node.isRunning);
        node.start();
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        assertTrue(node.isRunning);

        String response= Message.sendMessageToLocal(new Message(Message.MSGTYPE.PING,testAdd,testAdd,""));
        Message reply = Message.readMessage(response);
        assertTrue(reply.msgtype == Message.MSGTYPE.PING);

        Message.sendTerminate(9001);

    }


    @Test
    public void testNetwordOrder(){
        Address testAdd = new Address("localhost",9000,"France", "Europe");
        ExchangeNode node = new ExchangeNode("France", "Europe", testAdd,null,false);
        node.start();
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }


        node.datetime = 1451656800000L;
        node.name = "France";
        node.inventory = new Inventory();
        node.prices = engine.dataCube.getPriceData(node.datetime,node.name);
        node.inventory.updateIssue(engine.dataCube.getIssueQuantity(node.datetime, node.name));

        assertEquals(100, node.inventory.checkBalance("ACCOR"));
        // TEST TRADE 1 - no part fill, GS SENDS ORDER for ACCOR
        Order buy = new Order("1", "ACCOR", "France","Europe", Order.OrderType.BUY,50,node.datetime, UUID.randomUUID(), false);
        Message message = new Message(Message.MSGTYPE.ORDER,null,testAdd,buy.getPayload());
        String response= Message.sendMessageToLocal(message);
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        Message reply = Message.readMessage(response);
        Order returned = Order.readOrder(reply.payload);
        assertEquals(returned.status, Order.OrderStatus.FILLED);

        buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,60,node.datetime, UUID.randomUUID(), false);
        message = new Message(Message.MSGTYPE.ORDER,null,testAdd,buy.getPayload());
        response= Message.sendMessageToLocal(message);
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        reply = Message.readMessage(response);
        returned = Order.readOrder(reply.payload);
        assertEquals(returned.status, Order.OrderStatus.PARTFILL);

        buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,60,node.datetime, UUID.randomUUID(), false);
        message = new Message(Message.MSGTYPE.ORDER,null,testAdd,buy.getPayload());
        response= Message.sendMessageToLocal(message);
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        reply = Message.readMessage(response);
        returned = Order.readOrder(reply.payload);
        assertEquals(returned.status, Order.OrderStatus.NOINVENTORY);
        Message.sendTerminate(9000);
    }

    @Test
    public void testSupernodeUpdateLocal(){
        Address superAdd = new Address("localhost", 4444,"France", "Europe");
        ExchangeSuperNode superNode = new ExchangeSuperNode(superAdd);
        superNode.start();

        Address localAdd = new Address("localhost", 4445,"UK", "Europe");
        ExchangeNode exchangeNode = new ExchangeNode(localAdd,superAdd);
        exchangeNode.start();
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        assertTrue(superNode.localNodeAddress.size() == 1);

        Address localAdd2 = new Address("localhost", 4446,"Germany", "Europe");
        Message message = new Message(Message.MSGTYPE.LOCAL,localAdd2,superAdd,"");
        Message.sendMessageToLocal(message);
        assertTrue(superNode.localNodeAddress.size() == 2);
        Message.sendTerminate(4444);
        Message.sendTerminate(4445);
        Message.sendTerminate(4446);
    }


    @Test
    public void testSupernodeUpdateSuper(){
        Address superAdd2 = new Address("localhost", 4445,"China", "Asia");
        Address superAdd3 = new Address("localhost", 4446,"South Africa", "Africa");
        Address superAdd4 = new Address("localhost", 9444,"USA", "America");

        //engine.superNodeAddress.put(superAdd1.region, superAdd1);
        //engine.superNodeAddress.put(superAdd2.region, superAdd2);
        //engine.superNodeAddress.put(superAdd3.region, superAdd3);

        Address superAdd = new Address("localhost", 4444,"France", "Europe");
        ExchangeSuperNode superNode = new ExchangeSuperNode("France", "Europe", superAdd);
        superNode.start();
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(superNode.superNodeAddress.size(),1);

        ExchangeSuperNode superNode2 = new ExchangeSuperNode("China", "Asia", superAdd2);
        superNode2.start();
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(superNode2.superNodeAddress.size(),2);
        assertEquals(superNode.superNodeAddress.size(),2);

        ExchangeSuperNode superNode3 = new ExchangeSuperNode("South Africa", "Africa", superAdd3);
        superNode3.start();
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(superNode2.superNodeAddress.size(),3);
        assertEquals(superNode.superNodeAddress.size(),3);
        assertEquals(superNode3.superNodeAddress.size(),3);
        Message.sendTerminate(4444);
        Message.sendTerminate(4445);
        Message.sendTerminate(4446);

    }


    @Test
    public void testRoute(){
        Address superAdd1 = new Address("localhost", 30000,"UK", "Europe");
        Address superAdd2 = new Address("localhost", 21000,"China", "Asia");
        Address add1 = new Address("localhost", 30001,"France", "Europe");
        Address add2 = new Address("localhost", 21001,"HK", "Asia");

        ExchangeSuperNode superNode1 = new ExchangeSuperNode(superAdd1);
        superNode1.start();
        ExchangeSuperNode superNode2 = new ExchangeSuperNode(superAdd2);
        superNode2.start();

        ExchangeNode node1 = new ExchangeNode(add1,superAdd1);
        node1.start();

        ExchangeNode node2 = new ExchangeNode(add2,superAdd2);
        node2.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        node1.datetime = 1451656800000L;
        node1.inventory = new Inventory();
        node1.prices = engine.dataCube.getPriceData(node1.datetime,node1.name);
        node1.inventory.updateIssue(engine.dataCube.getIssueQuantity(node1.datetime, node1.name));


        assertEquals(100, node1.inventory.checkBalance("ACCOR"));
        // TEST TRADE 1 - no part fill, GS SENDS ORDER for ACCOR
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100,node1.datetime, UUID.randomUUID(), false);

        Message message = new Message(Message.MSGTYPE.ORDER,add2, add1,buy.getPayload());
        Message.sendMessage(message, add2);
        assertEquals(0, node1.inventory.checkBalance("ACCOR"));

    }
    public void tearDown(){
        Message message = new Message(Message.MSGTYPE.TERMINATE,null,Engine.engineAddress,"");
        Message.sendMessageToLocal(message);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}