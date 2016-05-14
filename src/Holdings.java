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
        if (order == null)
            return;
        if (order.isInventorySale)
            return;

        if (order.orderType == Order.OrderType.BUY){
            if (!this.containsKey(order.counterparty)) {
                this.put(order.counterparty, new HashMap<String, Integer>());
            }
            if (!this.get(order.counterparty).containsKey(order.ticker)){
                this.get(order.counterparty).put(order.ticker, 0);
            }
                this.get(order.counterparty).put(order.ticker, order.filledQuantity + this.get(order.counterparty).get(order.ticker));
        } else {

            this.get(order.counterparty).put(order.ticker, this.get(order.counterparty).get(order.ticker) - order.filledQuantity);
        }
    }

    public int checkHoldings(String counterparty, String ticker){
        if (this.containsKey(counterparty)) {
            if (this.get(counterparty).containsKey(ticker)){
                return this.get(counterparty).get(ticker);
            }
        }
        return 0;
    }
}
