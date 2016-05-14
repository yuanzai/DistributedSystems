import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by junyuanlau on 5/5/16.
 */
public class OrderList extends HashMap<String, LinkedList<Order>> {

    public ArrayList<Order> executeBuyOrder(Order buy, HashMap<String, Double> prices){
        ArrayList<Order> filledSellOrders = new ArrayList<Order>();
        double price = prices.get(buy.ticker);

        int buyQuantity = buy.quantity;
        if (this.get(buy.ticker) == null)
            return null;

        while (buyQuantity > 0){
            Order sell = this.get(buy.ticker).removeFirst();
            int sellQuantity = sell.quantity - sell.filledQuantity;
            if (sell.quantity <= buyQuantity) {
                buyQuantity -= sell.quantity;
                sell.filledQuantity = sell.quantity;
                filledOrders.add(sell);

            } else {
                filledOrders.add(sell);

                sell.quantity -= buyQuantity;
                this.get(buy.ticker).addFirst(sell);
                buyQuantity = 0;
            }
        }
        return filledSellOrders;
    }

    public void queueSellOrder(Order sell){
        if (this.get(sell.ticker) == null){
            LinkedList<Order> sellOrders = new LinkedList<Order>();
            sellOrders.addFirst(sell);
            this.put(sell.ticker, sellOrders);
        } else {
            this.get(sell.ticker).addFirst(sell);
        }
    }
}
