package net.wurmunlimited.forge;

import net.wurmunlimited.forge.config.ForgeConfig;
import net.wurmunlimited.forge.util.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;


public class Updater {

    private static final Logger logger = Logger.getLogger(Updater.class.getName());

    public static void main(final String[] args) {
        Updater updater = new Updater();
    }

    private Updater() {
        Path baseDir = Paths.get("").getParent();
        Properties properties = FileUtil.loadProperties("forge.properties");
        ForgeConfig.init(baseDir,properties);
        if(VersionHandler.getInstance().install()) {
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
