/**
 * Created by junyuanlau on 16/5/16.
 */
import java.sql.*;
import java.util.HashMap;

public class SqliteDB {
    private static String connectionString = "jdbc:sqlite:data.db";
    Connection c = null;
    Statement stmt = null;
    final static String ISSUES = "ISSUES";
    final static String PRICES = "PRICES";
    final static String ISSUES_COL = "AMOUNT";
    final static String PRICES_COL = "PRICE";

    public void open(){
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(connectionString);
        } catch ( Exception e ) {
            e.printStackTrace();

        }
    }

    public void createIssueTable() {
        createGenericTable(ISSUES, ISSUES_COL);
    }

    public void createPricesTable(){
        createGenericTable(PRICES, PRICES_COL);
    }

    private void createGenericTable(String name, String column){
        try {
            stmt = c.createStatement();
            String sql = "DROP TABLE IF EXISTS " + name;
            stmt.executeUpdate(sql);


            sql = "CREATE TABLE " + name + " " +
                    "(COUNTRY CHAR(100) NOT NULL, " +
                    " DATETIME NUMBER NOT NULL, " +
                    " TICKER CHAR(100), " +
                    " "+ column +" NUMBER)";
            stmt.executeUpdate(sql);
            stmt.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    public void startInsertStatement(){
        try {
            c.setAutoCommit(false);
            stmt = c.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertPriceTable(String country, long datetime, String ticker, double amount) {
        try {
            String sql = "INSERT INTO "+PRICES+" (COUNTRY,DATETIME,TICKER,"+PRICES_COL+") " +
                    "VALUES ('"+ country +"', "+ datetime +", \""+ ticker +"\", "+ amount +");";

            stmt.executeUpdate(sql);

            stmt.close();
            c.commit();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    public void insertIssueTable(String country, long datetime, String ticker, int amount){
        try {
            String sql = "INSERT INTO "+ISSUES+" (COUNTRY,DATETIME,TICKER,"+ISSUES_COL+") " +
                    "VALUES ('"+ country +"', "+ datetime +", \""+ ticker +"\", "+ amount +");";
            stmt.executeUpdate(sql);

            stmt.close();
            c.commit();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }



    public HashMap<String, Integer> getIssueTable(String country, long datetime){
        ResultSet rs = null;
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        try {
            stmt = c.createStatement();
            rs = stmt.executeQuery( "SELECT TICKER, "+PRICES_COL+" FROM "+PRICES+" WHERE COUNTRY ='"+ country +"' AND DATETIME ="+ datetime +";" );
            while ( rs.next() ) {
                String ticker = rs.getString("TICKER");
                int amount = rs.getInt(ISSUES_COL);
                result.put(ticker, amount);
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public HashMap<String, Double> getPriceTable(String country, long datetime){
        ResultSet rs = null;
        HashMap<String, Double> result = new HashMap<String, Double>();
        try {
            stmt = c.createStatement();
            rs = stmt.executeQuery( "SELECT TICKER, "+PRICES_COL+" FROM " +PRICES+ " WHERE COUNTRY ='"+ country +"' AND DATETIME ="+ datetime +";" );
            while ( rs.next() ) {
                String ticker = rs.getString("TICKER");
                double amount = rs.getDouble(PRICES_COL);
                result.put(ticker, amount);
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }


    public void endInsertStatement(){
        try {
            stmt.close();
            c.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public void close(){
        try {
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
