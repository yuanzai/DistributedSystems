import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Created by junyuanlau on 14/5/16.
 */
public class Holdings extends HashMap<String, HashMap<String, Integer>> {
    public String name;

    public Holdings(String name){
        this.name = name;
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

    public String getPayload(){
        Gson gson = new Gson();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("data", gson.toJson(this));
        map.put("name", name);
        return gson.toJson(map);
    }

    public static Holdings fromPayload(Message message){
        Gson gson = new GsonBuilder().create();
        Type typeOfHashMap = new TypeToken<HashMap<String, String>>() { }.getType();
        HashMap<String, String> map = gson.fromJson(message.payload, typeOfHashMap);
        Holdings h = new Holdings(map.get("name"));
        typeOfHashMap = new TypeToken<HashMap<String, HashMap<String, Integer>>>() { }.getType();
        HashMap<String, HashMap<String, Integer>> map2 = gson.fromJson(map.get("data"), typeOfHashMap);
        h.putAll(map2);
        return h;
    }
}
