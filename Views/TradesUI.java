import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

/**
 * Created by junyuanlau on 25/5/16.
 */
class TradesUI extends JPanel {
    ArrayList<JButton> tradeButtons = new ArrayList<JButton>();
    TableView tableView;
    public TradesUI(TradeManager tm){
        JFrame frame = new JFrame("low frequency trading");

        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setOpaque(true);

        JPanel row1 = new JPanel();
        row1.setLayout(new FlowLayout());

        JButton startTrading10 = new JButton("Simulate x 10");
        startTrading10.addActionListener(e -> {
            SimulateTrades st = new SimulateTrades("Simulator",tm,50,10);
            st.start();
        });

        JButton startTrading200 = new JButton("Simulate x 200");
        startTrading200.addActionListener(e -> {
            SimulateTrades st = new SimulateTrades("Simulator",tm,50,200);
            st.start();
        });

        JButton concurrentTrades = new JButton("Send all orders concurrently");
        concurrentTrades.addActionListener(e -> {
            for (JButton b: tradeButtons){
                b.doClick();
            }
        });

        JButton randomBuySell = new JButton("5) Random BuySell");
        randomBuySell.addActionListener(e -> {
            if (tm.isQ5) {
                tm.isQ5 = false;
            } else {
                tm.question5();
            }
        });

        JButton spin1000 = new JButton("6) 1000 Random BuySell");
        spin1000.addActionListener(e -> {
            tm.q6();
        });

        JButton log = new JButton("log?");
        log.addActionListener(e -> {
            tm.logMode = !tm.logMode;
        });


        tm.time = new JLabel("Hello World!!!!");
        row1.add(tm.time);
        row1.add(startTrading10);
        row1.add(startTrading200);
        row1.add(concurrentTrades);
        row1.add(randomBuySell);
        row1.add(spin1000);
        row1.add(log);
        ArrayList<JPanel> rows = new ArrayList<JPanel>();
        for (int i = 0; i < 2; i++)
            rows.add(new JPanel(new FlowLayout()));


        for (int i = 0; i < rows.size(); i++){
            JPanel row = rows.get(i);
            JLabel cptyL = new JLabel("Cpty");
            JTextField cpty = new JTextField("Cpty");
            cpty.setColumns(10);

            JLabel tickerL = new JLabel("Ticker");
            JTextField ticker = new JTextField("Ticker");
            ticker.setColumns(10);

            JLabel buySellL = new JLabel("BS");
            JTextField buySell = new JTextField("BS");
            buySell.setColumns(5);

            JLabel qtyL = new JLabel("Size");
            JTextField qty = new JTextField("Size");
            qty.setColumns(10);

            JButton trade = new JButton("Route trade");
            tradeButtons.add(trade);
            trade.addActionListener(e -> {
                String bs = buySell.getText();
                if (!bs.toUpperCase().equals("B") && !bs.toUpperCase().equals("S")){
                    JOptionPane.showMessageDialog(frame,"Please check BS","Trade error",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int size;
                try {
                    size = Integer.parseInt(qty.getText());
                } catch (NumberFormatException num_e){
                    JOptionPane.showMessageDialog(frame,"Please check size","Trade error",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String cptyName = cpty.getText();
                if (!tm.clientList.contains(cptyName)) {
                    JOptionPane.showMessageDialog(frame,"Please check cpty","Trade error",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String eq = ticker.getText();
                if (!tm.engine.dataCube.tickerToCountry.containsKey(eq)) {
                    JOptionPane.showMessageDialog(frame,"Please check ticker","Trade error",JOptionPane.WARNING_MESSAGE);
                    return;
                }

                tm.executeManualOrder(cptyName, eq, size, bs);
            });
            row.add(cptyL);
            row.add(cpty);
            row.add(tickerL);
            row.add(ticker);
            row.add(qtyL);
            row.add(qty);
            row.add(buySellL);
            row.add(buySell);
            row.add(trade);
        }




        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        tm.tradesView = new TableView();
        tm.tradesView.setOpaque(true);
        add(row1);

        for (JPanel row:rows){
            add(row);
        }
        add(tm.tradesView);
        frame.setContentPane(this);
        frame.pack();
        frame.setVisible(true);
    }
}

class TableView extends JPanel implements TableCellRenderer {

    private boolean DEBUG = false;
    DefaultTableModel model;
    JTable table;
    HashSet<Integer> color = new HashSet<Integer>();
    JScrollPane scrollPane;
    public TableView() {
        super(new GridLayout(1, 0));

        model = new DefaultTableModel();
        table = new JTable(model);
        model.setColumnIdentifiers(new Object[]{"TradeID", "Cpty", "Ticker", "Qty", "BS", "STATUS"});
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(1).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(40);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.setPreferredScrollableViewportSize(new Dimension(600, 400));
        table.setFillsViewportHeight(true);

        //Create the scroll pane and add the table to it.
        scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        add(scrollPane);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        return this;
    }

}