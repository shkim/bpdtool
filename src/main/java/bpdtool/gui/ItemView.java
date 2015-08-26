package bpdtool.gui;


import bpdtool.Main;
import bpdtool.Util;
import bpdtool.data.ItemCommons;
import bpdtool.data.Packet;
import bpdtool.data.PacketGroup;
import bpdtool.data.UserType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

interface ICollapseButtonListener
{
	void onCollapseChanged();
}

class CollapseButton extends JComponent
{
	private static ImageIcon s_imgPlus;
	private static ImageIcon s_imgMinus;

	static
	{
		s_imgPlus = Main.createImageIcon("plus.png", "PLUS");
		s_imgMinus = Main.createImageIcon("minus.png", "MINUS");
	}

	private boolean m_isCollapsed;
	private ICollapseButtonListener m_lsnr;

	public CollapseButton(ICollapseButtonListener lsnr)
	{
		m_lsnr = lsnr;
		m_isCollapsed = false;

		setSize(9, 9);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				toggle();
			}
		});
	}

	private void toggle()
	{
		m_isCollapsed = !m_isCollapsed;
		repaint();
		m_lsnr.onCollapseChanged();
	}

	public boolean isCollapsed()
	{
		return m_isCollapsed;
	}

	public void setCollapsed(boolean value)
	{
		m_isCollapsed = value;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.drawImage((m_isCollapsed ? s_imgPlus : s_imgMinus).getImage(), 0, 0, this);
	}
}

class TableItemPopupMenu
{
	private ItemView m_owner;
	private JPopupMenu m_popupMenu;
	private JMenuItem m_miProperty;
	private JMenuItem m_miCopy;
	private JMenuItem m_miInsert;
	private JMenuItem m_miDelete;
	private JMenuItem m_miMoveUp;
	private JMenuItem m_miMoveDown;

	public TableItemPopupMenu()
	{
		m_miProperty = new JMenuItem("Edit Property", 'p');
		m_miCopy = new JMenuItem("Copy", 'c');
		m_miInsert = new JMenuItem("Insert", 'i');
		m_miDelete = new JMenuItem("Delete", 'e');
		m_miMoveUp = new JMenuItem("Move Up", 'u');
		m_miMoveDown = new JMenuItem("Move Down", 'd');

		m_popupMenu = new JPopupMenu();

		m_miProperty.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_owner.onMenuEditProp();
			}
		});

		m_miCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_owner.onMenuCopy();
			}
		});

		m_miInsert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_owner.onItemInsert();
			}
		});

		m_miDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_owner.onMenuDelete();
			}
		});

		m_miMoveUp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_owner.onItemMoveUp();
			}
		});

		m_miMoveDown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_owner.onItemMoveDown();
			}
		});
	}

	public void show(ItemView owner, Component vw, Point pt)
	{
		m_popupMenu.removeAll();

		m_popupMenu.add(m_miProperty);
		m_popupMenu.addSeparator();
		m_popupMenu.add(m_miCopy);
		m_popupMenu.add(m_miInsert);
		m_popupMenu.add(m_miDelete);

		int canMoves = 0;
		if (owner.canItemMoveUp())
			canMoves |= 1;
		if (owner.canItemMoveDown())
			canMoves |= 2;

		if (canMoves != 0)
		{
			m_popupMenu.addSeparator();

			if ((canMoves & 1) != 0)
				m_popupMenu.add(m_miMoveUp);

			if ((canMoves & 2) != 0)
				m_popupMenu.add(m_miMoveDown);
		}

		m_owner = owner;
		m_popupMenu.show(vw, pt.x, pt.y);
	}

	public void show(ItemView owner, Point pt)
	{
		show(owner, owner, pt);
	}
}

public abstract class ItemView extends JPanel
	implements ICollapseButtonListener
{
	static Font FontBigName = new Font("Tahoma", Font.BOLD, 15);
	static Font FontStage = new Font("Arial", Font.PLAIN, 12);
	static Font FontComment = new Font("Gulim", Font.PLAIN, 12);
	static Font FontDescription = new Font("Gulim", Font.PLAIN, 12);
	static Font FontField = new Font("Verdana", Font.PLAIN, 12);
	static Font FontDatatypePrim = new Font("Arial", Font.ITALIC, 14);
	static Font FontDatatypeUser = new Font("Arial", Font.PLAIN, 14);

	static Color ColorComment = new Color(0, 128, 0);
	static Color ColorDatatypePrim = new Color(0, 0, 139);

	static Color ColorDarkViolet = new Color(0x9400D3);
	static Color ColorDarkBlue = new Color(0x00008B);
	static Color ColorDarkRed = new Color(0x8B0000);

	protected static final int MARGIN_HEIGHT = 5;

	protected static final int RBOX_MARGIN_LEFT = 12;
	protected static final int RBOX_MARGIN_TOP = 3;
	protected static final int RBOX_MARGIN_RIGHT = 4;
	protected static final int RBOX_MARGIN_BOTTOM = 3;
	protected static final int RBOX_CORNER_SIZE = 11;
	protected static final int RBOX_PADDING_LEFT = 6;
	protected static final int RBOX_PADDING_TOP = 4;
	protected static final int RBOX_PADDING_RIGHT = 5;
	protected static final int RBOX_PADDING_BOTTOM = 4;

	protected static final int COLLAPSED_HEIGHT = RBOX_MARGIN_TOP + RBOX_MARGIN_BOTTOM + RBOX_PADDING_TOP + RBOX_PADDING_BOTTOM + 10;
	protected static final int YPOS_SUBJECT = RBOX_MARGIN_TOP + RBOX_PADDING_TOP;
	protected static final int YPOS_DESCRIPTION = RBOX_MARGIN_TOP + RBOX_PADDING_TOP + 26;

	protected static TableItemPopupMenu s_popupItemMenu = new TableItemPopupMenu();

	private CollapseButton m_colbtn;

	protected String[] m_itemDescLines;    // not null if item description is multilined.
	protected static int s_cyDescLineHeight;
	protected int m_cxLastLayoutWidth;
	protected int m_cyRequiredHeight;
	private static boolean s_bUseAntiAlias = Util.isMacOSX();

	ItemView()
	{
		setLayout(null);
		//setBackground(Color.white);

		m_colbtn = new CollapseButton(this);
		add(m_colbtn);
		m_colbtn.setBounds(1, 8, 9, 9);

		/*addComponentListener(new java.awt.event.ComponentAdapter()
		{
			public void componentResized(java.awt.event.ComponentEvent evt)
			{
				recalcLayout(getWidth(), false);
			}
		});*/
	}

	public void onCollapseChanged()
	{
		recalcLayout();
		MainFrame.getInstance().getForm().refreshPacketList();
	}

	public boolean isCollapsed()
	{
		return m_colbtn.isCollapsed();
	}

	protected void recalcLayout(int cx, boolean forceRecalc)
	{
		if (!forceRecalc && cx == m_cxLastLayoutWidth)
			return;

		m_cxLastLayoutWidth = cx;
		int cyReqHeight = COLLAPSED_HEIGHT;
		if (!isCollapsed())
		{
			Graphics gr = MainFrame.getInstance().getGraphics();
			cyReqHeight += calcItemDescriptionLayout(gr);
		}

		setSize(m_cxLastLayoutWidth, cyReqHeight);
	}

	public void recalcLayout()
	{
		recalcLayout(getWidth(), true);
	}

	protected int calcItemDescriptionLayout(Graphics gr)
	{
		String desc = getItemCommons().getDescription();
		if (Util.isNullOrEmpty(desc))
			return 0;

		int[] cxy = new int[3];
		if (desc.indexOf('\n') >= 0)
		{
			m_itemDescLines = Util.splitAndMeasureMultilineString(gr, desc, FontDescription, cxy);
			s_cyDescLineHeight = cxy[2];
			return cxy[1];
		}
		else
		{
			m_itemDescLines = null;
			return (int) Util.measureString(gr, desc, FontDescription).getHeight();
		}
	}

	protected static int drawBigName(Graphics gr, String name, Color color)
	{
		gr.setFont(FontBigName);
		gr.setColor(color);
		gr.drawString(name, RBOX_MARGIN_LEFT + RBOX_PADDING_LEFT, YPOS_SUBJECT + 11);

		return (RBOX_MARGIN_LEFT + RBOX_PADDING_LEFT + 5 + Util.measureString(gr, name, FontBigName).width);
	}

	protected static void drawItemComment(Graphics gr, String cmnt, int x)
	{
		if (!Util.isNullOrEmpty(cmnt))
		{
			gr.setColor(ColorComment);
			gr.setFont(FontComment);
			gr.drawString(cmnt, x, YPOS_SUBJECT + 11);
		}
	}

	protected void drawItemDescription(Graphics gr, Color fontColor)
	{
		String desc = getItemCommons().getDescription();
		if (!Util.isNullOrEmpty(desc))
		{
			gr.setFont(FontDescription);
			gr.setColor(fontColor);

			if (m_itemDescLines != null)
			{
				Util.drawMultilineString(gr, m_itemDescLines, RBOX_MARGIN_LEFT + RBOX_PADDING_LEFT, YPOS_DESCRIPTION, s_cyDescLineHeight);
			}
			else
			{
				gr.drawString(desc, RBOX_MARGIN_LEFT + RBOX_PADDING_LEFT, YPOS_DESCRIPTION);
			}
		}
	}

	@Override
	protected void paintComponent(Graphics gr)
	{
		super.paintComponent(gr);

		if (s_bUseAntiAlias)
		{
			((java.awt.Graphics2D) gr).setRenderingHint(
				java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
				java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON
			);
		}

		int width = getWidth() - RBOX_MARGIN_LEFT - RBOX_MARGIN_RIGHT;
		int height = getHeight() - RBOX_MARGIN_TOP - RBOX_MARGIN_BOTTOM;

		//gr.setColor(Color.LIGHT_GRAY);
		//gr.drawRoundRect(RBOX_MARGIN_LEFT +1, RBOX_MARGIN_TOP +1, width, height, RBOX_CORNER_SIZE, RBOX_CORNER_SIZE);
		gr.setColor(Color.WHITE);
		gr.fillRoundRect(RBOX_MARGIN_LEFT, RBOX_MARGIN_TOP, width, height, RBOX_CORNER_SIZE, RBOX_CORNER_SIZE);
		gr.setColor(Color.GRAY);
		gr.drawRoundRect(RBOX_MARGIN_LEFT, RBOX_MARGIN_TOP, width, height, RBOX_CORNER_SIZE, RBOX_CORNER_SIZE);
	}

	protected abstract ItemCommons getItemCommons();
	protected abstract boolean canItemMoveUp();
	protected abstract boolean canItemMoveDown();
	protected abstract void onItemMoveDown();
	protected abstract void onItemMoveUp();
	protected abstract void onItemCopy();
	protected abstract void onItemDelete();
	protected abstract void onItemInsert();

	void onMenuEditProp()
	{
		ItemCommons d = getItemCommons();
		boolean bDescriptionChanged;

		if(d instanceof Packet)
		{
			PacketDlg dlg = new PacketDlg((Packet)d);
			if(!dlg.doModal())
				return;

			bDescriptionChanged = dlg.isDescriptionChanged();
		}
		else if (d instanceof PacketGroup)
		{
			PacketGroupDlg dlg = new PacketGroupDlg((PacketGroup)d);
			if (!dlg.doModal())
				return;

			bDescriptionChanged = dlg.isDescriptionChanged();
		}
		else
		{
			ItemCommonDlg dlg = new ItemCommonDlg(d);
			if(!dlg.doModal())
				return;

			bDescriptionChanged = dlg.isDescriptionChanged();
		}

		if(d instanceof UserType)
			MainFrame.getInstance().getDocument().onTypeNameChanged((UserType) d);

		if(bDescriptionChanged)
		{
			recalcLayout();
			MainFrame.getInstance().getForm().refreshPacketList();
		}

		repaint();
	}

	void onMenuCopy()
	{
		ItemCommons d = getItemCommons();
		String msg = String.format("Make a copy of the %s '%s' ?", d.getDataTypeName(), d.getName());

		if (MainFrame.showConfirmBox(msg, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			onItemCopy();
		}
	}
/*
	void onMenuInsert()
	{
		ItemCommons d = getItemCommons();
		//MainFrame.getInstance().insertItem(d);
		System.out.println("TODO: insert item");
	}
*/
	void onMenuDelete()
	{
		ItemCommons d = getItemCommons();
		String msg = String.format("Delete the %s '%s' ?", d.getDataTypeName(), d.getName());

		if (MainFrame.showConfirmBox(msg, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			onItemDelete();
		}
	}

}
