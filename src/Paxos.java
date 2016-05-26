import com.google.gson.Gson;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by junyuanlau on 24/5/16.
 */
class Paxos {
    public enum State {START, PREPARE, ACCEPTREQUEST, END, FAIL};
    State state;
    ArrayList<Address> addresses;
    int audienceCount;
    int quorumCount;
    int x;
    ExchangeNode node;
    long val;
    Address target;
    Address source;
    boolean isRogue;
    int promiseCount;
    int acceptCount;

    HashMap<Long, ArrayList<Address>> valueMap = new HashMap<Long, ArrayList<Address>>();

    public Paxos(){};

    public Paxos(ExchangeNode node){
        this.node = node;
        this.state = State.START;
        this.source = null;
    }


    public void newProposal(ArrayList<Address> addresses, Address target){
        valueMap = new HashMap<Long, ArrayList<Address>>();
        this.addresses = addresses;
        this.audienceCount = addresses.size() - 1;
        this.promiseCount = 0;
        this.acceptCount = 0;
        this.target = target;
        this.state = State.START;
        this.source = null;
    }

    public void sendPrepare(){
        TradeManager.log.info("[" + node.local.port + "] [PAXOS] ["+node.name+"] PREPARE");
        state = State.PREPARE;
        ArrayList<Address> list = new ArrayList<Address>();
        list.add(node.local);
        Inventory inventory = node.replicatedInventory.get(target.name);
        Holdings holdings = node.replicatedHoldings.get(target.name);

        if (!(inventory == null || holdings == null))
            valueMap.put((long) inventory.hashCode() + (long) holdings.hashCode(), list);

        x++;
        for (Address add : addresses){
            if (this.target.equals(add))
                continue;
            PaxosProposal paxosProposal = new PaxosProposal();
            paxosProposal.x = x;
            paxosProposal.type = PaxosProposal.Type.PREPARE;
            paxosProposal.val = 0;
            paxosProposal.add = target;

            Gson gson = new Gson();

            Message message = new Message(Message.MSGTYPE.PAXOS, node.local, add, gson.toJson(paxosProposal));
            String response = Message.sendMessageToLocal(message);
            Message reply = Message.readMessage(response);
            paxosProposal = gson.fromJson(reply.payload, PaxosProposal.class);

            if (paxosProposal.type == PaxosProposal.Type.PROMISE) {
                TradeManager.log.info("[" + node.local.port + "] [PAXOS] ["+node.name+"] RECEIVED PROMISE");
                receivePromise();
                if (!valueMap.containsKey(paxosProposal.val)){
                    list = new ArrayList<Address>();
                    valueMap.put(paxosProposal.val, list);
                }
                list = valueMap.get(paxosProposal.val);
                list.add(message.receiver);


            } else if (paxosProposal.type == PaxosProposal.Type.PREPARENACK) {

            } else {

            }
        }
        boolean proposalMajority = false;
        for (Map.Entry<Long, ArrayList<Address>> entry : valueMap.entrySet()){
            if (entry.getValue().size() > audienceCount/2){
                val = entry.getKey();
                proposalMajority = true;
                break;
            }
        }
        if (promiseCount > (audienceCount/2) && proposalMajority){
            TradeManager.log.info("[" + node.local.port + "] [PAXOS] ["+node.name+"] SENDING ACCEPT REQUEST");
            sendAcceptRequest();

        } else {
            state = State.FAIL;
        }
    }

    synchronized public void receivePromise(){
        promiseCount++;
    }

    synchronized public void receiveAccepted(){
        acceptCount++;
    }

    public void sendAcceptRequest(){
        TradeManager.log.info("[" + node.local.port + "] [PAXOS] ["+node.name+"] ACCEPTREQUEST");
        state = State.ACCEPTREQUEST;
        quorumCount = 0;
        for (Address add : addresses) {
            if (this.target.equals(add))
                continue;
            Gson gson = new Gson();
            PaxosProposal paxosProposal = new PaxosProposal();
            paxosProposal.x = x;
            paxosProposal.type = PaxosProposal.Type.ACCEPTREQUEST;
            paxosProposal.val = val;

            Message message = new Message(Message.MSGTYPE.PAXOS, node.local, add, gson.toJson(paxosProposal));
            String response = Message.sendMessageToLocal(message);
            Message reply = Message.readMessage(response);
            paxosProposal = gson.fromJson(reply.payload, PaxosProposal.class);
            if (paxosProposal.type == PaxosProposal.Type.ACCEPTED) {
                receiveAccepted();
                TradeManager.log.info("[" + node.local.port + "] [PAXOS] ["+node.name+"] ACCEPTED PROMISE");

            } else if (paxosProposal.type == PaxosProposal.Type.ACCEPTREQUESTNACK) {

            } else {

            }
            try { Thread.sleep(15); } catch (InterruptedException e) { e.printStackTrace(); }
            if (acceptCount > (audienceCount/2)) {
                sendCommit();
            } else {
                state = State.FAIL;
            }
        }
    }

    public void sendCommit(){
        TradeManager.log.info("[" + node.local.port + "] [PAXOS] ["+node.name+"] COMMIT");
        ArrayList<Address> list = valueMap.get(val);
        source = list.get(0);
        state = State.END;
    }

    public Message processMessage(Message message){
        Gson gson = new Gson();
        PaxosProposal paxosProposal= gson.fromJson(message.payload, PaxosProposal.class);
        this.target = paxosProposal.add;
        if (paxosProposal.type == PaxosProposal.Type.PREPARE){
            if (paxosProposal.x > x){
                x = paxosProposal.x;
                paxosProposal.type = PaxosProposal.Type.PROMISE;
            } else {
                paxosProposal.x = x;
                paxosProposal.type = PaxosProposal.Type.PREPARENACK;
            }
            Inventory inventory = node.replicatedInventory.get(paxosProposal.add.name);
            Holdings holdings = node.replicatedHoldings.get(paxosProposal.add.name);

            paxosProposal.val = (long) inventory.hashCode() + (long) holdings.hashCode();
            Random random = new Random();
            if (isRogue)
                paxosProposal.val = random.nextInt(10000);

        } else if (paxosProposal.type == PaxosProposal.Type.ACCEPTREQUEST){
            if (paxosProposal.x >= x){
                x = paxosProposal.x;
                paxosProposal.type = PaxosProposal.Type.ACCEPTED;
            } else {
                paxosProposal.x = x;
                paxosProposal.type = PaxosProposal.Type.ACCEPTREQUESTNACK;
            }
            //paxosProposal.val = (long) node.inventory.hashCode() + (long) node.holdings.hashCode();

        } else {
            paxosProposal.type = PaxosProposal.Type.ERR;
        }
        TradeManager.log.info("[" + node.local.port + "] [PAXOS] ["+node.name+"] " + paxosProposal.type);
        message.payload = gson.toJson(paxosProposal);
        return message;
    }

}
class PaxosProposal {
    public int x;
    public long val;
    public Type type;
    enum Type {PREPARE, ACCEPTREQUEST, PROMISE, PREPARENACK, ACCEPTED, ACCEPTREQUESTNACK, ERR}
    Address add;
}
