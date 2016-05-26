import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by junyuanlau on 26/5/16.
 */
public class SuperNode {
    HashMap<String, Address> superNodeAddress = new HashMap<String, Address>();;
    HashMap<String, Address> localNodeAddress = new HashMap<String, Address>();;
    HashSet<String> groupBackupSuperNodes = new HashSet<String>();
    Address local;
    public boolean isPrimary = true;
    Address backupSupernode;
    ExchangeNode node;
    String name;
    Paxos paxos;
    Gson gson = new Gson();

    public SuperNode(ExchangeNode node){
        this.node = node;
        this.local = node.local;
        this.name = node.name;
        this.paxos = node.paxos;
        if (isPrimary)
            sendSupernodeInfoToEngine(local, "PRIMARY");
        else
            sendSupernodeInfoToEngine(local, "SECONDARY");
    }

    public void backupSupernode(){
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("local", gson.toJson(localNodeAddress));
        data.put("super", gson.toJson(superNodeAddress));
        Message message = new Message(Message.MSGTYPE.DATA_SUPERNODE, local, backupSupernode, gson.toJson(data));
        Message.sendMessageToLocal(message);
    }

    public void getBackupSupernodeData(Message message){
        TradeManager.log.info("[" + local.port + "] UPDATING BACKUP SUPERNODE DATA");

        Type typeOfHashMap = new TypeToken<HashMap<String, String>>() {}.getType();
        HashMap<String, String> data = gson.fromJson(message.payload, typeOfHashMap); // This type must match TypeToken
        localNodeAddress = gson.fromJson(data.get("local"), typeOfHashMap);
        superNodeAddress = gson.fromJson(data.get("super"), typeOfHashMap);

    }

    public Message processMessage(Message message) {
        TradeManager.log.info("[" + local.port + "] PROCESS AS SUPER");

        if (message.msgtype == Message.MSGTYPE.TERMINATE) {
            node.terminate = true;
            return message;
        }

        if (message.msgtype == Message.MSGTYPE.TICK) {
            broadcastToLocals(message);
            node.tick();
            return message;
        }
        if (isPrimary) {

            if (message.receiver.name.equals(name)) {

                if (message.msgtype == Message.MSGTYPE.NODE_NEW_LOCAL) {
                    localNodeAddress.put(message.sender.name, message.sender);
                    if (backupSupernode == null) {
                        backupSupernode = message.sender;
                    } else {
                        backupSupernode();
                    }
                    message.sender = backupSupernode;
                } else if (message.msgtype == Message.MSGTYPE.NODE_SUPER) {
                    updateSupers(message);
                    if (backupSupernode != null) {
                        backupSupernode();
                    }
                } else if (message.msgtype == Message.MSGTYPE.NODE_RESTART) {

                    paxos.newProposal(new ArrayList<Address>(localNodeAddress.values()), message.sender);
                    paxos.sendPrepare();
                    if (paxos.state != Paxos.State.END) {
                        TradeManager.log.info("[" + local.port + "] PAXOS RESTART NODE " + paxos.target.name + " FAILED.");
                        Message.sendTerminate(paxos.target.port);
                        return null;
                    }
                    if (paxos.source != local) {
                        Message send = new Message(Message.MSGTYPE.GET_DATA_HOLDINGS, local, paxos.source, paxos.target.name);
                        String response = Message.sendMessageToLocal(send);
                        Message reply = Message.readMessage(response);
                        Gson gson = new Gson();
                        node.replicatedHoldings.put(paxos.target.name, Holdings.fromPayload(message));
                        send = new Message(Message.MSGTYPE.GET_DATA_INVENTORY, local, paxos.source, paxos.target.name);
                        response = Message.sendMessageToLocal(send);
                        reply = Message.readMessage(response);
                        node.replicatedInventory.put(paxos.target.name, Inventory.fromPayload(message));
                    }
                    for (Address add : localNodeAddress.values()) {
                        if (node.replicatedHoldings.get(paxos.source) != null) {
                            Message send = new Message(Message.MSGTYPE.DATA_HOLDINGS, local, add, node.replicatedHoldings.get(paxos.source).getPayload());
                            Message.sendMessageToLocal(send);
                        }
                        if (node.replicatedInventory.get(paxos.source) != null) {
                            Message send = new Message(Message.MSGTYPE.DATA_INVENTORY, local, add, node.replicatedInventory.get(paxos.source).getPayload());
                            Message.sendMessageToLocal(send);
                        }

                    }
                } else if (message.msgtype == Message.MSGTYPE.DATA_HOLDINGS) {
                    Holdings h = Holdings.fromPayload(message);
                    node.replicatedHoldings.put(h.name, h);
                    broadcastHoldings(h, message.sender);
                } else if (message.msgtype == Message.MSGTYPE.DATA_INVENTORY) {
                    Inventory i = Inventory.fromPayload(message);
                    node.replicatedInventory.put(i.name, i);
                    broadcastInventory(i, message.sender);

                } else {
                    return node.processMessage(message);
                }
            } else if (message.receiver.region.equals(node.region)) {
                TradeManager.log.fine("[" + local.port + "] Send Super2Local Route from " + name + " to " + message.receiver.name);

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
                TradeManager.log.fine("[" + local.port + "] Super2Local Route from " + name + " to " + message.receiver.name);
                message = Message.readMessage(response);

            } else {
                TradeManager.log.fine("[" + local.port + "] Send Super2Super Route from " + node.region + " to " + message.receiver.region);
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
                TradeManager.log.fine("[" + local.port + "] Super2Super Route from " + node.region + " to " + message.receiver.region);


                message = Message.readMessage(response);
            }
        } else {
            if (message.receiver.name.equals(name) && message.msgtype == Message.MSGTYPE.DATA_SUPERNODE) {
                getBackupSupernodeData(message);
            } else {
                return node.processMessage(message);
            }
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


    public void responseSellFills(ArrayList<Order> orders) {
        node.responseSellFills(orders);
    }


    public void responseSellFilled(Order order) {
        String country = order.origin;
        String region = node.countryToContinent.get(country);
        if (country == null){
            System.out.println(order.getPayload());
        }
        if (country.equals(local.name)) {
            node.receiveSellOrders(order);
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

    public void broadcastHoldings(Holdings holdings, Address exAddress) {
        for (Address add : localNodeAddress.values()) {
            if (!add.equals(exAddress)) {
                Message send = new Message(Message.MSGTYPE.DATA_HOLDINGS, local, add, holdings.getPayload());
                Message.sendMessageToLocal(send);
            }
        }
    }

    public void broadcastInventory(Inventory inventory, Address exAddress) {
        for (Address add : localNodeAddress.values()) {
            if (!add.equals(exAddress)) {
                Message send = new Message(Message.MSGTYPE.DATA_INVENTORY, local, add, inventory.getPayload());
                Message.sendMessageToLocal(send);
            }
        }
    }
}
