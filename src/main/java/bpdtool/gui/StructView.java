package bpdtool.gui;

import bpdtool.data.ItemCommons;
import bpdtool.data.Struct;
import bpdtool.data.StructField;

import java.awt.*;

public class StructView extends ItemView_WithTable
	implements InplaceTextEditor.Listener
{
	private static final String[] ColumnLabels = new String[] { "Name", "Type", "Repeat", "Bytes", "Comment" };
	private static final int[] MinColumnWidths = new int[] { 120, 100, 80, 60, 100 };

	private Struct m_data;

	public StructView(Struct d)
	{
		super(ColumnLabels, MinColumnWidths);

		m_data = d;
		setRowCount(d.getFields().size());
	}

	public Struct getData()
	{
		return m_data;
	}

	@Override
	protected void onPopupMenuClick(int cmd, int row, int inc)
	{
		StructField fld;
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
			m_data.getFields().add(row, StructField.createNew());
			break;

		case TABLEFIELD_REMOVE:
			m_data.getFields().remove(row);
			break;

		case TABLEFIELD_ADD:
			m_data.getFields().add(StructField.createNew());
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
		StructField fld = m_data.getFields().get(row);

		switch (col)
		{
		case 0:	// Name
			cell.str = fld.getName();
			cell.font = FontField;
			cell.color = Color.BLACK;
			cell.isCenter = true;
			break;

		case 1:	// Type
			cell.str = fld.getType();
			if (fld.getPrimitiveType() == null)
			{
				cell.font = FontDatatypeUser;
				cell.color = Color.BLACK;
			}
			else
			{
				cell.font = FontDatatypePrim;
				cell.color = ColorDarkBlue;
			}
			cell.isCenter = true;
			break;

		case 2:	// Repeat
			cell.str = fld.getRepeatInfo().toString();
			cell.font = FontDatatypeUser;
			cell.color = Color.GRAY;
			cell.isCenter = true;
			break;

		case 3:	// Bytes
			if (fld.getRepeatInfo().isVariableRepeat())
			{
				cell.str = "VAR";
			}
			else
			{
				int cb = (fld.getPrimitiveType() == null) ? fld.getCustomType().getSizeBytes() : fld.getPrimitiveType().getSizeBytes();
				cell.str = (cb > 0) ? Integer.toString(cb * fld.getRepeatInfo().getCount()) : "VAR";
			}
			cell.font = FontDatatypeUser;
			cell.color = Color.GRAY;
			cell.isCenter = true;
			break;

		case 4:	// Comment
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
		if(hit.area == HitResult.TYPE)
		{
			TypeSelectForm.show(this, m_data, hit);
		}
		else
		{
			String prev;

			switch (hit.area)
			{
			case HitResult.NAME:
				prev = m_data.getFields().get(hit.row).getName();
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
		//System.out.println("Struct.OnInplaceTextChanged: " + text);

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

		default:
			return;
		}

		recalcLayout();
	}

	@Override
	protected ItemCommons getItemCommons()
	{
		return m_data;
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
