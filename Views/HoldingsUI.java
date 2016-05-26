import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by junyuanlau on 25/5/16.
 */
class HoldingsUI extends JPanel {
    DefaultTableModel model;
    TradeManager tm;
    Object[] cols;

    public HoldingsUI(TradeManager tm){
        super(new GridLayout(1, 0));
        this.tm = tm;
        JFrame frame = new JFrame("Holdings");
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setOpaque(true);
        model = new DefaultTableModel();
        JTable table = new JTable(model);
        cols = new Object[]{"Exchange", "Ticker", "Cpty", "Qty"};
        model.setColumnIdentifiers(cols);
        table.getColumnModel().getColumn(0).setPreferredWidth(10);
        table.getColumnModel().getColumn(1).setPreferredWidth(10);
        table.getColumnModel().getColumn(2).setPreferredWidth(10);
        table.getColumnModel().getColumn(3).setPreferredWidth(5);
        table.setPreferredScrollableViewportSize(new Dimension(400, 300));
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);

        add(scrollPane);
        frame.setContentPane(this);
        frame.pack();
        frame.setVisible(true);
    }

    public void refresh(){

        int rows = 0;
        for (ExchangeNode node : tm.exchanges.values()) {
            for (HashMap<String, Integer> map : node.holdings.values()){
                rows += map.size();
            }
        }
        Object[][] data = new Object[rows][4];
        int i = 0;
        for (ExchangeNode node : tm.exchanges.values()) {
            for (Map.Entry<String, HashMap<String, Integer>> entry : node.holdings.entrySet()){
                for (Map.Entry<String, Integer> subentry : entry.getValue().entrySet()) {
                    data[i][0] = node.name;
                    data[i][1] = subentry.getKey();
                    data[i][2] = entry.getKey();
                    data[i][3] = subentry.getValue();
                    i++;

                }
            }
        }
        model.setDataVector(data,cols);

    }
}
