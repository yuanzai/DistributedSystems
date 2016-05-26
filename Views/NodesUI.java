import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by junyuanlau on 25/5/16.
 */
class NodesUI extends JPanel {
    NodeView nodeView;
    public NodesUI(TradeManager tm ) {
        JFrame frame = new JFrame("nodes");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());


        JButton toggleNode = new JButton("Start/Stop Node");
        toggleNode.addActionListener(e -> {
            int i = nodeView.table.getSelectedRow();

            if (i >= 0) {
                ArrayList<String> keys = new ArrayList<String>(tm.exchangeAddresses.keySet());
                String name = keys.get(i);
                boolean isRunning = false;
                if (tm.exchanges.get(name) != null) {
                    if (tm.exchanges.get(name).isRunning) {
                        isRunning = true;
                    }
                }
                if (isRunning) {
                    tm.terminateNode(name);
                } else {
                    tm.restartNode(name);
                }
            }
        });

        JButton toggleRogue = new JButton("Toggle Rogue");
        toggleRogue.addActionListener(e -> {
            int i = nodeView.table.getSelectedRow();

            if (i >= 0) {
                ArrayList<String> keys = new ArrayList<String>(tm.exchangeAddresses.keySet());
                String name = keys.get(i);
                if (tm.exchanges.get(name) != null) {
                    if(tm.exchanges.get(name).paxos.isRogue) {
                        tm.exchanges.get(name).paxos.isRogue = false;
                    } else {
                        tm.exchanges.get(name).paxos.isRogue = true;
                    }

                    tm.nodesUI.refresh();
                }
            }
        });

        buttons.add(toggleNode);
        buttons.add(toggleRogue);
        panel.add(buttons);

        nodeView = new NodeView(tm);
        panel.add(nodeView);
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
    }
    public void refresh(){
        nodeView.refresh();
    }
}
class NodeView extends JPanel implements TableCellRenderer {

    DefaultTableModel model;
    JTable table;
    TradeManager tm;

    public NodeView(TradeManager tm) {
        super(new GridLayout(1, 0));
        this.tm = tm;
        model = new DefaultTableModel();
        table = new JTable(model);
        model.setColumnIdentifiers(new Object[]{"Name", "Region", "Port", "SuperNode", "Status", "Rogue"});
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(40);
        table.getColumnModel().getColumn(3).setPreferredWidth(40);
        table.getColumnModel().getColumn(4).setPreferredWidth(30);
        table.getColumnModel().getColumn(5).setPreferredWidth(30);

        table.setPreferredScrollableViewportSize(new Dimension(400, 300));
        table.setFillsViewportHeight(true);

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        add(scrollPane);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }

    public void refresh(){
        int i= 0;
        model.setNumRows(tm.exchanges.size());

        for (Map.Entry<String, Address> entry: tm.exchangeAddresses.entrySet()){
            model.setValueAt(entry.getKey(),i,0);
            model.setValueAt(entry.getValue().region,i,1);
            model.setValueAt(entry.getValue().port,i,2);
            model.setValueAt(entry.getKey(),i,3);
            if (tm.exchanges.get(entry.getKey()).supernode != null)
                model.setValueAt(tm.exchanges.get(entry.getKey()).supernode.name,i,3);
            model.setValueAt("FALSE",i,4);
            model.setValueAt("NA",i,5);
            if (tm.exchanges.get(entry.getKey()) != null) {
                if (tm.exchanges.get(entry.getKey()).isRunning) {
                    model.setValueAt("TRUE",i,4);
                }
                model.setValueAt(tm.exchanges.get(entry.getKey()).paxos.isRogue,i,5);
            }


            i++;
        }
    }
}