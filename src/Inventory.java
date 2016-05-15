import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public Order fillBuyOrder(Order buy, double price, long datetime){
        int remaining = checkBalance(buy.ticker);
        if (remaining == 0)
            return null;

        int filled = Math.min(remaining , buy.remainingQuantity);
        Order fill = new Order(null,buy.ticker, buy.exchange, buy.region, buy.orderType, filled, datetime, UUID.randomUUID(),true );
        buy.fillOrder(filled, price, datetime);
        this.put(buy.ticker, this.get(buy.ticker) - filled);

        return fill;
    }

    public void updateIssue(HashMap<String, Integer> issueMap){
        if (issueMap == null)
            return;
        for (Map.Entry<String, Integer> entry : issueMap.entrySet()){
            if (!this.containsKey(entry.getKey())){
                this.put(entry.getKey(), 0);
            }
            this.put(entry.getKey(), this.get(entry.getKey()) + entry.getValue());
        }
    }
}
