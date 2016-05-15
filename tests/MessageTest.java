import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import junit.framework.TestCase;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Created by junyuanlau on 14/5/16.
 */
public class MessageTest extends TestCase{
    DataCube dataCube;
    protected void setUp(){

    }

    @Test
    public void testStringMessage(){
        Message message = new Message();
        message.payload = "Hello";

        String json = message.getMessage();

        Message messageReconstruct = Message.readMessage(json.getBytes());
        assertEquals(messageReconstruct.payload, message.payload);

    }

    @Test
    public void testMapMessage(){

        Message message = new Message();
        HashMap<String, Integer> map1 =new HashMap<String, Integer>();
        map1.put("hello",1);

        Gson gson = new GsonBuilder().create();

        message.payload = gson.toJson(map1);
        String json = message.getMessage();

        Message messageReconstruct = Message.readMessage(json.getBytes());
        assertEquals(messageReconstruct.payload, message.payload);

        Type typeOfHashMap = new TypeToken<Map<String, Integer>>() { }.getType();
        Map<String, Integer> newMap = gson.fromJson(messageReconstruct.payload, typeOfHashMap); // This type must match TypeToken
        assertEquals(newMap.get("hello"), new Integer(1));

    }

    @Test
    public void testCashBalance() {
        Engine.launchEngineThread();
        Address rec = new Address("France", "Test");

        System.out.println(Message.sendMessageToLocal(new Message(Message.MSGTYPE.CASHBALANCE,rec,Engine.engineAddress,"1")));
    }

    @Test
    public void testOrderMessage() {
        Order buy = new Order("1", "ACCOR", "","", Order.OrderType.BUY,100, 123450, UUID.randomUUID(), false);
        String json = buy.getPayload();
        Order buy2 = Order.readOrder(json);
        assertEquals(buy.orderID,buy2.orderID);
        assertEquals(buy.datetime,buy2.datetime);
        assertEquals(buy.counterparty,buy2.counterparty);
        assertEquals(buy.orderType,buy2.orderType);
        assertEquals(buy.isInventorySale,buy2.isInventorySale);

    }
}
