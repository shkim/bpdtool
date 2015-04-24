package bpdtool.gui;

import bpdtool.Util;
import bpdtool.data.ItemCommons;
import bpdtool.data.Packet;
import bpdtool.data.PacketGroup;
import bpdtool.data.Protocol;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;


class PacketGroupPopupMenu
{
	private PacketGroupView m_owner;
	private JPopupMenu m_popupMenu;
	private JMenuItem m_miAddPacket;
	private JMenuItem m_miProperty;
	private JMenuItem m_miInsert;
	private JMenuItem m_miDelete;
	private JMenuItem m_miMoveUp;
	private JMenuItem m_miMoveDown;

	public PacketGroupPopupMenu()
	{
		m_miAddPacket = new JMenuItem("Add Packet", 'a');
		m_miProperty = new JMenuItem("Edit Property", 'p');
		m_miInsert = new JMenuItem("Insert", 'i');
		m_miDelete = new JMenuItem("Delete", 'e');
		m_miMoveUp = new JMenuItem("Move Up", 'u');
		m_miMoveDown = new JMenuItem("Move Down", 'd');

		m_popupMenu = new JPopupMenu();

		m_miAddPacket.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_owner.addNewPacketAt(-1);
			}
		});

		m_miProperty.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_owner.onMenuEditProp();
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

	public void show(PacketGroupView owner, Component vw, Point pt)
	{
		m_popupMenu.removeAll();

		m_popupMenu.add(m_miAddPacket);
		m_popupMenu.addSeparator();
		m_popupMenu.add(m_miProperty);
		m_popupMenu.addSeparator();
		m_popupMenu.add(m_miInsert);

		if (owner.canItemDelete())
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

	public void show(PacketGroupView owner, Point pt)
	{
		show(owner, owner, pt);
	}
}

public class PacketGroupView extends ItemView
{
	private PacketGroup m_data;
	private DefaultMutableTreeNode m_treeNode;

	private ArrayList<PacketView> m_packetViews;

	protected static PacketGroupPopupMenu s_popupGroupMenu = new PacketGroupPopupMenu();

	public PacketGroupView(PacketGroup grp)
	{
		m_data = grp;
		m_treeNode = new DefaultMutableTreeNode(grp.getName());
		m_treeNode.setUserObject(this);

		m_packetViews = new ArrayList<PacketView>();

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				onMouseClick(e);
			}
		});
	}

	public void showItemMenu(Component comp, Point pt)
	{
		s_popupGroupMenu.show(this, comp, pt);
	}

	private void onMouseClick(MouseEvent e)
	{
		if (Util.isRightMouseButton(e))
		{
			s_popupGroupMenu.show(this, e.getPoint());
			return;
		}

		if(e.getClickCount() >= 2 && !e.isConsumed())
		{
			// double click
			onMenuEditProp();
		}
	}

	public PacketGroup getData()
	{
		return m_data;
	}

	public DefaultMutableTreeNode getTreeNode()
	{
		return m_treeNode;
	}

	@Override
	protected ItemCommons getItemCommons()
	{
		return m_data;
	}

	@Override
	protected boolean canItemMoveUp()
	{
		Protocol doc = MainFrame.getInstance().getDocument();
		int idx = doc.getPacketGroups().indexOf(getData());
		return (idx > 0);
	}

	@Override
	protected boolean canItemMoveDown()
	{
		Protocol doc = MainFrame.getInstance().getDocument();
		int idx = doc.getPacketGroups().indexOf(getData());
		return (idx >= 0 && (idx +1) < doc.getPacketGroups().size());
	}

	protected boolean canItemDelete()
	{
		return m_packetViews.isEmpty();
	}

	@Override
	protected void onItemMoveDown()
	{
		System.out.println("TODO: packetGroup move down");
		//MainFrame.s_this.ReorderComponent(this, 1);
	}

	@Override
	protected void onItemMoveUp()
	{
		System.out.println("TODO: packetGroup move up");
		//MainFrame.s_this.ReorderComponent(this, -1);
	}

	protected void onItemCopy()
	{
		throw new UnsupportedOperationException();
	}

	protected void onItemDelete()
	{
		MainFrame.getInstance().getForm().getPacketsPanel().deleteGroup(this);
	}

	protected void onItemInsert()
	{
		Util.printf("TODO: PacketGroup add");
	}

	static Color ColorGroupComment = new Color(0,196,0);
	static Color ColorStartID = new Color(255,128,128);

	@Override
	protected void paintComponent(Graphics gr)
	{
		int width = getWidth();
		int height = getHeight();

		//gr.setColor(getParent().getBackground());
		gr.setColor(Color.DARK_GRAY);
		gr.fillRect(0, 0, width, height);

		gr.setColor(Color.LIGHT_GRAY);
		--height;
		gr.drawLine(0, height, width, height);

		int xLabel = drawBigName(gr, m_data.getName(), Color.WHITE);
		int y = ItemView.YPOS_SUBJECT + 10;

		if (!Util.isNullOrEmpty(m_data.getStartId()))
		{
			String str = "(StartID=" + m_data.getStartId() + ")";

			gr.setFont(FontStage);
			gr.setColor(ColorStartID);
			gr.drawString(str, xLabel, y);
			xLabel += Util.measureString(gr, str, FontStage).width +3;
		}

		if (!Util.isNullOrEmpty(m_data.getComment()))
		{
			gr.setColor(ColorGroupComment);
			gr.setFont(FontComment);
			gr.drawString(m_data.getComment(), xLabel, y);
		}

		if (isCollapsed())
			return;

		drawItemDescription(gr, Color.lightGray);
	}

	public void addPacketView(PacketView pkv)
	{
		m_packetViews.add(pkv);
		getTreeNode().add(pkv.getTreeNode());
	}

	public void addPacketView(int pos, PacketView pkv)
	{
		m_packetViews.add(pos, pkv);
		getTreeNode().insert(pkv.getTreeNode(), pos);
	}

	public final ArrayList<PacketView> getPacketViews()
	{
		return m_packetViews;
	}

	public int indexOf(PacketView pkv)
	{
		return m_packetViews.indexOf(pkv);
	}

	public void reloadTree()
	{
		DefaultTreeModel tmodel = (DefaultTreeModel) MainFrame.getInstance().getForm().getPacketsTree().getModel();
		tmodel.reload(getTreeNode());
	}

	public void removePacket(PacketView pkv, boolean isDelete)
	{
		m_packetViews.remove(pkv);
		pkv.setParentGroupView(null);

		m_treeNode.remove(pkv.getTreeNode());
		getData().removePacket(pkv.getData());

		if (isDelete)
			pkv.deleteData();

		reloadTree();
	}

	public void movePacketTo(PacketView pkv, int toPos)
	{
		System.out.println("movePacketTo " + toPos);

		if (m_packetViews.contains(pkv))
		{
			int oldPos = m_packetViews.indexOf(pkv);

			m_packetViews.remove(oldPos);
			m_treeNode.remove(oldPos);

			getData().movePacketPos(pkv.getData(), toPos);

			if (oldPos < toPos)
				toPos--;

			m_packetViews.add(toPos, pkv);
			m_treeNode.insert(pkv.getTreeNode(), toPos);
		}
		else
		{
			pkv.getParentGroupView().removePacket(pkv, false);

			m_packetViews.add(toPos, pkv);
			pkv.setParentGroupView(this);

			m_treeNode.insert(pkv.getTreeNode(), toPos);

			getData().addPacket(toPos, pkv.getData());
		}

		reloadTree();
		MainFrame.getInstance().getForm().refreshPacketList();
	}

	public void addPacket(Packet pkt, int pos)
	{
		getData().addPacket(pos, pkt);
		PacketView pkv = new PacketView(pkt, this);
		addPacketView(pos, pkv);
		getParent().add(pkv);

		JTree tree = MainFrame.getInstance().getForm().getPacketsTree();
		DefaultTreeModel tmodel = (DefaultTreeModel) tree.getModel();
		tmodel.reload(getTreeNode());
		tree.expandPath(new TreePath(tmodel.getPathToRoot(getTreeNode())));

		MainFrame.getInstance().getForm().refreshPacketList();
	}

	public void addNewPacketAt(int pos)
	{
		if (pos < 0)
			pos = m_packetViews.size();

		Packet pkt = Packet.createNew();
		PacketDlg dlg = new PacketDlg(pkt);
		if(dlg.doModal())
		{
			addPacket(pkt, pos);
		}
	}
}
