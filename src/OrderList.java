import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by junyuanlau on 5/5/16.
 */
public class OrderList extends HashMap<String, LinkedList<Order>> {

    public boolean checkValid(Order buy) {

        LinkedList<Order> queue = this.get(buy.ticker);
        if (queue == null)
            return true;
        if (queue.size() == 0)
            return true;
        for (Order sell : queue){
            if (sell.counterparty.equals(buy.counterparty))
                return false;
        }
        return true;
    }

    public ArrayList<Order> executeBuyOrder(Order buy, double price, long datetime){
        buy.status = Order.OrderStatus.PENDING;
        ArrayList<Order> filledSellOrders = new ArrayList<Order>();

        LinkedList<Order> queue = this.get(buy.ticker);
        if (queue == null)
            return filledSellOrders;


        Order sell = queue.peekFirst();
        while (buy.remainingQuantity > 0 && sell != null){

            int sellQuantity = sell.quantity - sell.filledQuantity;
            if (sell.remainingQuantity <= buy.remainingQuantity) {
                // sell order is smaller or matches buy order
                buy.fillOrder(sell.remainingQuantity, price, datetime);
                sell.fillOrder(sell.remainingQuantity, price, datetime);

                sell.status = Order.OrderStatus.FILLED;
                queue.remove(sell);
                //}sell = queue.removeFirst();

                filledSellOrders.add(sell);

            } else {
                // sell order is partially filled
                // sell order is not removed from queue
                Order partFilled = sell.partFillSell(buy.remainingQuantity,price,0);
                buy.fillOrder(sell.remainingQuantity, price, datetime);
                buy.status = Order.OrderStatus.FILLED;
                filledSellOrders.add(partFilled);
            }

            sell = queue.peekFirst();
        }
        return filledSellOrders;
    }

    public void queueSellOrder(Order sell){
        sell.status = Order.OrderStatus.PENDING;
        if (this.get(sell.ticker) == null){
            LinkedList<Order> sellOrders = new LinkedList<Order>();
            sellOrders.addFirst(sell);
            this.put(sell.ticker, sellOrders);
        } else {
            this.get(sell.ticker).addFirst(sell);
        }
    }

    public int checkSellQueue(String counterparty, String ticker){
        LinkedList<Order> queue = this.get(ticker);
        if (queue == null)
            return 0;
        int sum = 0;
        for (Order order : queue){
            if (order.counterparty.equals(counterparty)){
                sum += order.remainingQuantity;
            }
        }
        return sum;
    }
}
