import java.util.HashMap;

/**
 * Created by junyuanlau on 5/5/16.
 */
public class Inventory extends HashMap<String, Integer> {
    public Inventory (){

    }

    public Inventory (byte[] data){

    }

    public int checkBalance(String ticker){
        if (!this.containsKey(ticker))
            return 0;
        return this.get(ticker);
    }

    public Trade fillBuyOrder(Order order, HashMap<String, Double> prices){
        double price = prices.get(order.ticker);
        int remaining = checkBalance(order.ticker);
        if (remaining == 0)
            return null;

        int filled = Math.min(remaining , order.quantity);
        Trade fill = new Trade(order, null, filled,price);
        this.put(order.ticker, this.get(order.ticker) - filled);

        return fill;
    }
}
