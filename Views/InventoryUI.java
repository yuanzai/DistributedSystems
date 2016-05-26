import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/**
 * Created by junyuanlau on 25/5/16.
 */
class InventoryUI extends JPanel {
    DefaultTableModel model;
    TradeManager tm;
    Object[] cols;

    public InventoryUI(TradeManager tm){
        super(new GridLayout(1, 0));
        this.tm = tm;
        JFrame frame = new JFrame("Inventory");
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setOpaque(true);
        model = new DefaultTableModel();
        JTable table = new JTable(model);
        this.cols = new Object[]{"Exchange", "Ticker", "Qty"};
        model.setColumnIdentifiers(cols);
        table.getColumnModel().getColumn(0).setPreferredWidth(10);
        table.getColumnModel().getColumn(1).setPreferredWidth(10);
        table.getColumnModel().getColumn(2).setPreferredWidth(5);
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
            rows += node.inventory.size();
        }
        Object[][] data = new Object[rows][3];
        int i = 0;
        for (ExchangeNode node : tm.exchanges.values()) {
            for (Map.Entry<String, Integer> entry : node.inventory.entrySet()){
                data[i][0] = node.name;
                data[i][1] = entry.getKey();
                data[i][2] = entry.getValue();
                i++;
            }
        }
        model.setDataVector(data,cols);

    }
}
