package net.wurmunlimited.forge;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;


public class AutoInstaller {

    private static final Logger logger = Logger.getLogger(AutoInstaller.class.getName());

    public static void main(final String[] args) {
        ServerConnection.getInstance().getAvailableMods(args[1]);
        if(ServerConnection.getInstance().install()) {
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(2,1));
            panel.setBounds(0,0,468,120);
            JLabel l = new JLabel("Your client mods have been updated, please restart your Wurm Unlimited client.");
            l.setVerticalTextPosition(JButton.TOP);
            l.setHorizontalTextPosition(JButton.CENTER);
            panel.add(l);
            JOptionPane.showMessageDialog(null,panel,"Client Mod Update",JOptionPane.PLAIN_MESSAGE);
        }
    }
}
