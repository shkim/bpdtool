package bpdtool.gui;

import bpdtool.data.PacketGroup;

import javax.swing.*;
import java.awt.event.*;

public class PacketGroupDlg extends JDialog
{
    private JPanel contentPane;
    private JButton m_btnOK;
    private JButton m_btnCancel;
    private JTextField m_tfName;
    private JTextField m_tfComment;
    private JTextArea m_taDescript;
    private JTextField m_tfStartID;

    private boolean m_dialogResult;
    private boolean m_isDescriptionChanged;
    private PacketGroup m_data;

    public PacketGroupDlg(PacketGroup grp)
    {
        super(MainFrame.getInstance(), "PacketGroup Property");

        m_data = grp;

        setContentPane(contentPane);
        setLocationRelativeTo(MainFrame.getInstance());
        setModal(true);
        getRootPane().setDefaultButton(m_btnOK);

        m_btnOK.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                onOK();
            }
        });

        m_btnCancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


        m_tfName.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent focusEvent)
            {
                super.focusGained(focusEvent);
                m_tfName.selectAll();
            }
        });

        m_tfName.setText(m_data.getName());
        m_tfComment.setText(m_data.getComment());
        m_taDescript.setText(m_data.getDescription());
        m_tfStartID.setText(m_data.getStartId());
    }

    private void onOK()
    {
        m_data.setName(m_tfName.getText().trim());
        m_data.setComment(m_tfComment.getText().trim());

        String desc = m_taDescript.getText().trim();
        if(!desc.equals(m_data.getDescription()))
        {
            m_isDescriptionChanged = true;
            m_data.setDescription(desc);
        }

        m_dialogResult = true;
        dispose();
    }

    private void onCancel()
    {
        m_dialogResult = false;
        dispose();
    }

    public boolean isDescriptionChanged()
    {
        return m_isDescriptionChanged;
    }

    public boolean doModal()
    {
        pack();
        setVisible(true);

        return m_dialogResult;
    }
}
