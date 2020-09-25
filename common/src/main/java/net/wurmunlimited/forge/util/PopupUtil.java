package net.wurmunlimited.forge.util;

import javax.swing.*;

public class PopupUtil {

    public static void messageBox(String message) {
        messageBox(message,JOptionPane.PLAIN_MESSAGE,0);
    }

    public static void errorMessage(String message) {
        messageBox(message,JOptionPane.ERROR_MESSAGE,1);
    }

    public static void messageBox(String message,int messageType,int exitStatus) {
        JOptionPane.showMessageDialog(null,message,"Wurm Unlimited Forge",messageType);
        System.exit(exitStatus);
    }

    public static boolean confirmBoxOkCancel(String question) {
        return confirmBox(question,JOptionPane.OK_CANCEL_OPTION);
    }

    public static boolean confirmBoxYesNo(String question) {
        return confirmBox(question,JOptionPane.YES_NO_OPTION);
    }

    public static boolean confirmBoxYesNoCancel(String question) {
        return confirmBox(question,JOptionPane.YES_NO_CANCEL_OPTION);
    }

    public static boolean confirmBox(String question,int optionType) {
        int result = JOptionPane.showConfirmDialog(null,question,"Wurm Unlimited Forge",optionType);
        return result==JOptionPane.OK_OPTION || result==JOptionPane.YES_OPTION;
    }
}
