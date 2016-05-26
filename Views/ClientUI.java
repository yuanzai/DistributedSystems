import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/**
 * Created by junyuanlau on 25/5/16.
 */
public class ClientUI extends JPanel {
    DefaultTableModel model;
    TradeManager tm;
    Object[] cols;

    public ClientUI(TradeManager tm){
        super(new GridLayout(1, 0));
        this.tm = tm;
        JFrame frame = new JFrame("Client");
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setOpaque(true);
        model = new DefaultTableModel();
        JTable table = new JTable(model);
        this.cols = new Object[]{"Cpty","Cash","Country"};
        model.setColumnIdentifiers(cols);
        table.getColumnModel().getColumn(0).setPreferredWidth(10);
        table.getColumnModel().getColumn(1).setPreferredWidth(10);
        table.getColumnModel().getColumn(2).setPreferredWidth(10);
        table.setPreferredScrollableViewportSize(new Dimension(200, 200));
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
        frame.setContentPane(this);
        frame.pack();
        frame.setVisible(true);
    }

    public void refresh(){

        int rows = tm.clientList.size();

        Object[][] data = new Object[rows][3];
        int i = 0;
        for (Map.Entry<String, Double> entry : tm.engine.dataCube.cashBalance.entrySet()){
            data[i][0] = entry.getKey();
            data[i][1] = entry.getValue();
            data[i][2] = tm.clientCountry.get(entry.getKey());

            i++;
        }

        model.setDataVector(data,cols);

    }
}

