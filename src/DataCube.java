import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by junyuanlau on 14/5/16.
 */
public class DataCube {
    HashMap<String, HashMap<String, Integer>> issueMap = new HashMap<String, HashMap<String, Integer>>();
    HashMap<String, HashMap<String, Double>> priceData = new HashMap<String, HashMap<String, Double>>();
    HashMap<String, Double> cashBalance = new HashMap<String, Double>();

    LinkedHashMap<Long, String> timeMap = new LinkedHashMap<Long, String>();
    HashMap<String, String> countryToContinent = new HashMap<String, String>();
    HashMap<String, String> tickerToCountry = new HashMap<String, String>();
    HashMap<String, String> marketToCountry = new HashMap<String, String>();

    public DataCube(){};

    public void generateCompanyStaticData(String path) {
        generateStaticData(path, true);
    }

    public void generateMarketStaticData(String path) {
        generateStaticData(path, false);
    }

    public void generateStaticData(String path, boolean isCompanyData) {
        String[] continents = null;
        String[] countries = null;
        String[] tickers = null;
        String[] markets = null;

        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            //FileInputStream fil = new FileInputStream(new File(path));


            String line;
            while ((line = in.readLine()) != null){
                String[] words = line.split(",");
                if (words[2].toLowerCase().equals("continent")) {
                    continents = words;
                } else if (words[2].toLowerCase().equals("country")) {
                    countries = words;
                } else if (words[2].toLowerCase().equals("stock")) {
                    tickers = words;
                } else if (words[2].toLowerCase().equals("market")) {
                    markets = words;
                } else {
                    long dateTime = dateFormat(words[0], words[1]);
                    String[] issues = words;
                    for (int i = 3; i < issues.length; i++) {
                        if (!issues[i].equals("")) {

                            String ticker = tickers[i];
                            String key = issueMapKey(dateTime, countries[i]);

                            if (isCompanyData) {
                                int quantity = Integer.parseInt(issues[i]);
                                if (!issueMap.containsKey(key)) {
                                    issueMap.put(key, new HashMap<String, Integer>());
                                }
                                HashMap<String, Integer> tickerMap = issueMap.get(key);
                                if (tickerMap.get(ticker) != null)
                                    quantity += tickerMap.get(ticker);
                                tickerMap.put(ticker, quantity);
                            } else {
                                timeMap.put(dateTime, words[0] +" "+ words[1]);

                                if (!priceData.containsKey(key)) {
                                    priceData.put(key, new HashMap<String, Double>());
                                }
                                HashMap<String, Double> tickerMap = priceData.get(key);
                                tickerMap.put(ticker, Double.parseDouble(words[i]));
                            }
                        }
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isCompanyData) {
            for (int i = 2; i < countries.length; i++) {
                countryToContinent.put(countries[i], continents[i]);
                tickerToCountry.put(tickers[i], countries[i]);
                marketToCountry.put(markets[i], countries[i]);
            }
        }
    }

    public HashMap<String, Integer> getIssueQuantity(long datetime, String country) {
        return issueMap.get(issueMapKey(datetime, country));
    }

    public HashMap<String, Double> getPriceData(long datetime, String country) {
        return priceData.get(issueMapKey(datetime, country));
    }

    public LinkedHashMap<Long, String> getTimeMap(){
        return timeMap;
    }


    public long dateFormat(String date, String time) {
        DateFormat formatter = new SimpleDateFormat("MM/dd/yy hh:mm");
        Date formattedDate = null;
        try {
            formattedDate = (Date) formatter.parse(date + " " + time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return formattedDate.getTime();

    }

    public static String issueMapKey(long dateTime, String country){
        return "" + dateTime + "|" + country;
    }

    public static String updateCashBalancePayload(String counterparty, double amount){
        return "" + counterparty + "|" + amount;
    }

    public static String getUpdateCashBalanceCpty(String payload){
        String[] result = payload.split("\\|");
        return result[0];
    }

    public static double getUpdateCashBalanceAmount(String payload){
        String[] result = payload.split("\\|");
        return Double.parseDouble(result[1]);
    }

    public void generateCashBalances(int n){
        for (int i = 1; i<=n; i++){
            cashBalance.put(String.valueOf(i),10000.0);
        }
    }
    public boolean checkBalance(Double value, String counterparty){
        if (!cashBalance.containsKey(counterparty))
            return false;
        return cashBalance.get(counterparty)>= value;
    }
    public boolean updateBalance(Double value, String counterparty){
        if (!cashBalance.containsKey(counterparty))
            return false;
        cashBalance.put(counterparty, cashBalance.get(counterparty)+value);
        return true;
    }
}
