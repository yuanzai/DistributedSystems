import com.google.gson.Gson;

import java.util.UUID;

/**
 * Created by junyuanlau on 28/4/16.
 */
public class Order {

    enum OrderType {BUY, SELL}
    enum OrderStatus {CREATED, PENDING, FILLED, PARTFILL, ERROR, REJECT, NOCASH, ACK, NOINVENTORY}
    String counterparty;
    String ticker;
    String exchange;
    String region;
    OrderType orderType;
    int quantity;
    long datetime;
    UUID orderID;
    OrderStatus status;

    int filledQuantity;
    int remainingQuantity;
    double executedPrice;
    long executedDateTime;
    int partialID;
    int fillCount;

    boolean isInventorySale;

    public static Order readOrder(String data) {
        Gson gson = new Gson();
        return gson.fromJson(data, Order.class);
    }

    public String getPayload(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public Order() {}

    public Order(String counterparty, String ticker, String exchange, String region, OrderType type, int quantity, long datetime, UUID orderID, boolean isInventorySale){
        this.counterparty = counterparty;
        this.ticker = ticker;
        this.exchange = exchange;
        this.region = region;
        this.orderType = type;
        this.quantity = quantity;
        this.datetime = datetime;
        this.orderID = orderID;
        
        this.quantity = quantity;
        this.remainingQuantity = quantity;
        this.partialID = 0;
        this.fillCount = 0;
        this.status = OrderStatus.CREATED;

        this.isInventorySale = isInventorySale;
        if (isInventorySale){
            this.counterparty = null;
            this.filledQuantity = this.remainingQuantity;
            this.remainingQuantity = 0;
            this.executedDateTime = datetime;
        }
    }

    public Order partFillSell(int filledQuantity, double executedPrice, int executedDateTime){
        this.filledQuantity += filledQuantity;
        this.remainingQuantity -= filledQuantity;
        Order partOrder = new Order();
        partOrder.remainingQuantity = this.remainingQuantity - filledQuantity;
        partOrder.filledQuantity = filledQuantity;
        partOrder.executedPrice = executedPrice;
        partOrder.executedDateTime = executedDateTime;
        fillCount++;
        partOrder.partialID = fillCount;
        partOrder.status = OrderStatus.PARTFILL;

        partOrder.counterparty = this.counterparty;
        partOrder.ticker = this.ticker;
        partOrder.exchange = this.exchange;
        partOrder.region = this.region;
        partOrder.orderType = this.orderType;
        partOrder.quantity = this.quantity;
        partOrder.datetime = this.datetime;
        partOrder.orderID = this.orderID;




        return partOrder;
    }

    public void fillOrder(int filledQuantity, double executedPrice, long executedDateTime){
        this.remainingQuantity -= filledQuantity;
        this.filledQuantity += filledQuantity;
        this.executedDateTime = executedDateTime;
        this.executedPrice = executedPrice;
    }

}
