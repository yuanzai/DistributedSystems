/**
 * Created by junyuanlau on 28/4/16.
 */
public class Order {

    enum OrderType {BUY, SELL}
    String counterparty;
    String ticker;
    String exchange;
    String region;
    OrderType orderType;
    int quantity;
    int datetime;
    int orderID;

    int filledQuantity;
    int remainingQuantity;
    double executedPrice;
    int executedDateTime;
    int partialID;
    int fillCount;



    public Order partFill(int filledQuantity, double executedPrice, int executedDateTime){
        Order partOrder = new Order();
        partOrder.remainingQuantity = this.remainingQuantity - filledQuantity;
        partOrder.filledQuantity = filledQuantity;
        partOrder.executedPrice = executedPrice;
        partOrder.executedDateTime = executedDateTime;
        fillCount++;
        partOrder.partialID = fillCount;

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
}
