package bpdtool.gui;

import bpdtool.Util;
import bpdtool.data.PacketStage;
import bpdtool.data.Protocol;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class EditStageDlg extends JDialog
	implements TableModel
{
	private JPanel contentPane;
	private JButton m_btnOK;
	private JButton m_btnCancel;
	private JTable m_tblStages;

	private JPopupMenu m_popupMenu;
	private JMenuItem m_miAdd;
	private JMenuItem m_miMoveDown;
	private JMenuItem m_miMoveUp;
	private JMenuItem m_miRemove;
	private JSeparator m_separator;

	private ArrayList<PacketStage> m_stages;
	private int m_curSelRow = -1;
	private boolean m_dialogResult;

	public EditStageDlg(Frame owner)
	{
		super(owner, true);

		setContentPane(contentPane);
		setLocationRelativeTo(owner);
		setTitle("Packet Dispatch Stages");
		setModal(true);
		getRootPane().setDefaultButton(m_btnOK);

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

		m_tblStages.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent evt)
			{
				onTableStageMouseClicked(evt, true);
			}
		});

		m_tblStages.getParent().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent evt)
			{
				onTableStageMouseClicked(evt, false);
			}
		});

		setupMenu();

		m_stages = new ArrayList<PacketStage>();

		Protocol doc = MainFrame.getInstance().getDocument();
		for(PacketStage ps : doc.getPacketStages())
		{
			m_stages.add(new PacketStage(ps));
		}

		m_tblStages.setModel(this);
		m_tblStages.getColumnModel().getColumn(0).setPreferredWidth(80);
		m_tblStages.getColumnModel().getColumn(1).setPreferredWidth(120);
		m_tblStages.getColumnModel().getColumn(2).setPreferredWidth(360);

		DefaultTableCellRenderer dtcr  = new DefaultTableCellRenderer();
		dtcr.setHorizontalAlignment(SwingConstants.CENTER);
		m_tblStages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tblStages.setDefaultRenderer(m_tblStages.getColumnClass(0), dtcr);
	}

	private void setupMenu()
	{
		m_popupMenu = new JPopupMenu();

		m_miMoveUp = new JMenuItem("Move Up", 'u');
		m_miMoveUp.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				changeOrder(-1);
			}
		});
		m_popupMenu.add(m_miMoveUp);

		m_miMoveDown = new JMenuItem("Move Down", 'd');
		m_miMoveDown.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				changeOrder(1);
			}
		});
		m_popupMenu.add(m_miMoveDown);

		m_separator = new JSeparator();
		m_popupMenu.add(m_separator);

		m_miAdd = new JMenuItem("Add", 'a');
		m_miAdd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				PacketStage pks = new PacketStage(m_stages.size(), "NewAbbr", "NewStageName");
				m_stages.add(pks);
				rearrangeIndices();
			}
		});
		m_popupMenu.add(m_miAdd);

		m_miRemove = new JMenuItem("Remove", 'r');
		m_miRemove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				if (m_stages.size() == 1)
				{
					JOptionPane.showMessageDialog(EditStageDlg.this, "At least, one stage is required.",
						MainFrame.APP_TITLE, JOptionPane.ERROR_MESSAGE);
					return;
				}

				m_stages.remove(m_curSelRow);
				rearrangeIndices();
			}
		});
		m_popupMenu.add(m_miRemove);
	}

	private void onTableStageMouseClicked(MouseEvent evt, boolean isInTable)
	{
		if (Util.isRightMouseButton(evt))
		{
			m_curSelRow = m_tblStages.rowAtPoint(evt.getPoint());
			if (m_curSelRow < 0)
				isInTable = false;

			if(isInTable && m_stages.size() > 1)
			{
				m_miMoveUp.setVisible(m_curSelRow > 0);
				m_miMoveDown.setVisible(m_curSelRow < m_stages.size() - 1);
				m_separator.setVisible(true);
			}
			else
			{
				m_miMoveUp.setVisible(false);
				m_miMoveDown.setVisible(false);
				m_separator.setVisible(false);
			}

			m_popupMenu.show(m_tblStages, evt.getX(), evt.getY());
		}
	}

	private void rearrangeIndices()
	{
		for(int i=0; i<m_stages.size(); i++)
		{
			m_stages.get(i).setIndex(i);
		}

		m_tblStages.updateUI();
	}

	private void changeOrder(int inc)
	{
		PacketStage old = m_stages.get(m_curSelRow);
		m_stages.remove(m_curSelRow);
		m_stages.add(m_curSelRow + inc, old);
		rearrangeIndices();
	}

	private void onOK()
	{
		Protocol doc = MainFrame.getInstance().getDocument();

		int cnt = m_stages.size();
		for(int i=0; i<cnt; i++)
		{
			PacketStage pksNew = m_stages.get(i);

			int cntOld = doc.getPacketStages().size();
			for(int j=0; j<cntOld; j++)
			{
				PacketStage pksOld = doc.getPacketStages().get(j);
				if(pksNew._src == pksOld)
				{
					pksOld.setIndex(pksNew.getIndex());
					pksOld.setName(pksNew.getName());
					pksOld.setAbbr(pksNew.getAbbr());
					m_stages.set(i, pksOld);
					break;
				}
			}
		}

		doc.setPacketStages(m_stages);
		m_dialogResult = true;
		dispose();
	}

	public boolean doModal()
	{
		pack();
		setVisible(true);

		return m_dialogResult;
	}

	@Override
	public int getRowCount()
	{
		return m_stages.size();
	}

	@Override
	public int getColumnCount()
	{
		return 3;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		switch(columnIndex)
		{
		case 0:
			return "Index";
		case 1:
			return "Abbreviated";
		case 2:
			return "Constant Name";
		default:
			throw new ArrayIndexOutOfBoundsException();
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		return (columnIndex == 0) ? Integer.class : String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (columnIndex > 0);
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		PacketStage pks = m_stages.get(rowIndex);

		switch(columnIndex)
		{
		case 0:
			return pks.getIndex();
		case 1:
			return pks.getAbbr();
		case 2:
			return pks.getName();
		default:
			throw new ArrayIndexOutOfBoundsException();
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		PacketStage pks = m_stages.get(rowIndex);

		switch(columnIndex)
		{
		case 1:
			pks.setAbbr(aValue.toString().trim());
			break;
		case 2:
			pks.setName(aValue.toString().trim());
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void addTableModelListener(TableModelListener tableModelListener)
	{
	}

	@Override
	public void removeTableModelListener(TableModelListener tableModelListener)
	{
	}
}
