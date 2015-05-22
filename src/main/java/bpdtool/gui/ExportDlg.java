package bpdtool.gui;

import bpdtool.Util;
import bpdtool.codegen.BufferedTabWriter;
import bpdtool.codegen.ErrorLogger;
import bpdtool.codegen.Exporter;
import bpdtool.codegen.ITextWriter;
import bpdtool.data.Protocol;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

public class ExportDlg extends JDialog
	implements ActionListener, ITextWriter
{
	private JPanel contentPane;
	private JButton m_btnExport;
	private JButton m_btnCancel;
	private JTextArea m_taOutput;
	private JButton m_btnPreview;
	private JTabbedPane m_tabs;
	private Exporter m_exporter;

	public ExportDlg()
	{
		super(MainFrame.getInstance());

		setContentPane(contentPane);
		setTitle("Protocol serialization code generation");
		setModal(true);
		getRootPane().setDefaultButton(m_btnExport);

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

		m_btnExport.addActionListener(this);
		m_btnCancel.addActionListener(this);
		m_btnPreview.addActionListener(this);

		// initial preparation
		Protocol doc = MainFrame.getInstance().getDocument();
		m_exporter = new Exporter(doc);
		ErrorLogger logger = new ErrorLogger();
		if (m_exporter.prepare(logger))
		{
			writeln("Ready to export...");
		}
		else
		{
			m_taOutput.setForeground(Color.red);
			m_taOutput.append(logger.getLoggedString());
			disableMoreAction();
		}
	}

	private void onCancel()
	{
		dispose();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_btnPreview)
		{
			onPreview();
		}
		else if (e.getSource() == m_btnExport)
		{
			onExport();
		}
		else if (e.getSource() == m_btnCancel)
		{
			onCancel();
			return;
		}

		m_btnCancel.setText("Close");
	}

	private void disableMoreAction()
	{
		m_btnPreview.setEnabled(false);
		m_btnExport.setEnabled(false);
	}

	private void onPreview()
	{
		disableMoreAction();
		m_exporter.preview(this);
	}

	private void onExport()
	{
		disableMoreAction();
		m_exporter.export(this);
	}

	@Override
	public void write(String format, Object... args)
	{
		m_taOutput.append(Util.stringFormat(format, args));
	}

	@Override
	public void writeln(String format, Object... args)
	{
		write(format, args);
		m_taOutput.append("\n");
	}

	public void addPreviewTab(String filename, String result)
	{
		JTextPane textPane = new JTextPane()
		{
			public boolean getScrollableTracksViewportWidth()
			{
				return getUI().getPreferredSize(this).width <= getParent().getSize().width;
			}
		};

		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 14);
		textPane.setFont(font);

		int w = textPane.getFontMetrics(font).charWidth(' ');
		TabStop[] tabStops = new TabStop[16];
		for (int i=0; i<tabStops.length; i++)
			tabStops[i] = new TabStop(w * i * BufferedTabWriter.TABSTOP);

		MutableAttributeSet attrs = new SimpleAttributeSet();
		StyleConstants.setTabSet(attrs, new TabSet(tabStops));
		textPane.setParagraphAttributes(attrs, false);
		textPane.setText(result);

		JScrollPane scr = new JScrollPane();
		scr.setViewportView(textPane);

		m_tabs.addTab(filename, scr);
	}

	public static void doModal()
	{
		ExportDlg dialog = new ExportDlg();
		dialog.setSize(640,480);
		dialog.setVisible(true);
	}

}
