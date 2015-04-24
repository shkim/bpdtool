package bpdtool.gui;

import bpdtool.Util;
import bpdtool.data.*;

import javax.swing.*;
import java.awt.event.*;

public class ItemCommonDlg extends JDialog
{
	private JPanel contentPane;
	private JTextField m_tfName;
	private JTextField m_tfComment;
	private JTextArea m_taDescript;
	private JButton m_btnCancel;
	private JButton m_btnOK;

	private ItemCommons m_data;
	private boolean m_dialogResult;
	public boolean m_isDescriptionChanged;

	public static void showInvalidVarNameBox(String name)
	{
		JOptionPane.showMessageDialog(MainFrame.getInstance(),
			"Invalid identifier: " + name,
			MainFrame.APP_TITLE, JOptionPane.ERROR_MESSAGE);
	}

	public static void showConflictVarNameBox(String name)
	{
		JOptionPane.showMessageDialog(MainFrame.getInstance(),
			String.format("Name '%s' is already used (conflict)", name),
			MainFrame.APP_TITLE, JOptionPane.ERROR_MESSAGE);
	}

	public ItemCommonDlg(ItemCommons data)
	{
		super(MainFrame.getInstance(), true);
		m_data = data;

		m_btnOK.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				onOK();
			}
		});

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		m_btnCancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});

		contentPane.registerKeyboardAction(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
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

		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(m_btnOK);

		setTitle(data.getDataTypeName() + " Property");
		m_tfName.setText(data.getName());
		m_tfComment.setText(data.getComment());
		m_taDescript.setText(data.getDescription());
		m_btnCancel.requestFocus();
	}

	private void onOK()
	{
		String name = m_tfName.getText().trim();

		if(!Util.isValidVarName(name))
		{
			ItemCommonDlg.showInvalidVarNameBox(name);
			return;
		}

		Protocol doc = MainFrame.getInstance().getDocument();

		boolean bConflict;
		if(m_data instanceof UserType)
			bConflict = doc.isTypeNameConflict(name, (UserType) m_data);
		else if(m_data instanceof Constant)
			bConflict = doc.isConstNameConflict(name, (Constant) m_data);
		else if(m_data instanceof PacketGroup)
			bConflict = false;  // group name can conflict
		else
			return;

		if(bConflict)
		{
			ItemCommonDlg.showConflictVarNameBox(name);
			return;
		}

		m_data.setName(name);
		m_data.setComment(m_tfComment.getText().trim());
		String desc = m_taDescript.getText().trim();
		if (!desc.equals(m_data.getDescription()))
		{
			m_isDescriptionChanged = true;
			m_data.setDescription(desc);
		}

		m_dialogResult = true;
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
