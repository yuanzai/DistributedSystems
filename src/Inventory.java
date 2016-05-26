import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by junyuanlau on 5/5/16.
 */
public class Inventory extends HashMap<String, Integer> {
    public String name;

    public Inventory (String name){
        this.name = name;
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

    public String getPayload(){
        Gson gson = new Gson();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("data", gson.toJson(this));
        map.put("name", name);
        return gson.toJson(map);
    }

    public static Inventory fromPayload(Message message){
        Gson gson = new GsonBuilder().create();
        Type typeOfHashMap = new TypeToken<HashMap<String, String>>() { }.getType();
        HashMap<String, String> map = gson.fromJson(message.payload, typeOfHashMap);
        Inventory i = new Inventory(map.get("name"));
        typeOfHashMap = new TypeToken<HashMap<String, Integer>>() { }.getType();
        HashMap<String, Integer> map2 = gson.fromJson(map.get("data"), typeOfHashMap);
        i.putAll(map2);
        return i;
    }
}
