package pers.zhengxiao.notepad.widgets;

import javax.swing.*;
import java.awt.*;

public class JStatusBar extends JPanel {
    public static final int STATUSBAR_HEIGHT = 22;

    public JStatusBar() {
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        setPreferredSize(new Dimension(400, STATUSBAR_HEIGHT));
        setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    }

}
