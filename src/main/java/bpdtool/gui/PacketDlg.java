package bpdtool.gui;


import bpdtool.Util;
import bpdtool.data.Packet;
import bpdtool.data.PacketStage;
import bpdtool.data.Protocol;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;


class CheckBoxRenderer extends JCheckBox
	implements TableCellRenderer
{
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		setEnabled(table.isEnabled());
		setSelected(((JCheckBox)value).isSelected());
		//setFont(table.getFont());
		setBackground(table.getBackground());
		setForeground(table.getForeground());
		setText(((JCheckBox)value).getText());
		return this;
	}
}

class CheckBoxCellEditor extends AbstractCellEditor implements TableCellEditor
{
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		return (JCheckBox) value;
	}

	@Override
	public Object getCellEditorValue()
	{
		return null;
	}
}

public class PacketDlg extends JDialog
	implements TableModel, TableModelListener
{
	private JPanel contentPane;
	private JTextArea m_taDescript;
	private JTextField m_tfComment;
	private JTextField m_tfName;
	private JRadioButton m_rbC2S;
	private JRadioButton m_rbIEX;
	private JRadioButton m_rbS2C;
	private JCheckBox m_chkDirectCast;
	private JCheckBox m_chkGenBuilder;
	private JRadioButton m_rbStageAll;
	private JRadioButton m_rbStageSpecific;
	private JTable m_tblStages;
	private JButton m_btnOK;
	private JButton m_btnEditStages;
	private JButton m_btnCancel;

	private Packet m_data;
	private boolean m_dialogResult;
	private boolean m_isDescriptionChanged;
	private int m_nStagesCount = 0;
	private PacketStage[] m_stages;
	private JCheckBox[] m_chkStages;

	public PacketDlg(Packet pkt)
	{
		super(MainFrame.getInstance(), "Packet Property");
		m_data = pkt;

		setContentPane(contentPane);
		setLocationRelativeTo(MainFrame.getInstance());
		setModal(true);

		m_tblStages.setModel(this);
		m_tblStages.setTableHeader(null);
		m_tblStages.setDefaultRenderer(JCheckBox.class, new CheckBoxRenderer());
		m_tblStages.getColumnModel().getColumn(0).setCellEditor(new CheckBoxCellEditor());

		ActionListener lsnrRbDir = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				onPacketFlowChanged(evt.getSource());
			}
		};

		m_rbC2S.addActionListener(lsnrRbDir);
		m_rbS2C.addActionListener(lsnrRbDir);
		m_rbIEX.addActionListener(lsnrRbDir);

		ActionListener lsnrRbStage = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				onStageChanged(evt.getSource());
			}
		};

		m_rbStageAll.addActionListener(lsnrRbStage);
		m_rbStageSpecific.addActionListener(lsnrRbStage);

		m_btnOK.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				onOK();
			}
		});

		getRootPane().setDefaultButton(m_btnOK);

		m_btnEditStages.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				onEditStages();
			}
		});

		m_btnCancel.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				dispose();
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
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


		// init dialog values
		JRadioButton rb;
		if(pkt.isAllStage())
			rb = m_rbStageAll;
		else
			rb = m_rbStageSpecific;

		rb.setSelected(true);
		onStageChanged(rb);

		switch (m_data.getFlow())
		{
		case Packet.FLOW_C2S:
			rb = m_rbC2S;
			break;
		case Packet.FLOW_S2C:
			rb = m_rbS2C;
			break;
		case Packet.FLOW_INTER:
			rb = m_rbIEX;
			break;
		}

		rb.setSelected(true);
		onPacketFlowChanged(rb);

		m_chkDirectCast.setSelected(m_data.isDirectCasting());
		m_chkGenBuilder.setSelected(m_data.isGenerateBuilder());
		//m_ckFiberCall.setSelected(pkt.EnableFiber);

		m_tfName.setText(m_data.getName());
		m_tfComment.setText(m_data.getComment());
		m_taDescript.setText(m_data.getDescription());

		resetStageList();
	}

	private void resetStageList()
	{
		Protocol doc = MainFrame.getInstance().getDocument();

		m_nStagesCount = doc.getPacketStages().size();
		m_stages = new PacketStage[m_nStagesCount];
		m_chkStages = new JCheckBox[m_nStagesCount];
		for(int i=0; i<m_nStagesCount; i++)
		{
			m_stages[i] = doc.getPacketStages().get(i);
			m_chkStages[i] = new JCheckBox(m_stages[i].getAbbr(), m_data.getStages().indexOf(m_stages[i]) >= 0);
		}
	}

	private void onPacketFlowChanged(Object src)
	{
		boolean bEnablePDS;

		if (src == m_rbS2C)
		{
			m_tfName.setForeground(PacketView.NameColorS2C);
			bEnablePDS = false;
		}
		else
		{
			if (src == m_rbC2S)
			{
				m_tfName.setForeground(PacketView.NameColorC2S);
			}
			else if (src == m_rbIEX)
			{
				m_tfName.setForeground(PacketView.NameColorINTER);
			}

			bEnablePDS = true;
		}


		m_rbStageAll.setEnabled(bEnablePDS);
		m_rbStageSpecific.setEnabled(bEnablePDS);

		bEnablePDS =  bEnablePDS && m_rbStageSpecific.isSelected();
		if(m_tblStages.isEnabled() && !bEnablePDS)
			m_tblStages.editingCanceled(new ChangeEvent(src));
		m_tblStages.setEnabled(bEnablePDS);
		m_btnEditStages.setEnabled(bEnablePDS);
	}

	private void onStageChanged(Object src)
	{
		boolean bEnable;

		if(src == m_rbStageAll)
		{
			m_tblStages.editingCanceled(new ChangeEvent(src));
			bEnable = false;
		}
		else
		{
			bEnable = true;
		}

		m_tblStages.setEnabled(bEnable);
		m_btnEditStages.setEnabled(bEnable);
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
		if(doc.isPacketNameConflict(name, m_data))
		{
			ItemCommonDlg.showConflictVarNameBox(name);
			return;
		}

		m_data.setName(name);
		m_data.setComment(m_tfComment.getText().trim());

		String desc = m_taDescript.getText().trim();
		if(!desc.equals(m_data.getDescription()))
		{
			m_isDescriptionChanged = true;
			m_data.setDescription(desc);
		}

		m_data.setDirectCasting(m_chkDirectCast.isSelected());
		m_data.setGenerateBuilder(m_chkGenBuilder.isSelected());
		//m_data.setEnableFiber(m_chkFiberCall.isSelected());

		if(m_rbC2S.isSelected())
		{
			m_data.setFlow(Packet.FLOW_C2S);
		}
		else if(m_rbS2C.isSelected())
		{
			m_data.setFlow(Packet.FLOW_S2C);
		}
		else if(m_rbIEX.isSelected())
		{
			m_data.setFlow(Packet.FLOW_INTER);
		}

		if(m_data.getFlow() != Packet.FLOW_S2C)
		{
			m_data.getStages().clear();

			if(m_rbStageAll.isSelected())
			{
				m_data.setAllStage(true);
			}
			else
			{
				m_data.setAllStage(false);

				for(int i=0; i<m_nStagesCount; i++)
				{
					if(m_chkStages[i].isSelected())
					{
						m_data.getStages().add(doc.getPacketStages().get(i));
					}
				}
			}
		}

		m_dialogResult = true;
		dispose();
	}

	private void onEditStages()
	{
		EditStageDlg dlg = new EditStageDlg((Frame)getOwner());
		if(dlg.doModal())
		{
			resetStageList();
			m_tblStages.updateUI();
			m_tblStages.repaint();
			MainFrame.getInstance().getForm().refreshPacketList();
		}
	}

	public boolean isDescriptionChanged()
	{
		return m_isDescriptionChanged;
	}

	public boolean doModal()
	{
		m_dialogResult = false;

		pack();
		setVisible(true);

		return m_dialogResult;
	}

	@Override
	public int getRowCount()
	{
		return m_nStagesCount;
	}

	@Override
	public int getColumnCount()
	{
		return 1;
	}

	@Override
	public String getColumnName(int i)
	{
		return null;
	}

	@Override
	public Class<?> getColumnClass(int i)
	{
		return JCheckBox.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return m_rbStageSpecific.isSelected();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		return m_chkStages[rowIndex];
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex)
	{
	}

	@Override
	public void addTableModelListener(TableModelListener tableModelListener)
	{
	}

	@Override
	public void removeTableModelListener(TableModelListener tableModelListener)
	{
	}

	@Override
	public void tableChanged(TableModelEvent tableModelEvent)
	{
		System.out.println("TODO: stage table changed?");
	}
}
