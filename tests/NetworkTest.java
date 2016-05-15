import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;


/**
 * Created by junyuanlau on 14/5/16.
 */
public class NetworkTest extends TestCase {

    @Test
    public void testNodeRun(){
        Address testAdd = new Address("localhost",9000);
        ExchangeNode node = new ExchangeNode("France", "Europe", testAdd,null,false);
        //node.startServer();
        assertTrue(!node.isRunning);
        node.start();
        try {
            Thread.sleep(2000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        assertTrue(node.isRunning);

        String response= Engine.sendMessage(new Message(Message.MSGTYPE.PING,testAdd,testAdd,""));
        Message reply = Message.readMessage(response);
        assertTrue(reply.msgtype == Message.MSGTYPE.PING);


    }

    @Test
    public void testNetwordOrder(){
        Address testAdd = new Address("localhost",9000);
        ExchangeNode node = new ExchangeNode("France", "Europe", testAdd,null,false);
        //node.startServer();
        assertTrue(!node.isRunning);
        node.start();
        try {
            Thread.sleep(2000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        assertTrue(node.isRunning);

        String response= Engine.sendMessage(new Message(Message.MSGTYPE.PING,testAdd,testAdd,""));
        Message reply = Message.readMessage(response);
        assertTrue(reply.msgtype == Message.MSGTYPE.PING);


    }

}