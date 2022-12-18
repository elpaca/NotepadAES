package pers.zhengxiao.notepad.widgets;

import javax.swing.*;
import java.awt.*;

public class JStatusBarItem extends JLabel {
    public JStatusBarItem() {
        this(80);
    }

    public JStatusBarItem(int width) {
        setPreferredSize(new Dimension(width, JStatusBar.STATUSBAR_HEIGHT));
        setHorizontalAlignment(LEFT);
        setVerticalAlignment(CENTER);
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
    }
}
