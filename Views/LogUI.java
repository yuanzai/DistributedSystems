import javax.swing.*;
import javax.swing.plaf.DimensionUIResource;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by junyuanlau on 25/5/16.
 */
public class LogUI extends JPanel {
    TradeManager tm;
    JTextArea textArea;
    LinkedList<String> text = new LinkedList<String>();
    public LogUI(TradeManager tm){
        this.tm = tm;
        setPreferredSize(new Dimension(500, 300));
        textArea = new JTextArea();
        add(textArea);
        JFrame frame = new JFrame("Log");
        setOpaque(true);
        frame.setContentPane(this);
        frame.pack();
        frame.setVisible(true);
    }

    public void update(String s){
        if (text.size() >= 20){
            text.removeFirst();
        }
        text.addLast(s);
        StringBuilder sb = new StringBuilder();
        for (String t : text){
            sb.append(t);
        }
        String result = new String(sb);
        textArea.setText(result);
    }

}
