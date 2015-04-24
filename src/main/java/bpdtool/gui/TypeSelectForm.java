package bpdtool.gui;

import bpdtool.data.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

public class TypeSelectForm extends JDialog
{
	private final String AUTO_REPEAT = "(Auto)";

	private JPanel contentPane;
	private JPanel rootPanel;
	private JRadioButton m_rbPrimitive;
	private JRadioButton m_rbUserDef;
	private JComboBox m_cbPrimitives;
	private JComboBox m_cbUserDefs;
	private JRadioButton m_rbVarLength;
	private JComboBox m_cbVarLength;
	private JRadioButton m_rbFixedLength;
	private JRadioButton m_rbNoArray;
	private JSpinner m_tbRepeatLimit;
	private JSpinner m_tbFixedLength;
	private JButton m_btnApply;
	private JButton m_btnCancel;

	private ItemView_WithTable m_owner;
	private Packet m_refPacket;
	private Struct m_refStruct;
	private StructField m_commonField;
	private int m_nFieldIndex;
	private boolean m_isAllPrimitivesLoaded;

	private static boolean s_needUpdateDelegatePrimitives;
	public static void setDelegatePrimitivesDirty()
	{
		s_needUpdateDelegatePrimitives = true;
	}

	public TypeSelectForm()
	{
		super(MainFrame.getInstance(), true);

		setContentPane(contentPane);
		setUndecorated(true);
		setLocationRelativeTo(null);

		loadAllPrimitives();

		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		/*addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				close();
			}
		});*/

		ActionListener escActionLsnr = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				close();
			}
		};

		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		rootPanel.registerKeyboardAction(escActionLsnr, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

		m_btnApply.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				onApply();
			}
		});

		m_btnCancel.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				close();
			}
		});

		m_rbPrimitive.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				onClickPrimitive();
			}
		});

		m_rbUserDef.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				onClickUserDef();
			}
		});

		m_rbVarLength.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				onClickVarLength();
			}
		});

		m_rbFixedLength.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				onClickFixedLength();
			}
		});

		m_rbNoArray.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				onClickNoArray();
			}
		});

		m_tbRepeatLimit.setModel(new SpinnerNumberModel(2, 2, 9999, 1));
		m_tbFixedLength.setModel(new SpinnerNumberModel(2, 2, 9999, 1));
	}

	private void onClickPrimitive()
	{
		m_cbPrimitives.setEnabled(true);
		m_cbUserDefs.setEnabled(false);
	}

	private void onClickUserDef()
	{
		m_cbPrimitives.setEnabled(false);
		m_cbUserDefs.setEnabled(true);
	}

	private void onClickVarLength()
	{
		m_tbFixedLength.setEnabled(false);
		m_cbVarLength.setEnabled(true);
		m_tbRepeatLimit.setEnabled(true);
	}

	private void onClickFixedLength()
	{
		m_tbFixedLength.setEnabled(true);
		m_cbVarLength.setEnabled(false);
		m_tbRepeatLimit.setEnabled(false);
	}

	private void onClickNoArray()
	{
		m_tbFixedLength.setEnabled(false);
		m_cbVarLength.setEnabled(false);
		m_tbRepeatLimit.setEnabled(false);
	}

	private void loadAllPrimitives()
	{
		m_isAllPrimitivesLoaded = true;

		m_cbPrimitives.removeAllItems();
		for (String primTypeName : Protocol.getPrimitiveTypeNames())
		{
			m_cbPrimitives.addItem(primTypeName);
		}
	}

	private void loadDelegatePrimitives(HashMap<PrimitiveType, String> delePrims)
	{
		m_isAllPrimitivesLoaded = false;
		s_needUpdateDelegatePrimitives = false;

		m_cbPrimitives.removeAllItems();
		for (String typename : delePrims.values())
		{
			m_cbPrimitives.addItem(typename);
		}

		// Non-numeric types (String, Buffer)
		for (String key : Protocol.getAllPrimitives().keySet())
		{
			PrimitiveType pt = Protocol.getAllPrimitives().get(key);
			if (pt.getCategory() == 0)
			{
				m_cbPrimitives.addItem(key);
			}
		}
	}

	private void initCommon()
	{
		Protocol doc = MainFrame.getInstance().getDocument();

		if (doc.getConfig().EnableAlternativeCppTypes)
		{
			if (!m_isAllPrimitivesLoaded)
			{
				loadAllPrimitives();
			}
		}
		else
		{
			if (m_isAllPrimitivesLoaded || s_needUpdateDelegatePrimitives)
			{
				loadDelegatePrimitives(doc.getConfig().DelegatePrimitiveNames);
			}
		}

		// init user types
		m_cbUserDefs.removeAllItems();
		for (UserType ut : doc.getUserTypes())
		{
			if (m_refStruct != null)
			{
				// skip blind class and self
				if (ut instanceof BlindClass || ut == (UserType) m_refStruct)
					continue;
			}

			m_cbUserDefs.addItem(ut.getName());
		}

		if (m_cbUserDefs.getItemCount() == 0)
		{
			m_cbUserDefs.setEnabled(false);
			m_rbUserDef.setEnabled(false);
		}
		else
		{
			m_cbUserDefs.setEnabled(true);
			m_rbUserDef.setEnabled(true);
		}


		if (m_commonField.getPrimitiveType() != null)
		{
			m_cbPrimitives.setSelectedItem(m_commonField.getType());
			m_rbPrimitive.setSelected(true);
		}
		else
		{
			m_cbUserDefs.setSelectedItem(m_commonField.getType());
			m_rbUserDef.setSelected(true);
		}
	}

	private void initStruct(Struct d)
	{
		m_refPacket = null;
		m_refStruct = (Struct) d;
		m_commonField = m_refStruct.getFields().get(m_nFieldIndex);

		initCommon();

		// struct variable-length-field is prohibited
		m_cbVarLength.setEnabled(false);
		m_rbVarLength.setEnabled(false);
		m_tbRepeatLimit.setEnabled(false);

		StructField fld = m_refStruct.getFields().get(m_nFieldIndex);

		if (fld.getRepeatInfo().hasRepeat())
		{
			m_rbFixedLength.setSelected(true);
			m_tbFixedLength.setValue(fld.getRepeatInfo().getCount());
		}
		else
		{
			m_rbNoArray.setSelected(true);
		}
	}

	private void initPacket(Packet d)
	{
		PacketField fld;

		m_refPacket = (Packet) d;
		m_refStruct = null;
		m_commonField = m_refPacket.getFields().get(m_nFieldIndex);

		initCommon();

		m_cbVarLength.removeAllItems();
		m_cbVarLength.addItem(AUTO_REPEAT);

		for (int i = 0; i < m_nFieldIndex; i++)
		{
			fld = m_refPacket.getFields().get(i);
			if (fld.getPrimitiveType() != null)
			{
				if (fld.getPrimitiveType().getCategory() == PrimitiveType.SIGNED_INTEGER
				|| fld.getPrimitiveType().getCategory() == PrimitiveType.UNSIGNED_INTEGER)
				{
					m_cbVarLength.addItem(fld.getName());
				}
			}
		}

		if (m_cbVarLength.getItemCount() == 0)
		{
			m_cbVarLength.setEnabled(false);
			m_rbVarLength.setEnabled(false);
		}

		fld = m_refPacket.getFields().get(m_nFieldIndex);

		if (fld.getRepeatInfo().isOnce())
		{
			m_rbNoArray.setSelected(true);
		}
		else if(fld.getRepeatInfo().isVariableRepeat())
		{
			if (fld.getRepeatInfo().getType() == RepeatInfo.TYPE_AUTO_VAR)
				m_cbVarLength.setSelectedIndex(0);
			else
				m_cbVarLength.setSelectedItem(fld.getRepeatInfo().getReference());

			m_tbRepeatLimit.setValue(fld.getRepeatInfo().getLimit());
			m_rbVarLength.setSelected(true);
		}
		else
		{
			m_tbFixedLength.setValue(fld.getRepeatInfo().getCount());
			m_rbFixedLength.setSelected(true);
		}
	}

	private void initData(Object d, int row)
	{
		m_nFieldIndex = row;

		if (d instanceof Packet)
		{
			initPacket((Packet) d);
		}
		else if(d instanceof Struct)
		{
			initStruct((Struct) d);
		}
		else
		{
			showErrorBox("Invalid type for TypeSelector.Popup");
			return;
		}

		if (m_rbPrimitive.isSelected())
			onClickPrimitive();
		else
			onClickUserDef();

		if (m_rbNoArray.isSelected())
			onClickNoArray();
		else if (m_rbFixedLength.isSelected())
			onClickFixedLength();
		else
			onClickVarLength();;
	}

	private void onApply()
	{
		RepeatInfo newRepeat = new RepeatInfo();

		if (m_rbVarLength.isSelected())
		{
			if (m_cbVarLength.getSelectedIndex() < 0)
			{
				showErrorBox("Please select a repeater field.");
				return;
			}

			if (m_refPacket == null)
			{
				showErrorBox("Invalid repeat selection");
				return;
			}

			newRepeat.setReference(m_cbVarLength.getItemAt(m_cbVarLength.getSelectedIndex()).toString());
			if (newRepeat.getReference().equals(AUTO_REPEAT))
			{
				newRepeat.setType(RepeatInfo.TYPE_AUTO_VAR);
				newRepeat.setReference(null);
				newRepeat.setCount(-1);
			}
			else
			{
				newRepeat.setType(RepeatInfo.TYPE_BY_REFERENCE);
				newRepeat.setCount(-1);
			}

			newRepeat.setLimit(Integer.parseInt(m_tbRepeatLimit.getValue().toString()));
		}
		else if (m_rbNoArray.isSelected())
		{
			newRepeat.setOnce();
		}
		else
		{
			newRepeat.set(Integer.parseInt(m_tbFixedLength.getValue().toString()));
		}

		boolean changed = false;
		if (m_rbPrimitive.isSelected())
		{
			String primname = m_cbPrimitives.getSelectedItem().toString().trim();
			PrimitiveType primtype = Protocol.getIfPrimitive(primname);
			if (primtype == null)
			{
				showErrorBox("'" + primname + "' is not a valid(supported) primitive type.");
				return;
			}

			if (m_commonField.getPrimitiveType() == primtype)
			{
				// no change, check if name differrent
				if (!m_commonField.getType().equals(primname))
				{
					m_commonField.setType(primname);
					changed = true;
				}
			}
			else
			{
				changed = true;
				m_commonField.setType(primname);

				if (m_commonField.getPrimitiveType() != null)
				{
					// changed to another primitive
					assert(m_commonField.getCustomType() == null);
				}
				else
				{
					// was usertype
					assert(m_commonField.getCustomType() != null);
					m_commonField.getCustomType().decRef();
					m_commonField.setCustomType(null);
				}

				m_commonField.setPrimitiveType(primtype);
			}
		}
		else
		{
			String typename = m_cbUserDefs.getSelectedItem().toString().trim();

			boolean found = false;
			for(int i=0; i<m_cbUserDefs.getItemCount(); i++)
			{
				String item = m_cbUserDefs.getItemAt(i).toString();
				if(item.equals(typename))
				{
					found = true;
					break;
				}
			}

			if(!found)
			{
				showErrorBox("Custom type '" + typename + "' is not in the combo box.");
				return;
			}

			UserType utFound = null;
			for (UserType ut : MainFrame.getInstance().getDocument().getUserTypes())
			{
				if (ut.getName().equals(typename))
				{
					utFound = ut;
					break;
				}
			}

			if (utFound == null)
			{
				showErrorBox("Invalid user-defined type: " + typename);
				return;
			}

			if (utFound != m_commonField.getCustomType())
			{
				if (m_commonField.getCustomType() != null)
				{
					assert(m_commonField.getPrimitiveType() == null);
					m_commonField.getCustomType().decRef();
				}

				m_commonField.setType(typename);
				m_commonField.setPrimitiveType(null);
				m_commonField.setCustomType(utFound);
				m_commonField.getCustomType().incRef();
				changed = true;
			}
		}

		if (!changed && m_commonField.getRepeatInfo().isSame(newRepeat))
		{
			if(m_refPacket != null)
			{
				if (newRepeat.isSame(m_refPacket.getFields().get(m_nFieldIndex).getRepeatInfo()))
				{
					// no change
					close();
					return;
				}
			}
			else
			{
				close();
				return;
			}
		}

		m_commonField.getRepeatInfo().set(newRepeat);
		if(m_refPacket != null)
			m_refPacket.getFields().get(m_nFieldIndex).getRepeatInfo().set(newRepeat);

		m_owner.recalcLayout();
		close();
	}

	private static void showErrorBox(String msg)
	{
		JOptionPane.showMessageDialog(s_this, msg,  "Type Select", JOptionPane.ERROR_MESSAGE);
	}

	private void close()
	{
		setVisible(false);
	}

	private static TypeSelectForm s_this;

	public static void show(ItemView_WithTable owner, Object d, ItemView_WithTable.HitResult hit)
	{
		if (s_this == null)
		{
			s_this = new TypeSelectForm();
		}

		s_this.m_owner = owner;
		s_this.initData(d, hit.row);

		s_this.pack();

		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		final int displayHeight = gd.getDisplayMode().getHeight();

		Point ptBase = owner.getLocationOnScreen();
		ptBase.y += hit.box.y;
		if (ptBase.y + s_this.getHeight() > displayHeight)
		{
			ptBase.y -= s_this.getHeight() + ItemView_WithTable.CY_ROW;
		}

		s_this.setLocation(ptBase.x + hit.box.x -1, ptBase.y);
		s_this.setVisible(true);
		s_this.requestFocus();
	}

}
