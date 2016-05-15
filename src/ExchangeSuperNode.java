import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by junyuanlau on 28/4/16.
 */
public class ExchangeSuperNode extends ExchangeNode {
    HashMap<String, Address> superNodeAddress;
    HashMap<String, Address> localNodeAddress;

    public ExchangeSuperNode(Address localAddress) {
        this(localAddress.name,localAddress.region,localAddress);
    }
    public ExchangeSuperNode(String name, String region, Address localAddress) {
        super(name, region, localAddress,null,true);
        superNodeAddress = new HashMap<String, Address>();
        localNodeAddress = new HashMap<String, Address>();
        Message message = new Message(Message.MSGTYPE.SUPER, localAddress,Engine.engineAddress,"");
        Message response = Message.readMessage(Message.sendMessageToLocal(message));
        updateSupers(response);
    }

    @Override
    public Message processMessage(Message message){
        if (message.msgtype == Message.MSGTYPE.TERMINATE) {
            terminate = true;
            return message;
        }

        if (message.msgtype == Message.MSGTYPE.TICK) {
            broadcastToLocals(message);
            tick();
            return message;
        }

        if (message.receiver.name.equals(name)) {
            if (message.msgtype == Message.MSGTYPE.LOCAL) {
                localNodeAddress.put(message.sender.name, message.sender);
            } else if (message.msgtype == Message.MSGTYPE.SUPER) {
                updateSupers(message);
                //superNodeAddress.put(message.sender.region, message.sender);
            } else {
                return super.processMessage(message);
            }
        } if (message.receiver.region.equals(region)) {
            if (!localNodeAddress.containsKey(message.receiver.name)) {
                message.payload = "NACK";
                return message;
            }

            message.receiver = localNodeAddress.get(message.receiver.name);
            String response = Message.sendMessageToLocal(message);
            if (response == null) {
                localNodeAddress.remove(message.receiver.name);
                message.payload = "NACK";
                return message;
            }
        } else {
            if (!superNodeAddress.containsKey(message.receiver.region)) {
                message.payload = "NACK";
                return message;
            }

            String response = Message.sendMessage(message, superNodeAddress.get(message.receiver.region) );
            if (response == null) {
                superNodeAddress.remove(message.receiver.region);
                message.payload = "NACK";
                return message;
            }
        }
        return message;
    }

    public void updateSupers(Message message){
        Gson gson = new Gson();
        Type typeOfHashMap = new TypeToken<HashMap<String, Address>>() { }.getType();
        HashMap<String, Address> newMap = gson.fromJson(message.payload, typeOfHashMap); // This type must match TypeToken
        superNodeAddress = newMap;
    }

    public void broadcastToLocals(Message message){
        for (Address a : localNodeAddress.values()){
            Message.sendMessage(message, a);
        }
    }
}
