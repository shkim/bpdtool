package bpdtool.gui;

import bpdtool.data.PrimitiveType;
import bpdtool.data.Protocol;
import bpdtool.data.PxConfig;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;


class CppTypesModel implements TableModel
{
	private ArrayList<PrimitiveType> m_vartypes;
	private HashMap<PrimitiveType, ArrayList<String>> m_expressions;

	public CppTypesModel()
	{
		m_vartypes = new ArrayList<>();
		m_vartypes.addAll(Protocol.getRecommendedNumericPrimitives().keySet());
		Collections.sort(m_vartypes);

		m_expressions = new HashMap<>();
		HashMap<String, PrimitiveType> allPrims = Protocol.getAllPrimitives();
		for (String typename : allPrims.keySet())
		{
			PrimitiveType pt = allPrims.get(typename);
			if (pt.getSizeBytes() == 0)
				continue; // not numeric type

			ArrayList<String> names = m_expressions.get(pt);
			if (names == null)
			{
				names = new ArrayList<>();
				m_expressions.put(pt, names);
			}

			names.add(typename);
		}
	}

	void fillEditorComboBox(JComboBox combo, int row)
	{
		PrimitiveType pt = m_vartypes.get(row);
		ArrayList<String> typenames = m_expressions.get(pt);
		combo.removeAllItems();
		for (String typename : typenames)
		{
			combo.addItem(typename);
		}
	}


	@Override
	public int getColumnCount()
	{
		return 2;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		switch(columnIndex)
		{
		case 0:
			return "Variable Type";
		case 1:
			return "Expression";
		}

		throw new RuntimeException("ERROR");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		return String.class;
	}

	@Override
	public int getRowCount()
	{
		return m_vartypes.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (columnIndex == 0)
		{
			return m_vartypes.get(rowIndex).getDescription();
		}

		PrimitiveType pt = m_vartypes.get(rowIndex);
		return MainFrame.getInstance().getDocument().getConfig().DelegatePrimitiveNames.get(pt);
	}

	@Override
	public void setValueAt(Object obj, int rowIndex, int columnIndex)
	{
		if (obj == null)
			return;	// maybe editing canceled

		PrimitiveType pt = m_vartypes.get(rowIndex);
		MainFrame.getInstance().getDocument().getConfig().DelegatePrimitiveNames.put(pt, obj.toString());
		TypeSelectForm.setDelegatePrimitivesDirty();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (columnIndex > 0);
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

public class ConfigForm
{
	public JPanel rootPanel;
	private JTextField m_tfPrefixC2SID;
	private JTextField m_tfPrefixStruct;
	private JTextField m_tfPrefixS2CID;
	private JTextField m_tfPrefixSender;
	private JTextField m_tfPrefixBuilder;
	private JTextField m_tfPrefixHandler;
	private JCheckBox m_chkUse16BitID;
	private JCheckBox m_chkUseHexID;
	private JCheckBox m_chkNoComment;
	private JCheckBox m_chkNoPacketStage;
	private JCheckBox m_chkAddBOM;
	private JCheckBox m_chkEnableAltTypes;
	private JTable m_tblCppTypes;
	private JButton m_btnUseRecommendedTypes;
	private CppTypesModel m_cppTypesModel;

	public ConfigForm()
	{
		m_cppTypesModel = new CppTypesModel();
		m_tblCppTypes.setModel(m_cppTypesModel);
		m_tblCppTypes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tblCppTypes.setRowHeight(19);

		m_tblCppTypes.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox()) {
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
				m_cppTypesModel.fillEditorComboBox((JComboBox) editorComponent, row);
				return super.getTableCellEditorComponent(table, value, isSelected, row, column);
			}
		});

		TableColumnModel cm = m_tblCppTypes.getColumnModel();
		cm.getColumn(0).setPreferredWidth(200);
		cm.getColumn(1).setPreferredWidth(150);

		m_btnUseRecommendedTypes.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				selectAdvisableCppTypes();
			}
		});

		m_chkEnableAltTypes.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				onToggleAltTypeEnable();
			}
		});
	}

	public void updateFromDocument(Protocol doc)
	{
		PxConfig config = doc.getConfig();

		m_tfPrefixC2SID.setText(config.Prefix.C2SPacketID);
		m_tfPrefixS2CID.setText(config.Prefix.S2CPacketID);
		m_tfPrefixStruct.setText(config.Prefix.PacketStruct);
		m_tfPrefixSender.setText(config.Prefix.SenderMethod);
		m_tfPrefixBuilder.setText(config.Prefix.BuilderMethod);
		m_tfPrefixHandler.setText(config.Prefix.HandlerMethod);

		m_chkUse16BitID.setSelected(config.Use16BitPacketID);
		m_chkUseHexID.setSelected(config.UseHexID);
		m_chkNoComment.setSelected(config.NoExportComment);
		m_chkNoPacketStage.setSelected(config.NoUsePacketDispatchStage);
		m_chkAddBOM.setSelected(config.AddUnicodeBOM);

		m_chkEnableAltTypes.setSelected(config.EnableAlternativeCppTypes);
	}

	public void updateToDocument(Protocol doc)
	{
		PxConfig config = doc.getConfig();

		config.Prefix.C2SPacketID = m_tfPrefixC2SID.getText().trim();
		config.Prefix.S2CPacketID = m_tfPrefixS2CID.getText().trim();
		config.Prefix.PacketStruct = m_tfPrefixStruct.getText().trim();
		config.Prefix.SenderMethod = m_tfPrefixSender.getText().trim();
		config.Prefix.BuilderMethod = m_tfPrefixBuilder.getText().trim();
		config.Prefix.HandlerMethod = m_tfPrefixHandler.getText().trim();

		config.Use16BitPacketID = m_chkUse16BitID.isSelected();
		config.UseHexID = m_chkUseHexID.isSelected();
		config.NoExportComment = m_chkNoComment.isSelected();
		config.NoUsePacketDispatchStage = m_chkNoPacketStage.isSelected();
		config.AddUnicodeBOM = m_chkAddBOM.isSelected();

		config.EnableAlternativeCppTypes = m_chkEnableAltTypes.isSelected();
	}

	private void onToggleAltTypeEnable()
	{
		boolean enableAlt = m_chkEnableAltTypes.isSelected();
		MainFrame.getInstance().getDocument().getConfig().EnableAlternativeCppTypes = enableAlt;
		m_tblCppTypes.setEnabled(!enableAlt);
		m_tblCppTypes.clearSelection();
		if (m_tblCppTypes.isEditing())
			m_tblCppTypes.getCellEditor().stopCellEditing();
	}

	private void selectAdvisableCppTypes()
	{
		MainFrame.getInstance().getDocument().getConfig().setDefaultDelegatePrimitives();
		m_tblCppTypes.repaint();
	}
}
