import java.util.HashMap;

/**
 * Created by junyuanlau on 14/5/16.
 */
public class Holdings extends HashMap<String, HashMap<String, Integer>> {
    public Holdings(){

    }

    public Holdings(byte[] data){

    }

    public void updateHoldings(Order order){
        if (order.orderType == Order.OrderType.BUY){
            if (!this.containsKey(order.counterparty)) {
                this.put(order.counterparty, new HashMap<String, Integer>());
            }
            if (!this.get(order.counterparty).containsKey(order.ticker)){
                this.get(order.counterparty).put(order.ticker, 0);
            }
                this.get(order.counterparty).put(order.ticker, order.filledQuantity + this.get(order.counterparty).get(order.ticker));
        } else {
            this.get(order.counterparty).put(order.ticker, order.filledQuantity - this.get(order.counterparty).get(order.ticker));
        }
    }

    public boolean checkHoldings(Order order){
        if (this.containsKey(order.counterparty)) {
            if (this.get(order.counterparty).containsKey(order.ticker)){
                return this.get(order.counterparty).get(order.ticker) >= order.quantity;
            }
        }
        return false;
    }
}
