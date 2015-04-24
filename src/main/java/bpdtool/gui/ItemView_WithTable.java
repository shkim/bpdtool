package bpdtool.gui;

import bpdtool.Util;
import bpdtool.data.Protocol;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


class ColumnInfo
{
	public int x, center, width;
}

class RowInfo
{
	public int y, height, center;
	public boolean hasDescription;
	public String[] descLines;
}

class TableFieldPopupMenu
{
	private ItemView_WithTable m_owner;
	private int m_nRowUnderMenu;

	private JPopupMenu m_popupFieldMenu;
	private JMenuItem m_miMoveUp;
	private JMenuItem m_miMoveDown;
	private JMenuItem m_miInsert;
	private JMenuItem m_miRemove;
	private JMenuItem m_miAdd;
	private JMenuItem m_miAddDesc;

	private JPopupMenu m_popupNonFieldMenu;
	private JMenuItem m_miNonFieldAdd;

	public TableFieldPopupMenu()
	{
		m_popupFieldMenu = new JPopupMenu();
		m_popupNonFieldMenu = new JPopupMenu();

		m_miNonFieldAdd = new JMenuItem("Add", 'a');
		m_miMoveUp = new JMenuItem("Move Up", 'u');
		m_miMoveDown = new JMenuItem("Move Down", 'd');
		m_miInsert = new JMenuItem("Insert", 'i');
		m_miRemove = new JMenuItem("Remove", 'r');
		m_miAdd = new JMenuItem("Add", 'a');
		m_miAddDesc = new JMenuItem("Add Description", 'e');

		m_popupNonFieldMenu.add(m_miNonFieldAdd);

		m_miMoveUp.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (m_nRowUnderMenu > 0)
				{
					m_owner.onPopupMenuClick(ItemView_WithTable.TABLEFIELD_MOVEUP, m_nRowUnderMenu, -1);
				}
			}
		});
		m_miMoveDown.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (m_nRowUnderMenu < m_owner.getRowCount() - 1)
				{
					m_owner.onPopupMenuClick(ItemView_WithTable.TABLEFIELD_MOVEDOWN, m_nRowUnderMenu, 1);
				}
			}
		});
		m_miInsert.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_owner.onPopupMenuClick(ItemView_WithTable.TABLEFIELD_INSERT, m_nRowUnderMenu, 0);
			}
		});
		m_miRemove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_owner.onPopupMenuClick(ItemView_WithTable.TABLEFIELD_REMOVE, m_nRowUnderMenu, 0);
			}
		});
		m_miAdd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_owner.onPopupMenuClick(ItemView_WithTable.TABLEFIELD_ADD, 0, 0);
			}
		});
		m_miNonFieldAdd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_owner.onPopupMenuClick(ItemView_WithTable.TABLEFIELD_ADD, 0, 0);
			}
		});
		m_miAddDesc.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_owner.onPopupMenuClick(ItemView_WithTable.TABLEFIELD_ADD_DESC, m_nRowUnderMenu, 0);
			}
		});
	}

	public void show(ItemView_WithTable owner, int rowUnderMenu, Point pt)
	{
		m_owner = owner;
		m_nRowUnderMenu = rowUnderMenu;

		if (rowUnderMenu < 0)
		{
			m_popupNonFieldMenu.show(owner, pt.x, pt.y);
			return;
		}

		m_popupFieldMenu.removeAll();
		if (owner.getRowCount() > 1)
		{
			if (rowUnderMenu > 0)
			{
				m_popupFieldMenu.add(m_miMoveUp);
			}
			if (rowUnderMenu < owner.getRowCount() - 1)
			{
				m_popupFieldMenu.add(m_miMoveDown);
			}

			m_popupFieldMenu.addSeparator();
		}

		if (rowUnderMenu == owner.getRowCount() - 1)
		{
			m_popupFieldMenu.add(m_miAdd);
		}

		m_popupFieldMenu.add(m_miInsert);
		m_popupFieldMenu.add(m_miRemove);

		if (!m_owner.getRowInfo(rowUnderMenu).hasDescription)
		{
			m_popupFieldMenu.addSeparator();
			m_popupFieldMenu.add(m_miAddDesc);
		}

		m_popupFieldMenu.show(owner, pt.x, pt.y);
	}
}

public abstract class ItemView_WithTable extends ItemView
{
	public static class HitResult
	{
		public int area;
		public int row;
		public Rectangle box;

		public static final int NAME =1;
		public static final int TYPE =2;
		public static final int COMMENT =3;
		public static final int DESCRIPTION =4;
	}


	public static final int TABLEFIELD_MOVEUP =1;
	public static final int TABLEFIELD_MOVEDOWN =2;
	public static final int TABLEFIELD_INSERT =3;
	public static final int TABLEFIELD_REMOVE =4;
	public static final int TABLEFIELD_ADD =5;
	public static final int TABLEFIELD_ADD_DESC =6;

	public static final int CY_ROW = 20;
	private final int CX_LEFTMARGIN = RBOX_MARGIN_LEFT + RBOX_PADDING_LEFT;
	private final int CX_RIGHTMARGIN = RBOX_MARGIN_RIGHT + RBOX_PADDING_RIGHT;

	private static Color BrsTableHeader = new Color(0xF0, 0xF0, 0xFF);
	private static Color BrsTableRowEven = new Color(0xFF, 0xFF, 0xFF);
	private static Color BrsTableRowOdd = new Color(0xF8, 0xF8, 0xF8);
	private static Font FontLabel = new Font("Verdana", Font.PLAIN, 12);

	private static TableFieldPopupMenu s_popupFieldMenu = new TableFieldPopupMenu();

	private String[] m_aHeaderLabels;
	private int[] m_aMinColumnWidths;
	private int m_nColumns, m_nRows;

	private ColumnInfo[] m_columns;
	private RowInfo[] m_rows;
	private int m_nTableWidth;
	private int m_nTableBeginYPos;
	private boolean m_bRowHasDescription;

	public ItemView_WithTable(String[] labels, int[] minWidths)
	{
		m_aHeaderLabels = labels;
		m_aMinColumnWidths = minWidths;

		m_nColumns = labels.length;
		m_columns = new ColumnInfo[m_nColumns];
		for(int c=0; c<m_nColumns; c++)
		{
			m_columns[c] = new ColumnInfo();
		}

		addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				HitResult hit = hitTest(e);
				if (hit != null)
					onHitTable(hit);
			}
		});
	}

	public void showItemMenu(Component comp, Point pt)
	{
		s_popupItemMenu.show(this, comp, pt);
	}

	protected boolean checkValidVarName(String name)
	{
		if (Util.isValidVarName(name))
		{
			return true;
		}

		MainFrame.showMsgBox("Syntax error: invalid name", JOptionPane.ERROR_MESSAGE);
		return false;
	}

	protected boolean checkValidNumber(String name)
	{
		if (Util.isStringNumber(name))
		{
			return true;
		}

		MainFrame.showMsgBox("Syntax error: not a number", JOptionPane.ERROR_MESSAGE);
		return false;
	}

	protected abstract void onPopupMenuClick(int cmd, int row, int inc);
	protected abstract String getRowDescription(int row);
	protected abstract void getTableCellContents(int col, int row, CellContents cell);
	protected abstract void onHitTable(HitResult hit);

	public int getRowCount()
	{
		return m_nRows;
	}

	public RowInfo getRowInfo(int r)
	{
		assert (r >= 0 && r < m_nRows);
		return m_rows[r];
	}

	public void setRowCount(int rows)
	{
		m_nRows = rows;
		m_rows = new RowInfo[rows];

		for(int n=0; n<rows; n++)
		{
			m_rows[n] = new RowInfo();
		}
	}

	@Override
	protected void recalcLayout(int cx, boolean forceRecalc)
	{
		if (forceRecalc == false && cx == m_cxLastLayoutWidth)
			return;

		m_cxLastLayoutWidth = cx;
		m_cyRequiredHeight = isCollapsed() ? COLLAPSED_HEIGHT : calcTableLayout(cx);
		setSize(cx, m_cyRequiredHeight);
		repaint();
	}

	protected int calcTableLayout(int width)
	{
		Graphics gr = MainFrame.getInstance().getGraphics();

		width -= CX_RIGHTMARGIN;
		m_nTableWidth = width - CX_LEFTMARGIN - 1;

		int cyReqHeight = COLLAPSED_HEIGHT + calcItemDescriptionLayout(gr);
		m_nTableBeginYPos = cyReqHeight + 1;

		CellContents cell = new CellContents();

		cyReqHeight += CY_ROW;	// header is always visible (for now)

		for (int x = 0; x < m_nColumns - 1; x++)
		{
			m_columns[x].width = m_aMinColumnWidths[x];
		}

		int[] cxy = new int[3];
		m_bRowHasDescription = false;
		for (int y = 0; y < m_nRows; y++)
		{
			m_rows[y].y = cyReqHeight;

			for (int x = 0; x < m_nColumns - 1; x++)
			{
				getTableCellContents(x, y, cell);
				if (cell.str == null)
				{
					continue;
				}

				int cWidth = (int) Util.measureString(gr, cell.str, cell.font).getWidth();
				cWidth += 16;	// margin
				if (m_columns[x].width < cWidth)
				{
					m_columns[x].width = cWidth;
				}
			}

			cyReqHeight += CY_ROW;

			String desc = getRowDescription(y);
			m_rows[y].hasDescription = !Util.isNullOrEmpty(desc);
			if (m_rows[y].hasDescription)
			{
				m_bRowHasDescription = true;
				if(desc.indexOf('\n') >= 0)
				{
					m_rows[y].descLines = Util.splitAndMeasureMultilineString(gr, desc, FontDescription, cxy);
					cyReqHeight += cxy[1] + 5;
					//s_cyDescLineHeight = cxy[2];
				}
				else
				{
					m_rows[y].descLines = null;
					int cHeight = (int) Util.measureString(gr, desc, FontDescription).getHeight();
					cyReqHeight += cHeight + 5;
				}
			}

			m_rows[y].height = cyReqHeight - m_rows[y].y;
			m_rows[y].center = m_rows[y].y + m_rows[y].height / 2 +2;
		}

		m_columns[0].x = CX_LEFTMARGIN;
		for (int x = 1; x < m_nColumns; x++)
		{
			m_columns[x].x = m_columns[x - 1].x + m_columns[x - 1].width;
		}

		m_columns[m_nColumns - 1].width = width - m_columns[m_nColumns - 1].x - 1;
//			if (m_columns[nColumns - 1].width < m_aMinColumnWidths[nColumns - 1])
//				m_columns[nColumns - 1].width = m_aMinColumnWidths[nColumns - 1];

		for (int x = 0; x < m_nColumns; x++)
		{
			m_columns[x].center = m_columns[x].x + m_columns[x].width / 2;
		}

		cyReqHeight += MARGIN_HEIGHT;
		return cyReqHeight + RBOX_PADDING_BOTTOM;
	}

	public void drawTable(Graphics gr)
	{
		if (YPOS_DESCRIPTION < m_nTableBeginYPos)
		{
			drawItemDescription(gr, Color.gray);
		}

		CellContents cell = new CellContents();

		int y = m_nTableBeginYPos;
		// table header
		{
			gr.setColor(BrsTableHeader);
			gr.fillRect(CX_LEFTMARGIN, m_nTableBeginYPos, m_nTableWidth, CY_ROW);

			gr.setFont(FontLabel);
			gr.setColor(Color.black);

			int yc = y + CY_ROW / 2;
			for (int i = 0; i < m_nColumns; i++)
			{
				Util.drawStringCenter(gr, m_aHeaderLabels[i], m_columns[i].center, yc);
			}

			y += CY_ROW;
		}

		int x2 = m_nTableWidth + CX_LEFTMARGIN;
		int seq = 0;
		for (int row = 0; row < m_nRows; row++)
		{
			Color bgc = ((seq++ & 1) == 0) ? BrsTableRowEven : BrsTableRowOdd;
			gr.setColor(bgc);
			gr.fillRect(CX_LEFTMARGIN, y, m_nTableWidth, m_rows[row].height);
			gr.setColor(Color.gray);
			gr.drawLine(CX_LEFTMARGIN, y, x2, y);

			int yc = y + CY_ROW / 2 + 1;
			for (int col = 0; col < m_nColumns; col++)
			{
				getTableCellContents(col, row, cell);
				if(Util.isNullOrEmpty(cell.str))
					continue;

				gr.setFont(cell.font);
				gr.setColor(cell.color);

				if (col == 0)
				{
					// first column may be rowspan=2 by description row on the right side.
					Util.drawStringCenter(gr, cell.str, m_columns[col].center, m_rows[row].center);
				}
				else if (cell.isCenter)
				{
					Util.drawStringCenter(gr, cell.str, m_columns[col].center, yc);
				}
				else
				{
					gr.drawString(cell.str, m_columns[col].x + 4, yc +4);
				}
			}

			y += CY_ROW;

			if (m_rows[row].hasDescription)
			{
				gr.setColor(Color.gray);
				gr.drawLine(m_columns[1].x, y, x2, y);

				gr.setFont(FontDescription);
				gr.setColor(Color.gray);

				if(m_rows[row].descLines != null)
				{
					Util.drawMultilineString(gr, m_rows[row].descLines,
						m_columns[1].x + 4, y + 15, s_cyDescLineHeight);
				}
				else
				{
					gr.drawString(getRowDescription(row), m_columns[1].x + 4, y + 15);
				}

				y += m_rows[row].height - CY_ROW;
			}
		}

		gr.setColor(Color.gray);
		gr.drawRect(CX_LEFTMARGIN, m_nTableBeginYPos, m_nTableWidth, y - m_nTableBeginYPos);

		// vertical lines
		gr.setColor(Color.gray);
		if (m_bRowHasDescription)
		{
			gr.drawLine(m_columns[1].x, m_nTableBeginYPos, m_columns[1].x, y);

			y = m_nTableBeginYPos;
			for (int row = 0; row < m_nRows; row++)
			{
				for (int i = 2; i < m_nColumns; i++)
				{
					gr.drawLine(m_columns[i].x, y +1, m_columns[i].x, m_rows[row].y + CY_ROW);
				}

				y = m_rows[row].y + m_rows[row].height;
			}
		}
		else
		{
			for (int i = 1; i < m_nColumns; i++)
			{
				gr.drawLine(m_columns[i].x, m_nTableBeginYPos, m_columns[i].x, y);
			}
		}

/*
		if(seq == 0)
		{
			gr.FillRectangle(BrsTableRowEven, CX_TABLE_LEFTMARGIN, y, cxBox, CY_TABLE_ROW);
			gr.DrawRectangle(Pens.Gray, CX_TABLE_LEFTMARGIN, y, cxBox, CY_TABLE_ROW);

			gr.DrawString("Press Right-Mouse-Button to pop up the context menu...",
				FontNotify, Brushes.Red, this.Width / 2, y + CY_TABLE_ROW / 2 + 2, SfCenter);
		}
 */
	}

	protected HitResult hitTest(MouseEvent e)
	{
		Point pt = e.getPoint();

		if (Util.isRightMouseButton(e))
		{
			if (pt.y < m_nTableBeginYPos || pt.x < m_columns[0].x)
			{
				s_popupItemMenu.show(this, pt);
				return null;
			}

			for (int r = 0; r < m_nRows; r++)
			{
				if (m_rows[r].y < pt.y && pt.y <= (m_rows[r].y + m_rows[r].height))
				{
					s_popupFieldMenu.show(this, r, pt);
					return null;
				}
			}

			s_popupFieldMenu.show(this, -1, pt);
			return null;
		}

		if (pt.y < m_nTableBeginYPos || pt.x < m_columns[0].x)
		{
			if(e.getClickCount() >= 2 && !e.isConsumed())
			{
				// double click
				onMenuEditProp();
			}

			return null;
		}

		for (int r = 0; r < m_nRows; r++)
		{
			if (m_rows[r].y < pt.y && pt.y <= (m_rows[r].y + m_rows[r].height))
			{
				HitResult ret = new HitResult();

				ret.row = r;
				ret.box = new Rectangle();
				ret.box.y = m_rows[r].y;

				if (pt.x <= m_columns[1].x)
				{
					ret.area = HitResult.NAME;
					ret.box.x = m_columns[0].x;
					ret.box.width = m_columns[0].width;
					ret.box.height = m_rows[r].height;
				}
				else
				{
					ret.box.height = CY_ROW;

					if (pt.y < (m_rows[r].y + CY_ROW))
					{
						if (pt.x < m_columns[m_nColumns - 1].x)
						{
							ret.area = HitResult.TYPE;
							ret.box.x = m_columns[1].x;

							if (m_nColumns == 3)
							{
								// Constant table
								ret.box.width = m_columns[1].width;
							}
							else
							{
								ret.box.y += CY_ROW;
							}
						}
						else
						{
							ret.area = HitResult.COMMENT;
							ret.box.x = m_columns[m_nColumns - 1].x;
							ret.box.width = m_columns[m_nColumns - 1].width;
						}
					}
					else if (m_rows[r].hasDescription)
					{
						ret.area = HitResult.DESCRIPTION;

						ret.box.x = m_columns[1].x;
						ret.box.y += CY_ROW;
						ret.box.height = m_rows[r].height - CY_ROW + 2;
						ret.box.width = 0;
						for (int i = 1; i < m_nColumns; i++)
						{
							ret.box.width += m_columns[i].width;
						}
					}
				}

				ret.box.x += 1;
				ret.box.y += 1;
				ret.box.width -= 1;
				ret.box.height -= 2;

				return ret;
			}
		}

		return null;
	}

}
