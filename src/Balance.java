import java.util.HashMap;

/**
 * Created by junyuanlau on 5/5/16.
 */
public class Balance extends HashMap<String,HashMap<String, Integer>> {
    public Balance(){

    }

    public Balance(byte[] data){

    }

    public int checkBalance(String counterparty, String ticker){
        if (this.get(counterparty) == null) {
            return 0;
        }
        HashMap<String, Integer> tickerBalance = this.get(counterparty);
        if (!tickerBalance.containsKey(ticker)){
            return 0;
        }
        return tickerBalance.get(ticker);
    }

    public void addBalance(String counterparty, String ticker, int quantity) {
        if (this.get(counterparty) == null){
            this.put(counterparty, new HashMap<String, Integer>());
        }
        HashMap<String, Integer> tickerBalance = this.get(counterparty);
        if (!tickerBalance.containsKey(ticker)){
            tickerBalance.put(ticker, 0);
        }
        tickerBalance.put(ticker, tickerBalance.get(ticker) + quantity);
    }
}
