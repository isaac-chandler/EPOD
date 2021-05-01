package epodpack;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class locInError {
    private JTextArea EPODTextArea1;
    private JTextArea errorTextArea;
    private JButton tryAgainButton;
    private JPanel panel4;
    static JFrame frame = new JFrame("EPOD");

    public locInError() {
        tryAgainButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locationInput locin = new locationInput();
                frame.dispose();
                locin.showFrame(true);

            }
        });
    }

    public static void showFrame(boolean b){
        frame.setContentPane(new locInError().panel4 );
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
        frame.getContentPane().setBackground(Color.DARK_GRAY);

    }
}
