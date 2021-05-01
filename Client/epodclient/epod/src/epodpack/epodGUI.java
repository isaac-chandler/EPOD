package epodpack;

import epod.platform.JobScheduler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class epodGUI {
    private JButton startBtn;
    private JPanel panel1;
    private JTextArea EPODTextArea;
    private JTextArea explainArea;
    static JFrame frame = new JFrame("EPOD");

    public static void setupJ(){
        frame.setContentPane(new epodGUI().panel1 );
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
        frame.getContentPane().setBackground(Color.DARK_GRAY);



    }

    public boolean fullSend(){
        sendping sp = new sendping();
        locationInput li = new locationInput();
        sp.sendPing(li.readFromFile());
        return true;

    }

    public epodGUI() {
        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("testing");
                String url="https://www.google.com/maps";
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                    frame.dispose();
                    locationInput locin = new locationInput();
                    locin.showFrame(true);

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        setupJ();
        locationInput li = new locationInput();
        if(!li.firstRequest()){
            epodGUI eg = new epodGUI();
            new JobScheduler(eg::fullSend, 5000, true, true).start();

        }

    }
}
