package pers.zhengxiao.notepad;

import com.sigpwned.chardet4j.Chardet;
import org.drjekyll.fontchooser.FontDialog;
import pers.zhengxiao.notepad.widgets.JStatusBar;
import pers.zhengxiao.notepad.widgets.JStatusBarItem;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class MainWindow extends JFrame {
    // components
    JTextArea textArea;
    JStatusBar statusBar;
    JStatusBarItem sbItemPosition, sbItemEncoding, sbItemEncryption;
    JPopupMenu popupMenu;
    UndoManager undoManager = new UndoManager();

    // state fields
    File currentFile = null;
    boolean modified = false, encryptionMode = true, newlyOpenedDisableUndo=false;
    Charset currentCharset = StandardCharsets.UTF_8;
    String currentEncryptionKey = null;

    MainWindow() {
        setupFrame();
        setupTextArea();
        setupStatusBar();
        setupMenu(); // last to execute!

        setEncryptionMode(true);
        onNewFile(null); // set modified/currentFile/charset
    }

    private void setupFrame() {
        setTitle("记事本");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/img/pencil.ico"))).getImage());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (safeSave()) {
                    System.exit(0);
                }
            }
        });
    }

    private void setupMenu() {
        var menuBar = new JMenuBar();

        var menuFile = new JMenu("文件(F)");
        menuFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menuFile);

        var menuEdit = new JMenu("编辑(E)");
        menuEdit.setMnemonic(KeyEvent.VK_E);
        menuBar.add(menuEdit);

        var menuFormat = new JMenu("格式(O)");
        menuFormat.setMnemonic(KeyEvent.VK_O);
        menuBar.add(menuFormat);

        var menuView = new JMenu("查看(V)");
        menuView.setMnemonic(KeyEvent.VK_V);
        menuBar.add(menuView);

        var menuEncoding = new JMenu("编码(N)");
        menuEncoding.setMnemonic(KeyEvent.VK_N);
        menuBar.add(menuEncoding);

        var menuAbout = new JMenu("关于(A)");
        menuAbout.setMnemonic(KeyEvent.VK_A);
        menuAbout.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(null,
                        "Notepad with AES encryption support\nA HITWH curriculum design\nby lizhengxiao", "About",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        menuBar.add(menuAbout);

        setupMenuFile(menuFile);
        setupMenuEdit(menuEdit);
        setupMenuFormat(menuFormat);
        setupMenuView(menuView);
        setupMenuEncoding(menuEncoding);

        setJMenuBar(menuBar);
        setupPopupMenu();
    }

    private void setupMenuFile(JMenu menuFile) {
        var menuItem = new JMenuItem("新建(N)",
                KeyEvent.VK_N);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(this::onNewFile);
        menuFile.add(menuItem);

        menuItem = new JMenuItem("打开(O)",
                KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(e -> doOpenFile(null, null, true));
        menuFile.add(menuItem);

        menuItem = new JMenuItem("保存(S)",
                KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener((e) -> doSave(currentFile, false));
        menuFile.add(menuItem);

        menuItem = new JMenuItem("另存为(A)",
                KeyEvent.VK_A);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        menuItem.addActionListener(e -> doSave(null, true));
        menuFile.add(menuItem);

        menuFile.addSeparator();

        menuItem = new JCheckBoxMenuItem("加密模式(E)");
        menuItem.setSelected(encryptionMode);
        menuItem.setMnemonic(KeyEvent.VK_E);
        menuItem.addActionListener(e -> {
            if (e.getSource() instanceof JCheckBoxMenuItem item) {
                setEncryptionMode(item.isSelected());
            }
        });
        menuFile.add(menuItem);

        menuFile.addSeparator();

        menuItem = new JMenuItem("退出(X)",
                KeyEvent.VK_X);
        menuItem.addActionListener(e -> {
            if (safeSave()) {
                System.exit(0);
            }
        });
        menuFile.add(menuItem);
    }
    private void setupMenuEdit(JMenu menuEdit){
        var menuItem = new JMenuItem("撤销(U)",
                KeyEvent.VK_U);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(e -> undoManager.undo());
        menuEdit.add(menuItem);

        menuEdit.addSeparator();

        menuItem = new JMenuItem("剪切(T)",
                KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(e -> textArea.cut());
        menuEdit.add(menuItem);

        menuItem = new JMenuItem("复制(C)",
                KeyEvent.VK_C);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener((e) -> textArea.copy());
        menuEdit.add(menuItem);

        menuItem = new JMenuItem("粘贴(P)",
                KeyEvent.VK_P);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(e -> textArea.paste());
        menuEdit.add(menuItem);

        menuEdit.addSeparator();

        menuItem = new JMenuItem("全选(A)",
                KeyEvent.VK_A);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(e -> textArea.selectAll());
        menuEdit.add(menuItem);
    }
    private void setupPopupMenu(){
        popupMenu=new JPopupMenu();
        var menuItem = new JMenuItem("撤销(U)",
                KeyEvent.VK_U);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(e -> undoManager.undo());
        popupMenu.add(menuItem);

        popupMenu.addSeparator();

        menuItem = new JMenuItem("剪切(T)",
                KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(e -> textArea.cut());
        popupMenu.add(menuItem);

        menuItem = new JMenuItem("复制(C)",
                KeyEvent.VK_C);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener((e) -> textArea.copy());
        popupMenu.add(menuItem);

        menuItem = new JMenuItem("粘贴(P)",
                KeyEvent.VK_P);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(e -> textArea.paste());
        popupMenu.add(menuItem);

        popupMenu.addSeparator();

        menuItem = new JMenuItem("全选(A)",
                KeyEvent.VK_A);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(e -> textArea.selectAll());
        popupMenu.add(menuItem);

        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                process(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                process(e);
            }

            private void process(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(),
                            e.getX(), e.getY());
                }
            }
        });
    }
    private void setupMenuFormat(JMenu menuFormat) {
        JMenuItem menuItem;
        menuItem = new JCheckBoxMenuItem("自动换行(W)");
        menuItem.setSelected(textArea.getLineWrap());
        menuItem.setMnemonic(KeyEvent.VK_W);
        menuItem.addActionListener(e -> {
            if (e.getSource() instanceof JCheckBoxMenuItem item) {
                textArea.setLineWrap(item.isSelected());
            }
        });
        menuFormat.add(menuItem);

        menuItem = new JMenuItem("字体(F)...",
                KeyEvent.VK_F);
        menuItem.addActionListener(e -> {
            FontDialog dialog = new FontDialog(this, "选择字体", true);
            dialog.setSelectedFont(textArea.getFont());
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            textArea.setFont(dialog.getSelectedFont());
        });
        menuFormat.add(menuItem);
    }

    private void setupMenuView(JMenu menuView) {
        JMenuItem menuItem;
        menuItem = new JCheckBoxMenuItem("状态栏(S)");
        menuItem.setSelected(statusBar.isVisible());
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.addActionListener(e -> {
            if (e.getSource() instanceof JCheckBoxMenuItem item) {
                statusBar.setVisible(item.isSelected());
            }
        });
        menuView.add(menuItem);
    }

    private void setupMenuEncoding(JMenu menuEncoding) {
        JMenu menuOpenAs = new JMenu("以编码方式重新打开");
        menuEncoding.add(menuOpenAs);
        JMenu menuSaveAs = new JMenu("以编码方式保存");
        menuEncoding.add(menuSaveAs);

        String[] encodings = {"UTF-8", "UTF-16LE", "UTF-16BE", "GBK", "GB2312", "GB18030"};

        JMenuItem menuItem;
        for (String encoding : encodings) {
            if (!Charset.isSupported(encoding))
                continue;
            menuItem = new JMenuItem(encoding);
            menuItem.addActionListener(this::onEncodingOpenAs);
            menuOpenAs.add(menuItem);

            menuItem = new JMenuItem(encoding);
            menuItem.addActionListener(this::onEncodingSaveAs);
            menuSaveAs.add(menuItem);
        }
    }

    private void onNewFile(ActionEvent e) {
        if (safeSave()) {
            newlyOpenedDisableUndo=true;
            textArea.setText("");
            undoManager.discardAllEdits();
            setCurrentFile(null);
            setModified(false);
            setCharset(StandardCharsets.UTF_8);
            currentEncryptionKey = null;
        }
    }

    private void onEncodingOpenAs(ActionEvent e) {
        if (currentFile == null)
            return;
        if (e.getSource() instanceof JMenuItem menuItem) {
            String encoding = menuItem.getText();
            doOpenFile(currentFile, Charset.forName(encoding), false);
        }
    }

    private void onEncodingSaveAs(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem menuItem) {
            String encoding = menuItem.getText();
            setCharset(Charset.forName(encoding));
            setModified(true);
        }
    }

    private void setupTextArea() {
        textArea = new JTextArea();
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.addCaretListener((e -> {
            if (e.getSource() instanceof JTextArea area) {
                int row = 0, column = 0;
                try {
                    row = area.getLineOfOffset(area.getCaretPosition());
                    column = area.getCaretPosition() - area.getLineStartOffset(row);
                } catch (BadLocationException ex) {
                    // do nothing
                }
                sbItemPosition.setText("第" + (row + 1) + "行，第" + (column + 1) + "列");
            }
        }));
        textArea.getDocument().addUndoableEditListener(e -> {
            if(newlyOpenedDisableUndo)
                newlyOpenedDisableUndo=false;
            else
                undoManager.undoableEditHappened(e);
        });
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setModified(true);
            }
        });
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    private void setupStatusBar() {
        statusBar = new JStatusBar();

        sbItemPosition = new JStatusBarItem(120);
        sbItemPosition.setText("第1行，第1列");
        statusBar.add(sbItemPosition);

        sbItemEncoding = new JStatusBarItem();
        sbItemEncoding.setText("UTF-8");
        statusBar.add(sbItemEncoding);

        sbItemEncryption = new JStatusBarItem();
        sbItemEncryption.setText("加密模式");
        sbItemEncryption.setFont(sbItemEncryption.getFont().deriveFont(Font.BOLD));
        statusBar.add(sbItemEncryption);

        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    private void setCurrentFile(File file) {
        currentFile = file;
        updateInfo();
    }

    private void setModified(boolean modified) {
        if(this.modified != modified){
            this.modified = modified;
            updateInfo();
        }
    }

    private void setEncryptionMode(boolean enabled) {
        if(encryptionMode!=enabled){
            encryptionMode = enabled;
            sbItemEncryption.setVisible(encryptionMode);
            if (!encryptionMode)
                currentEncryptionKey = null;
            setModified(true);
        }
    }

    private void setCharset(Charset charset) {
        currentCharset = charset;
        updateInfo();
    }

    private void updateInfo() {
        String pre = modified ? "*" : "";
        if (currentFile != null)
            setTitle(pre + currentFile.getName() + " - 记事本");
        else
            setTitle(pre + "无标题 - 记事本");
        sbItemEncoding.setText(currentCharset.displayName());
    }

    /**
     * 保存文件操作
     *
     * @param file 若file非空，则保存到file；否则弹出文件选择对话框，再按选择保存。
     * @return 是否保存成功
     */
    private boolean doSave(File file, boolean newPasswordNeeded) {
        if (file == null) {
            JFileChooser fc = new JFileChooser();
            if (currentFile != null)
                fc.setSelectedFile(currentFile);
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
            } else
                return false;
        }
        try (FileOutputStream fs = new FileOutputStream(file)) {
            byte[] buffer = textArea.getText().getBytes(currentCharset);
            // 加密模式逻辑
            String result = null;
            if (encryptionMode) {
                if (currentEncryptionKey == null || newPasswordNeeded) {
                    result = JOptionPane.showInputDialog(this, "请输入密码",
                            "输入密码", JOptionPane.QUESTION_MESSAGE);
                    if (result == null)
                        return false;
                } else
                    result = currentEncryptionKey;
                buffer = AESUtils.encrypt(buffer, result, null);
            }
            fs.write(buffer);
            if (encryptionMode)
                currentEncryptionKey = result;
            setCurrentFile(file);
            setModified(false);
            return true;
        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException exception) {
            JOptionPane.showMessageDialog(this, exception.getLocalizedMessage(), "保存失败", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * 若未保存更改先保存，然后打开文件
     *
     * @param charset           若{@code file}为{@code null}，则弹出文件选择对话框选择文件。
     *                          若{@code charset}为{@code null}，则自动检测编码。
     *                          若两参数都非{@code null}，则静默打开文件。
     * @param newPasswordNeeded （加密模式下）是否强制用户输入密码
     */
    private void doOpenFile(File file, Charset charset, boolean newPasswordNeeded) {
        if (!safeSave())
            return;
        if (file == null) {
            JFileChooser fc = new JFileChooser();
            fc.addChoosableFileFilter(new FileNameExtensionFilter("文本文件(*.txt)", "txt"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                file = fc.getSelectedFile();
            else
                return;
        }
        try (FileInputStream fs = new FileInputStream(file)) {
            byte[] buffer = fs.readAllBytes();
            // 加密模式逻辑
            if (encryptionMode) {
                String result;
                if (newPasswordNeeded) {
                    result = JOptionPane.showInputDialog(this, "请输入密码",
                            "输入密码", JOptionPane.QUESTION_MESSAGE);
                    if (result == null)
                        return;
                } else
                    result = currentEncryptionKey;
                buffer = AESUtils.decrypt(buffer, result, null);

                currentEncryptionKey = result;
            }
            setCharset(charset == null ? Chardet.detectCharset(buffer).orElse(StandardCharsets.UTF_8) : charset);
            newlyOpenedDisableUndo=true;
            textArea.setText(new String(buffer, currentCharset));
            undoManager.discardAllEdits();
            setCurrentFile(file);
            setModified(false);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, exception.getLocalizedMessage(), "打开失败", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalBlockSizeException e) {
            JOptionPane.showMessageDialog(this, "可能选择了非加密的文件。\n详细信息：" + e.getLocalizedMessage(), "解密时发生错误", JOptionPane.ERROR_MESSAGE);
        } catch (BadPaddingException e) {
            JOptionPane.showMessageDialog(this, "密码错误。\n详细信息：" + e.getLocalizedMessage(), "解密时发生错误", JOptionPane.ERROR_MESSAGE);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException |
                 NoSuchAlgorithmException | InvalidKeyException e) {
            JOptionPane.showMessageDialog(this, "意外错误：" + e.getLocalizedMessage(), "解密时发生错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 在进行其他操作前，确保已修改的内容得到保存
     *
     * @return 是否已处理好未保存的数据（或不需要保存）
     */
    private boolean safeSave() {
        if (modified) {
            int choice = JOptionPane.showConfirmDialog(this, "是否要保存当前更改？", "记事本", JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION)
                return false;
            if (choice == JOptionPane.YES_OPTION)
                return doSave(currentFile, false);
        }
        return true;
    }
}