import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by junyuanlau on 28/4/16.
 */
public class ExchangeSuperNode extends ExchangeNode {
    HashMap<String, Address> superNodeAddress;
    HashMap<String, Address> localNodeAddress;
    HashSet<String> groupBackupSuperNodes = new HashSet<String>();
    boolean isPrimary = true;

    public ExchangeSuperNode(Address localAddress) {
        this(localAddress.name, localAddress.region, localAddress);
    }

    public static ExchangeSuperNode upgradeToSuper(ExchangeNode node){
        return (ExchangeSuperNode) node;
    };
    public ExchangeSuperNode(String name, String region, Address localAddress) {
        super(name, region, localAddress, null, true);
        superNodeAddress = new HashMap<String, Address>();
        localNodeAddress = new HashMap<String, Address>();
        sendSupernodeInfoToEngine(localAddress, "PRIMARY");
    }

    /*
    public void backupSupernode(){
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("local", gson.toJson(localNodeAddress));
        data.put("super", gson.toJson(superNodeAddress));
        Message message = new Message(Message.MSGTYPE.DATA_SUPERNODE, local, backupSupernode, gson.toJson(data));
        Message.sendMessageToLocal(message);
    }

    public void getBackupSupernodeData(Message message){
        Type typeOfHashMap = new TypeToken<HashMap<String, String>>() {}.getType();
        HashMap<String, String> data = gson.fromJson(message.payload, typeOfHashMap); // This type must match TypeToken
        localNodeAddress = gson.fromJson(data.get("local"), typeOfHashMap);
        superNodeAddress = gson.fromJson(data.get("super"), typeOfHashMap);

    }
    */
    @Override
    public Message processMessage(Message message) {
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

            if (message.msgtype == Message.MSGTYPE.NODE_NEW_LOCAL) {
                localNodeAddress.put(message.sender.name, message.sender);
                /*
                if (backupSupernode == null){
                    sendSupernodeInfoToEngine(message.sender, "SECONDARY");
                    backupSupernode = message.sender;
                } else {
                    //backupSupernode();
                }
                message.sender = backupSupernode;
                */
            } else if (message.msgtype == Message.MSGTYPE.NODE_SUPER) {
                updateSupers(message);
                //backupSupernode();
            } else if (message.msgtype == Message.MSGTYPE.NODE_RESTART) {

                paxos.newProposal(new ArrayList<Address>(localNodeAddress.values()), message.sender);
                paxos.sendPrepare();
                if (paxos.state != Paxos.State.END){
                    TradeManager.log.info("[" + local.port + "] PAXOS RESTART NODE " + paxos.target.name + " FAILED.");
                    Message.sendTerminate(paxos.target.port);
                    return null;
                }
                if (paxos.source != local) {
                    Message send = new Message(Message.MSGTYPE.GET_DATA_HOLDINGS, local, paxos.source, paxos.target.name);
                    String response = Message.sendMessageToLocal(send);
                    Message reply = Message.readMessage(response);
                    Gson gson = new Gson();
                    replicatedHoldings.put(paxos.target.name, Holdings.fromPayload(message));
                    send = new Message(Message.MSGTYPE.GET_DATA_INVENTORY, local, paxos.source, paxos.target.name);
                    response = Message.sendMessageToLocal(send);
                    reply = Message.readMessage(response);
                    replicatedInventory.put(paxos.target.name, Inventory.fromPayload(message));
                }
                for (Address add : localNodeAddress.values()) {
                    if (replicatedHoldings.get(paxos.source)!= null) {
                        Message send = new Message(Message.MSGTYPE.DATA_HOLDINGS, local, add, replicatedHoldings.get(paxos.source).getPayload());
                        Message.sendMessageToLocal(send);
                    }
                    if (replicatedInventory.get(paxos.source)!= null) {
                        Message send = new Message(Message.MSGTYPE.DATA_INVENTORY, local, add, replicatedInventory.get(paxos.source).getPayload());
                        Message.sendMessageToLocal(send);
                    }

                }
            } else if (message.msgtype == Message.MSGTYPE.DATA_HOLDINGS) {
                Holdings h = Holdings.fromPayload(message);
                replicatedHoldings.put(h.name, h);
                broadcastHoldings(h, message.sender);
            } else if (message.msgtype == Message.MSGTYPE.DATA_INVENTORY) {
                Inventory i = Inventory.fromPayload(message);
                replicatedInventory.put(i.name, i);
                broadcastInventory(i, message.sender);
            } else if (message.msgtype == Message.MSGTYPE.DATA_SUPERNODE) {
                //getBackupSupernodeData(message);
            } else {
                return super.processMessage(message);
            }
        } else if (message.receiver.region.equals(region)) {
            TradeManager.log.fine("[" + local.port + "] Send Super2Local Route from "+name + " to " + message.receiver.name);

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
            TradeManager.log.fine("[" + local.port + "] Super2Local Route from "+name + " to " + message.receiver.name);
            message = Message.readMessage(response);

        } else {
            TradeManager.log.fine("[" + local.port + "] Send Super2Super Route from " + region + " to " + message.receiver.region);
            if (!superNodeAddress.containsKey(message.receiver.region)) {
                message.payload = "NACK";
                return message;
            }

            String response = Message.sendMessage(message, superNodeAddress.get(message.receiver.region));
            if (response == null) {
                superNodeAddress.remove(message.receiver.region);
                message.payload = "NACK";
                return message;
            }
            TradeManager.log.fine("[" + local.port + "] Super2Super Route from "+ region + " to " + message.receiver.region);


            message = Message.readMessage(response);
        }
        return message;
    }

    public void sendSupernodeInfoToEngine(Address address, String type){
        TradeManager.log.info("[" + local.port + "] UPDATING ENGINE SUPERNODE DATA");
        Message message = new Message(Message.MSGTYPE.NODE_SUPER, address, Engine.engineAddress, type);
        String received = Message.sendMessageToLocal(message);
        Message response = Message.readMessage(received);
        updateSupers(response);
    }

    public void updateSupers(Message message) {
        Gson gson = new Gson();
        Type typeOfHashMap = new TypeToken<HashMap<String, Address>>() {
        }.getType();
        HashMap<String, Address> newMap = gson.fromJson(message.payload, typeOfHashMap); // This type must match TypeToken
        superNodeAddress = newMap;
    }

    public void broadcastToLocals(Message message) {
        for (Address a : localNodeAddress.values()) {
            Message.sendMessage(message, a);
        }
    }

    @Override
    public void responseSellFills(ArrayList<Order> orders) {
        super.responseSellFills(orders);
    }

    @Override
    public void responseSellFilled(Order order) {
        String country = order.origin;
        String region = countryToContinent.get(country);
        if (country == null){
            System.out.println(order.getPayload());
        }
        if (country.equals(local.name)) {
            receiveSellOrders(order);
        } else if (region.equals(local.region)) {
            Address add = localNodeAddress.get(country);
            Message message = new Message(Message.MSGTYPE.ORDER, local, add, order.getPayload());
            Message.sendMessageToLocal(message);

        } else {
            Address add = superNodeAddress.get(region);
            Message message = new Message(Message.MSGTYPE.ORDER, local, new Address(country, region), order.getPayload());
            Message.sendMessage(message, add);
        }
    }

    @Override
    public void broadcastHoldings(Holdings holdings, Address exAddress) {
        for (Address add : localNodeAddress.values()) {
            if (!add.equals(exAddress)) {
                Message send = new Message(Message.MSGTYPE.DATA_HOLDINGS, local, add, holdings.getPayload());
                Message.sendMessageToLocal(send);
            }
        }
    }

    @Override
    public void broadcastInventory(Inventory inventory, Address exAddress) {
        for (Address add : localNodeAddress.values()) {
            if (!add.equals(exAddress)) {
                Message send = new Message(Message.MSGTYPE.DATA_INVENTORY, local, add, inventory.getPayload());
                Message.sendMessageToLocal(send);
            }
        }
    }
}
