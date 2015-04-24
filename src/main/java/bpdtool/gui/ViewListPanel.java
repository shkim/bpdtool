package bpdtool.gui;

import bpdtool.Util;
import bpdtool.data.BlindClass;
import bpdtool.data.Constant;
import bpdtool.data.Protocol;
import bpdtool.data.Struct;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class ViewListPanel extends JPanel
{
	private ArrayList<ItemView> m_views;

	private JPopupMenu m_popupMenu;

	public ViewListPanel(boolean isUserTypeList)
	{
		setLayout(null);
		m_views = new ArrayList<ItemView>();

		if (isUserTypeList)
		{
			setupUserTypeMenu();
		}
		else
		{
			setupConstantMenu();
		}

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (Util.isRightMouseButton(e))
				{
					m_popupMenu.show(ViewListPanel.this, e.getX(), e.getY());
				}
			}
		});
	}

	private void setupUserTypeMenu()
	{
		m_popupMenu = new JPopupMenu();
		JMenuItem miAddStruct = new JMenuItem("Add Struct", 's');
		miAddStruct.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				MainFrame.getInstance().getForm().onMenuAddStruct();
			}
		});
		m_popupMenu.add(miAddStruct);

		JMenuItem miAddBclass = new JMenuItem("Add BlindClass", 'b');
		miAddBclass.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				MainFrame.getInstance().getForm().onMenuAddBlindClass();
			}
		});
		m_popupMenu.add(miAddBclass);
	}

	private void setupConstantMenu()
	{
		m_popupMenu = new JPopupMenu();
		JMenuItem miAddDefine = new JMenuItem("Add Define", 'd');
		miAddDefine.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				MainFrame.getInstance().getForm().onMenuAddDefine();
			}
		});
		m_popupMenu.add(miAddDefine);

		JMenuItem miAddEnum = new JMenuItem("Add Enum", 'e');
		miAddEnum.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				MainFrame.getInstance().getForm().onMenuAddEnum();
			}
		});
		m_popupMenu.add(miAddEnum);
	}

	public void clear()
	{
		for (ItemView vw : m_views)
		{
			remove(vw);
		}

		m_views.clear();
	}

	public void addView(ItemView vw)
	{
		m_views.add(vw);
		add(vw);
	}

	public void layoutSubviews()
	{
		int width = getWidth();
		int y = 0;
		for (ItemView vw : m_views)
		{
			vw.recalcLayout(width, false);
			vw.setBounds(0, y, width, vw.getHeight());
			y += vw.getHeight();
		}

		setSize(width, y);
		setPreferredSize(getSize());
	}

	public void addStruct()
	{
		Struct st = Struct.createNew();
		ItemCommonDlg dlg = new ItemCommonDlg(st);
		if(dlg.doModal())
		{
			MainFrame.getInstance().getDocument().getUserTypes().add(st);

			StructView vwSt = new StructView(st);
			m_views.add(vwSt);
			add(vwSt);
			layoutSubviews();
		}
	}

	public void addBlindClass()
	{
		BlindClass bc = BlindClass.createNew();
		ItemCommonDlg dlg = new ItemCommonDlg(bc);
		if(dlg.doModal())
		{
			MainFrame.getInstance().getDocument().getUserTypes().add(bc);

			BlindClassView vwBc = new BlindClassView(bc);
			m_views.add(vwBc);
			add(vwBc);
			layoutSubviews();
		}
	}

	public void addConstant(boolean isEnum)
	{
		Constant ct = Constant.createNew(isEnum);
		ItemCommonDlg dlg = new ItemCommonDlg(ct);
		if(dlg.doModal())
		{
			MainFrame.getInstance().getDocument().getConstants().add(ct);

			ConstantView vwCt = new ConstantView(ct);
			m_views.add(vwCt);
			add(vwCt);
			layoutSubviews();
		}
	}
}
