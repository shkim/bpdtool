package bpdtool.gui;

import bpdtool.Main;
import bpdtool.Util;
import bpdtool.data.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class PacketView extends ItemView_WithTable
	implements InplaceTextEditor.Listener
{
	public static ImageIcon ImgC2S;
	public static ImageIcon ImgS2C;
	public static ImageIcon ImgInter;
	private static ImageIcon ImgDirectCast;
	private static ImageIcon ImgGenerateBuilder;

	public final static Color NameColorC2S = new Color(0x9400D3);	// DarkViolet
	public final static Color NameColorS2C = new Color(0x4169E1);	// RoyalBlue
	public final static Color NameColorINTER = Color.black;

	private static final String[] ColumnLabels = new String[] { "Name", "Type", "Repeat", "Bytes", "Comment" };
	private static final int[] MinColumnWidths = new int[] { 120, 100, 80, 60, 100 };

	static
	{
		ImgC2S = Main.createImageIcon("c2s.png", "Client to Server");
		ImgS2C = Main.createImageIcon("s2c.png", "Server to Client");
		ImgInter = Main.createImageIcon("inter.png", "Inter Exchange");
		ImgDirectCast = Main.createImageIcon("dcast.png", "Direct Casting");
		ImgGenerateBuilder = Main.createImageIcon("builder.png", "Generate Builder");
	}

	private Packet m_data;
	private PacketGroupView m_parentGroupView;
	private DefaultMutableTreeNode m_treeNode;

	public PacketView(Packet d, PacketGroupView grv)
	{
		super(ColumnLabels, MinColumnWidths);

		m_data = d;
		m_parentGroupView = grv;
		m_treeNode = new DefaultMutableTreeNode(d.getName());
		m_treeNode.setUserObject(this);

		setRowCount(d.getFields().size());
	}

	public Packet getData()
	{
		return m_data;
	}

	public void deleteData()
	{
		m_data = null;
		m_treeNode.setUserObject(null);
	}

	public DefaultMutableTreeNode getTreeNode()
	{
		return m_treeNode;
	}

	public PacketGroupView getParentGroupView()
	{
		return m_parentGroupView;
	}

	public void setParentGroupView(PacketGroupView grv)
	{
		m_parentGroupView = grv;
	}

	@Override
	protected ItemCommons getItemCommons()
	{
		return m_data;
	}

	@Override
	protected boolean canItemMoveUp()
	{
		int idx = m_parentGroupView.indexOf(this);
		return (idx > 0);
	}

	@Override
	protected boolean canItemMoveDown()
	{
		int idx = m_parentGroupView.indexOf(this);
		return (idx >= 0 && (idx +1) < m_parentGroupView.getPacketViews().size());
	}

	@Override
	protected void onItemMoveDown()
	{
		int toPos = m_parentGroupView.indexOf(this) +2;
		getParentGroupView().movePacketTo(this, toPos);
	}

	@Override
	protected void onItemMoveUp()
	{
		int toPos = m_parentGroupView.indexOf(this) -1;
		getParentGroupView().movePacketTo(this, toPos);
	}

	@Override
	protected void onItemCopy()
	{
		int idx = getParentGroupView().indexOf(this);
		if (idx < 0)
		{
			// error
			return;
		}

		Packet pkt = Packet.makeCopy(getData(), MainFrame.getInstance().getDocument());
		getParentGroupView().addPacket(pkt, idx + 1);
	}

	@Override
	protected void onItemInsert()
	{
		int idx = getParentGroupView().indexOf(this);
		if (idx < 0)
		{
			// error
			return;
		}

		getParentGroupView().addNewPacketAt(idx);
	}

	@Override
	protected void onItemDelete()
	{
		getParentGroupView().removePacket(this, true);
		getParent().remove(this);
		MainFrame.getInstance().getForm().refreshPacketList();
	}

	@Override
	public void getTableCellContents(int col, int row, CellContents cell)
	{
		PacketField fld = m_data.getFields().get(row);

		switch (col)
		{
		case 0:	// Name
			cell.str = fld.getName();
			cell.font = FontField;
			cell.color = Color.black;
			cell.isCenter = true;
			break;

		case 1:	// Type
			cell.str = fld.getType();
			if(fld.getPrimitiveType() == null)
			{
				cell.font = FontDatatypeUser;
				cell.color = Color.black;
			}
			else
			{
				cell.font = FontDatatypePrim;
				cell.color = ColorDatatypePrim;
			}
			cell.isCenter = true;
			break;

		case 2:	// Repeat
			if(fld.getRepeatInfo().isVariableRepeat())
			{
				cell.font = FontDatatypePrim;
				//cell.color = (fld.Repeat.type == RepeatTypes.AutoVarNumber) ? Brushes.Black : Brushes.Gray;
				cell.color = Color.black;
			}
			else
			{
				cell.font = FontDatatypeUser;
				cell.color = Color.gray;
			}
			cell.str = fld.getRepeatInfo().toDecorativeString();
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
			cell.color = Color.gray;
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
	public String getRowDescription(int row)
	{
		return m_data.getFields().get(row).getDescription();
	}

	@Override
	public void onPopupMenuClick(int cmd, int row, int inc)
	{
		PacketField fld;
		int r2;

		int nOldFields = m_data.getFields().size();
		switch(cmd)
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
			m_data.getFields().add(row, PacketField.createNewPacketField());
			break;

		case TABLEFIELD_REMOVE:
			m_data.getFields().remove(row);
			break;

		case TABLEFIELD_ADD:
			m_data.getFields().add(PacketField.createNewPacketField());
			break;

		case TABLEFIELD_ADD_DESC:
			m_data.getFields().get(row).setDescription("EDITME");
			break;
		}

		if (nOldFields != m_data.getFields().size())
		{
			setRowCount(m_data.getFields().size());
		}

		recalcLayout(getWidth(), true);
		((PacketListPanel)getParent()).layoutSubviews();
	}

	@Override
	protected void paintComponent(Graphics gr)
	{
		super.paintComponent(gr);

		Color namecolor;
		ImageIcon bmFlow;

		switch (m_data.getFlow())
		{
		case Packet.FLOW_C2S:
			namecolor = NameColorC2S;
			bmFlow = ImgC2S;
			break;

		case Packet.FLOW_S2C:
			namecolor = NameColorS2C;
			bmFlow = ImgS2C;
			break;

		case Packet.FLOW_INTER:
			namecolor = NameColorINTER;
			bmFlow = ImgInter;
			break;

		default:
			namecolor = Color.red;
			bmFlow = null;
			break;
		}

		int xLabel = drawBigName(gr, m_data.getName(), namecolor);
		if(bmFlow != null)
		{
			gr.drawImage(bmFlow.getImage(), xLabel, ItemView.YPOS_SUBJECT, this);
			xLabel += 32;
		}

		if(m_data.isDirectCasting())
		{
			gr.drawImage(ImgDirectCast.getImage(), xLabel, ItemView.YPOS_SUBJECT, this);
			xLabel += 18;
		}

		if(m_data.isGenerateBuilder())
		{
			gr.drawImage(ImgGenerateBuilder.getImage(), xLabel, ItemView.YPOS_SUBJECT, this);
			xLabel += 12;
		}

		int y = ItemView.YPOS_SUBJECT + 10;

		if (m_data.getFlow() != Packet.FLOW_S2C && m_data.isAllStage() == false && m_data.getStages().size() > 0)
		{
			String sts = "[";
			for (int i = 0; i < m_data.getStages().size() - 1; i++)
			{
				sts += m_data.getStages().get(i).getAbbr() + ",";
			}

			sts += m_data.getStages().get(m_data.getStages().size() - 1).getAbbr() + "]";

			gr.setFont(FontStage);
			gr.setColor(Color.red);
			gr.drawString(sts, xLabel, y);
			xLabel += Util.measureString(gr, sts, FontStage).width +3;
		}

		if(!Util.isNullOrEmpty(m_data.getComment()))
		{
			gr.setColor(ColorComment);
			gr.setFont(FontComment);
			gr.drawString(m_data.getComment(), xLabel, y);
		}

		if (!isCollapsed())
		{
			drawTable(gr);
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
		//System.out.println("OnInplaceTextChanged: " + text);

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
}

