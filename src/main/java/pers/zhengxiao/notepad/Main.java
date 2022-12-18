package pers.zhengxiao.notepad;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException |
                 InstantiationException e) {
            // do nothing
        }
        MainWindow mainWindow = new MainWindow();
        mainWindow.setVisible(true);
    }
}
