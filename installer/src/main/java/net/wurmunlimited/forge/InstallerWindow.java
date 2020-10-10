package net.wurmunlimited.forge;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static net.wurmunlimited.forge.interfaces.ForgeConstants.FORGE_BASE_URL;

public class InstallerWindow extends WindowAdapter implements WindowListener {

    JFrame frame;
    JTextArea textArea;

    public InstallerWindow() {
        frame = new JFrame("Wurm Unlimited Forge");
        frame.setSize(960,700);
        frame.setLocationRelativeTo(null);

        JLabel label = new JLabel();
        label.setText("<html><div style=\"text-align:center;margin-bottom:10px;\">"+
                      "<h1>Wurm Unlimited Forge</h1>"+
                      "For more information visit:<br>"+
                      "<a href=\""+FORGE_BASE_URL+"\">"+FORGE_BASE_URL+"</a><br>&nbsp;<br></div>"+
                      "<div style=\"margin-bottom:10px;\">Based on <i>Ago's Client Mod Launcher</i>,<br>"+
                      "by Alexander Gottwald, a.k.a. Ago</div>"+
                      "<div style=\"margin-bottom:10px;\"><i>WU Forge</i> is developed and maintained<br>"+
                      "by Per L&ouml;wgren, a.k.a. Kenabil</div>"+
                      "</html>");
        label.setVerticalAlignment(SwingConstants.TOP);
        label.setHorizontalAlignment(SwingConstants.CENTER);
//        label.setOpaque(true);
//        label.setBackground(new Color(0.8f,1.0f,0.7f));

        JLabel image;
        try {
            BufferedImage bufimg = ImageIO.read(this.getClass().getResource("/images/forge.png"));
            image = new JLabel(new ImageIcon(bufimg));
        } catch(IOException e) {
            image = new JLabel();
        }
//        image.setOpaque(true);
//        image.setBackground(new Color(0.3f,0.2f,0.1f));

        textArea=new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setMargin(new Insets(10,10,10,10));

        GridLayout vlayout = new GridLayout(2,1);
        JPanel panel = new JPanel();
        panel.setLayout(vlayout);
        panel.add(label);
        panel.add(image);

        GridLayout hlayout = new GridLayout(1,2);
        frame.getContentPane().setLayout(hlayout);
        frame.add(panel);
        frame.add(new JScrollPane(textArea));
        frame.setVisible(true);
        frame.setResizable(false);

        frame.addWindowListener(this);
    }

    @Override
    public synchronized void windowClosed(WindowEvent event) {
        System.exit(0);
    }

    @Override
    public synchronized void windowClosing(WindowEvent event) {
        close();
    }

    public void close() {
        frame.setVisible(false);
        frame.dispose();
    }

    public void log(String text) {
        textArea.append(text+"\n");
    }
}
