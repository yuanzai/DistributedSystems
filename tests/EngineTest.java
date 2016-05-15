import com.sun.tools.javac.api.ClientCodeWrapper;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by junyuanlau on 15/5/16.
 */
public class EngineTest extends TestCase{
    Engine engine;
    Address superAdd1;
    Address superAdd2;
    Address add1;
    Address add2;
    ExchangeSuperNode superNode1;
    ExchangeSuperNode superNode2;
    ExchangeNode node1;
    ExchangeNode node2;

    protected void setUp(){
        engine = Engine.launchEngineThread();
        try {
            Thread.sleep(100);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        engine.init();
        engine.dataCube.generateCompanyStaticData("testStockQty");
        engine.dataCube.generateMarketStaticData("testStockPrice");
        engine.dataCube.generateCashBalances(20);

        superAdd1 = new Address("localhost", 30000,"UK", "Europe");
        superAdd2 = new Address("localhost", 21000,"China", "Asia");
        add1 = new Address("localhost", 30001,"France", "Europe");
        add2 = new Address("localhost", 21001,"HK", "Asia");

        superNode1 = new ExchangeSuperNode(superAdd1);
        superNode1.start();
        superNode2 = new ExchangeSuperNode(superAdd2);
        superNode2.start();

        node1 = new ExchangeNode(add1,superAdd1);
        node1.start();

        node2 = new ExchangeNode(add2,superAdd2);
        node2.start();
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

    }

    @Test
    public void testHoldings(){
        engine.tick();
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        assertEquals(node1.datetime,1451656800000L);
        assertEquals(node2.datetime,1451656800000L);
        assertEquals(superNode1.datetime,1451656800000L);
        assertEquals(superNode2.datetime,1451656800000L);
        assertEquals(node1.prices.get("ACCOR"), 1.11);
        assertNull(node2.prices.get("ACCOR"));
        assertEquals(node1.inventory.checkBalance("ACCOR"), 100);
        engine.tick();
        try {
            Thread.sleep(50);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        assertEquals(node1.datetime,1451660400000L);
        assertEquals(node2.datetime,1451660400000L);
        assertEquals(superNode1.datetime,1451660400000L);
        assertEquals(superNode2.datetime,1451660400000L);
        assertEquals(node1.prices.get("ACCOR"), 1.0);
        assertNull(node2.prices.get("ACCOR"));
        assertEquals(node1.inventory.checkBalance("ACCOR"), 200);


    }

    public void tearDown(){
        Message.sendTerminate(10000);
        Message.sendTerminate(superAdd1.port);
        Message.sendTerminate(superAdd2.port);
        Message.sendTerminate(add1.port);
        Message.sendTerminate(add2.port);
    }
}
