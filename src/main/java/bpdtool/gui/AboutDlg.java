package bpdtool.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;

public class AboutDlg extends JDialog
{
	private JPanel contentPane;
	private JButton m_btnOK;
	private JLabel m_lbVersion;
	private JLabel m_lbTitle;
	private JButton m_btnLink;

	public AboutDlg(JFrame parent)
	{
		super(parent);

		setContentPane(contentPane);
		setLocationRelativeTo(parent);
		setTitle("About " + MainFrame.APP_TITLE);

		m_lbTitle.setText(MainFrame.APP_TITLE);
		m_lbVersion.setText("Version " + MainFrame.APP_VERSION);
		setModal(true);
		getRootPane().setDefaultButton(m_btnOK);

		m_btnOK.addActionListener(new ActionListener()
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

		m_btnLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
		m_btnLink.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				gotoLink(m_btnLink.getText());
			}
		});
	}

	private void onCancel()
	{
		dispose();
	}

	private void gotoLink(String url)
	{
		if (Desktop.isDesktopSupported())
		{
			try
			{
				Desktop.getDesktop().browse(new URI(url));
				return;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		MainFrame.showMsgBox("Couldn't open a browser.", JOptionPane.ERROR_MESSAGE);
	}

	public static void doModal()
	{
		AboutDlg dlg = new AboutDlg(MainFrame.getInstance());
		dlg.pack();
		dlg.setVisible(true);
	}
}
