package bpdtool.gui;

import bpdtool.Util;
import bpdtool.data.Constant;
import bpdtool.data.ConstantField;
import bpdtool.data.ItemCommons;

import java.awt.*;

public class ConstantView extends ItemView_WithTable
	implements InplaceTextEditor.Listener
{
	private static final String[] DefineColumnLabels = new String[]
		{
			"Define", "Value", "Comment"
		};
	private static final String[] EnumColumnLabels = new String[]
		{
			"Enum", "Value", "Comment"
		};
	private static final int[] MinColumnWidths = new int[]
		{
			150, 80, 200
		};

	private Constant m_data;

	public ConstantView(Constant d)
	{
		super(d.isEnum() ? EnumColumnLabels : DefineColumnLabels, MinColumnWidths);

		m_data = d;
		setRowCount(d.getFields().size());
	}

	@Override
	protected ItemCommons getItemCommons()
	{
		return m_data;
	}

	@Override
	protected void onPopupMenuClick(int cmd, int row, int inc)
	{
		ConstantField fld;
		int r2;

		int nOldFields = m_data.getFields().size();
		switch (cmd)
		{
		case TABLEFIELD_MOVEUP:
			r2 = row - 1;
			fld = m_data.getFields().get(row);
			m_data.getFields().set(row, m_data.getFields().get(r2));
			m_data.getFields().set(r2, fld);
			break;
		case TABLEFIELD_MOVEDOWN:
			r2 = row + 1;
			fld = m_data.getFields().get(row);
			m_data.getFields().set(row, m_data.getFields().get(r2));
			m_data.getFields().set(r2, fld);
			break;

		case TABLEFIELD_INSERT:
			m_data.getFields().add(row, ConstantField.createNew());
			break;

		case TABLEFIELD_REMOVE:
			m_data.getFields().remove(row);
			break;

		case TABLEFIELD_ADD:
			m_data.getFields().add(ConstantField.createNew());
			break;

		case TABLEFIELD_ADD_DESC:
			m_data.getFields().get(row).setDescription("EDITME");
			break;
		}

		if (nOldFields != m_data.getFields().size())
		{
			setRowCount(m_data.getFields().size());
		}

		recalcLayout();
		((ViewListPanel)getParent()).layoutSubviews();
	}

	@Override
	protected String getRowDescription(int row)
	{
		return m_data.getFields().get(row).getDescription();
	}

	@Override
	protected void getTableCellContents(int col, int row, CellContents cell)
	{
		ConstantField fld = m_data.getFields().get(row);

		switch (col)
		{
		case 0:	// Name
			cell.str = fld.getName();
			cell.font = FontField;
			cell.color = ColorDarkViolet;
			cell.isCenter = true;
			break;

		case 1:	// Value
			cell.str = fld.getValue();
			cell.font = FontDatatypeUser;
			cell.color = Color.BLACK;
			cell.isCenter = true;
			break;

		case 2:	// Comment
			cell.str = fld.getComment();
			cell.font = FontComment;
			cell.color = ColorComment;
			cell.isCenter = false;
			break;

		default:
			cell.str = null;
			break;
		}
	}

	@Override
	protected void onHitTable(HitResult hit)
	{
		if (hit != null)
		{
			String prev;

			switch (hit.area)
			{
			case HitResult.NAME:
				prev = m_data.getFields().get(hit.row).getName();
				break;

			case HitResult.TYPE:
				prev = m_data.getFields().get(hit.row).getValue();
				break;

			case HitResult.COMMENT:
				prev = m_data.getFields().get(hit.row).getComment();
				break;

			case HitResult.DESCRIPTION:
				prev = m_data.getFields().get(hit.row).getDescription();
				break;

			default:
				return;
			}

			InplaceTextEditor.show(this, prev, hit);
		}
	}

	@Override
	public void onInplaceTextChanged(HitResult hit, String text)
	{
		switch (hit.area)
		{
		case HitResult.NAME:
			if (checkValidVarName(text))
			{
				m_data.getFields().get(hit.row).setName(text);
			}
			break;
		case HitResult.COMMENT:
			m_data.getFields().get(hit.row).setComment(text);
			break;
		case HitResult.DESCRIPTION:
			m_data.getFields().get(hit.row).setDescription(text);
			break;

		case HitResult.TYPE:
			if(Util.isNullOrEmpty(text))
			{
				m_data.getFields().get(hit.row).setValue(null);
			}
			else if (checkValidNumber(text))
			{
				m_data.getFields().get(hit.row).setValue(text);
			}
			break;

		default:
			return;
		}

		recalcLayout();
	}

	@Override
	protected boolean canItemMoveUp()
	{
		return false;
	}

	@Override
	protected boolean canItemMoveDown()
	{
		return false;
	}

	@Override
	protected void onItemMoveDown()
	{

	}

	@Override
	protected void onItemMoveUp()
	{

	}

	@Override
	protected void onItemCopy()
	{

	}

	@Override
	protected void onItemDelete()
	{

	}

	@Override
	protected void onItemInsert()
	{

	}

	@Override
	protected void paintComponent(Graphics gr)
	{
		super.paintComponent(gr);

		int xLabel = drawBigName(gr, m_data.getName(), ColorDarkBlue);
		drawItemComment(gr, m_data.getComment(), xLabel);

		if (!isCollapsed())
		{
			drawTable(gr);
		}
	}
}
