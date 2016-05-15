import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by junyuanlau on 9/5/16.
 */
public class Engine {
    StaticData staticData;
    CashBalance cashBalance;
    ServerSocket serverSocket;
    public static Address engineAddress = new Address("localhost", 10000);

    public static void main(String[] args) {
        StaticData csd = new StaticData();
        StaticData csd2 = new StaticData();
        //csd.generateCompanyStaticData(testStockQty);
        System.out.println(csd.dateFormat("1/1/2016", "10:00"));
        System.out.println(csd.dateFormat("1/10/2016", "18:00"));
        Date date = new Date();
        date.setTime(1452470400000L);
        System.out.println(date.toString());
    }

    public Engine() {

    }

    public static void launchEngineThread(){
        EngineThread engineThread = new EngineThread("Engine");
        engineThread.start();
    }

    public void start(){
        serverSocket = null;

        try {
            serverSocket = new ServerSocket(engineAddress.port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            Socket clientSocket = null;
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                clientSocket = serverSocket.accept();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("LISTENING ON " + engineAddress.port);
            try {
                String data = in.readLine();
                Message received = Message.readMessage(data);
                Message response = processMessage(received);

                out.println(response.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void init() {
        cashBalance = new CashBalance(100);
    }

    public Message processMessage(Message message) {
        if (message.msgtype == Message.MSGTYPE.CASHBALANCE) {
            if (!cashBalance.containsKey(message.payload))
                message.payload = "NOTFOUND";
            else
                message.payload = cashBalance.get(message.payload).toString();

        }
        return message;
    }


    public HashMap<String, Integer> getIssueQuantity(long datetime, String country) {
        return staticData.getIssueQuantity(datetime, country);
    }

    public HashMap<String, Double> getPriceData(long datetime, String country) {
        return staticData.getPriceData(datetime, country);
    }

    public static String sendMessage(Message message) {
        System.out.println("SENDING TO " + message.receiver.host + " " + message.receiver.port);

        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            echoSocket = new Socket(message.receiver.host, message.receiver.port);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        } catch (IOException e) {
            return "FAIL";
        }

        out.println(message.getMessage());

        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                return inputLine;
            }

        } catch (IOException e) {
            return "FAIL";
        }
        return "FAIL";
    }
}
class EngineThread implements Runnable {
    private Thread t;
    private String threadName;

    EngineThread( String name){
        threadName = name;
        System.out.println("Creating " +  threadName );
    }

    public void run() {
        System.out.println("Running " +  threadName );

        Engine eng = new Engine();
        eng.init();
        eng.start();
    }

    public void start() {
        if (t == null)
        {
            t = new Thread (this, threadName);
            t.start ();
        }
    }

}


class StaticData {
    HashMap<String, HashMap<String, Integer>> issueMap = new HashMap<String, HashMap<String, Integer>>();
    HashMap<String, HashMap<String, Double>> priceData = new HashMap<String, HashMap<String, Double>>();

    LinkedHashMap<Long, String> timeMap = new LinkedHashMap<Long, String>();
    HashMap<String, String> countryToContinent = new HashMap<String, String>();
    HashMap<String, String> tickerToCountry = new HashMap<String, String>();
    HashMap<String, String> marketToCountry = new HashMap<String, String>();

    public StaticData(){};

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

    public String issueMapKey(long dateTime, String country){
        return "" + dateTime + "|" + country;
    }
}

class CashBalance extends HashMap<String, Double>{
    public CashBalance(int n){
        for (int i = 1; i<=n; i++){
            this.put(String.valueOf(i),10000.0);
        }
    }
    public boolean checkBalance(Double value, String counterparty){
        if (!this.containsKey(counterparty))
            return false;
        return this.get(counterparty)>= value;
    }
    public boolean updateBalance(Double value, String counterparty){
        if (!this.containsKey(counterparty))
            return false;
        this.put(counterparty, this.get(counterparty)+value);
        return true;
    }

}

