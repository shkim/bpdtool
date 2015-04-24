package bpdtool.gui;

import bpdtool.Util;
import bpdtool.data.Packet;
import bpdtool.data.PacketGroup;
import bpdtool.data.Protocol;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;

class PacketTreeCellRenderer extends DefaultTreeCellRenderer
{
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

		Object obj = ((DefaultMutableTreeNode) value).getUserObject();
		if (obj instanceof PacketGroupView)
		{
			PacketGroupView grv = (PacketGroupView)obj;
			PacketGroup grp = grv.getData();

			if (grp.getPackets().isEmpty())
			{
				//setIcon(grv.isCollapsed() ? getClosedIcon() : getOpenIcon());
				setIcon(getClosedIcon());
			}

			setText(grp.getName());
			setToolTipText(grp.getComment());
		}
		else if (obj instanceof PacketView)
		{
			Packet pkt = ((PacketView) obj).getData();

			Color textColor;
			switch (pkt.getFlow())
			{
			case Packet.FLOW_C2S:
				setIcon(PacketView.ImgC2S);
				textColor = PacketView.NameColorC2S;
				break;
			case Packet.FLOW_S2C:
				setIcon(PacketView.ImgS2C);
				textColor = PacketView.NameColorS2C;
				break;
			case Packet.FLOW_INTER:
				setIcon(PacketView.ImgInter);
				textColor = PacketView.NameColorINTER;
				break;
			default:
				throw new RuntimeException("Invalid packet flow " + pkt.getFlow());
			}

			setForeground((hasFocus || sel) ? Color.white : textColor);
			setText(pkt.getName());
			setToolTipText(pkt.getComment());
		}

		return this;
	}
}

class TreeTransferHandler extends TransferHandler
{
	private DataFlavor m_nodesFlavor;
	private DataFlavor[] m_flavors = new DataFlavor[1];
	private DefaultMutableTreeNode m_curDragNode;

	public TreeTransferHandler()
	{
		try
		{
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" +
				javax.swing.tree.DefaultMutableTreeNode[].class.getName() + "\"";
			m_nodesFlavor = new DataFlavor(mimeType);
			m_flavors[0] = m_nodesFlavor;
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	public boolean canImport(TransferHandler.TransferSupport support)
	{
		if(!support.isDrop())
		{
			return false;
		}

		support.setShowDropLocation(true);
		if(!support.isDataFlavorSupported(m_nodesFlavor))
		{
			return false;
		}

		JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
		TreePath tpath = dl.getPath();
		DefaultMutableTreeNode target = (DefaultMutableTreeNode)tpath.getLastPathComponent();

		if (target.getUserObject() instanceof PacketView)
		{
			// packet cannot have a child.
			return false;
		}

		if (target.getLevel() == 0)
		{
			if (m_curDragNode.getUserObject() instanceof PacketView)
			{
				// Packet cannot go to root
				return false;
			}
		}
		else
		{
			if (m_curDragNode.getUserObject() instanceof PacketGroupView)
			{
				// PacketGroup can only move to root
				return false;
			}
		}

		if(support.getDropAction() == TransferHandler.COPY)
		{
			if (m_curDragNode.getUserObject() instanceof PacketGroupView)
			{
				// PacketGroup cannot be copied.
				return false;
			}
		}

		return true;
	}

	protected Transferable createTransferable(JComponent c)
	{
		JTree tree = (JTree) c;
		TreePath[] paths = tree.getSelectionPaths();
		if (paths == null || paths.length != 1)
			return null;

		m_curDragNode = (DefaultMutableTreeNode) paths[0].getLastPathComponent();

		DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[]{m_curDragNode};
		return new NodesTransferable(nodes);
	}

	public boolean importData(TransferHandler.TransferSupport support)
	{
		if(!canImport(support))
		{
			return false;
		}

		// Extract transfer data.
		DefaultMutableTreeNode[] nodes = null;
		try
		{
			Transferable t = support.getTransferable();
			nodes = (DefaultMutableTreeNode[])t.getTransferData(m_nodesFlavor);
		}
		catch(Exception ex)
		{
			System.out.println("DnD Exception: " + ex.getMessage());
			return false;
		}

		if (nodes.length != 1 || nodes[0] != m_curDragNode)
		{
			throw new RuntimeException("DnD data mismatch");
		}

		// Get drop location info.
		JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
		int childIndex = dl.getChildIndex();
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode)dl.getPath().getLastPathComponent();

		//JTree tree = (JTree)support.getComponent();
		//DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

		Object userObj = m_curDragNode.getUserObject();

		if(support.getDropAction() == TransferHandler.MOVE)
		{
			if (userObj instanceof PacketGroupView)
			{
				PacketGroupView grv = (PacketGroupView)userObj;
				if (MainFrame.getInstance().getDocument().isPacketGroupSamePos(grv.getData(), childIndex))
				{
					// no need to move
					return false;
				}
			}
			else if (userObj instanceof PacketView)
			{
				PacketView pkv = (PacketView)userObj;
				PacketGroupView grv = (PacketGroupView)parent.getUserObject();

				if (grv.getData().isSamePos(pkv.getData(), childIndex))
				{
					// no need to move
					return false;
				}

				grv.movePacketTo(pkv, childIndex);
				return true;
			}
			else
			{
				// unknown object
				return false;
			}
		}
		else if(support.getDropAction() == TransferHandler.COPY)
		{
			System.out.println("drop copy");
		}
		else
		{
			// unknown action
			return false;
		}

		return false;//true;
	}

/*
	protected void exportDone(JComponent source, Transferable data, int action)
	{
		if (action != TransferHandler.NONE)
		{
			MainFrame.getInstance().getForm().refreshPacketList();
		}

		System.out.println(String.format("exportDone(%d) called", action));
		if((action & MOVE) == MOVE)
		{
			JTree tree = (JTree)source;
			DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		}
	}
*/

	public int getSourceActions(JComponent c)
	{
		return COPY_OR_MOVE;
	}

	public String toString()
	{
		return getClass().getName();
	}

	public class NodesTransferable implements Transferable
	{
		DefaultMutableTreeNode[] m_nodes;

		public NodesTransferable(DefaultMutableTreeNode[] nodes)
		{
			m_nodes = nodes;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
		{
			if(!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
			return m_nodes;
		}

		public DataFlavor[] getTransferDataFlavors()
		{
			return m_flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			return m_nodesFlavor.equals(flavor);
		}
	}
}

public class PacketListPanel extends JPanel
{
	private JTree m_tree;
	private DefaultMutableTreeNode m_treeRoot;
	private DefaultTreeModel m_treeModel;

	private ArrayList<PacketGroupView> m_groupViews;

	private JPopupMenu m_popupMenu;

	public PacketListPanel(JTree tree)
	{
		setLayout(null);

		m_groupViews = new ArrayList<PacketGroupView>();

		setupTree(tree);

		m_popupMenu = new JPopupMenu();
		JMenuItem miAddGroup = new JMenuItem("Add PacketGroup", 'g');
		miAddGroup.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				addPacketGroup();
			}
		});
		m_popupMenu.add(miAddGroup);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (Util.isRightMouseButton(e))
				{
					// empty area of PacketView list
					m_popupMenu.show(PacketListPanel.this, e.getX(), e.getY());
				}
			}
		});
	}

	private void setupTree(JTree tree)
	{
		ToolTipManager.sharedInstance().registerComponent(tree);

		m_tree = tree;
		m_treeRoot = new DefaultMutableTreeNode();
		m_treeModel = new DefaultTreeModel(m_treeRoot);
		tree.setModel(m_treeModel);

		tree.setCellRenderer(new PacketTreeCellRenderer());

		tree.setDragEnabled(true);
		tree.setDropMode(DropMode.ON_OR_INSERT);
		tree.setTransferHandler(new TreeTransferHandler());

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
				onTreeNodeSelected(node.getUserObject());
			}
		});

		tree.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				if (Util.isRightMouseButton(e))
				{
					TreePath tpath = m_tree.getPathForLocation(e.getX(), e.getY());
					m_tree.setSelectionPath(tpath);

					if (tpath == null)
					{
						m_popupMenu.show(m_tree, e.getX(), e.getY());
					}
					else
					{
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) tpath.getLastPathComponent();
						showTreeContextMenu(node.getUserObject(), e.getPoint());
					}
				}
			}
		});
	}

	private void showTreeContextMenu(Object obj, Point pt)
	{
		if (obj instanceof PacketGroupView)
		{
			PacketGroupView grv = (PacketGroupView) obj;
			grv.showItemMenu(m_tree, pt);
		}
		else if (obj instanceof PacketView)
		{
			PacketView pkv = (PacketView)obj;
			pkv.showItemMenu(m_tree, pt);
		}
	}

	private void onTreeNodeSelected(Object obj)
	{
		if (obj instanceof PacketGroupView)
		{
			PacketGroupView grv = (PacketGroupView) obj;
			MainFrame.getInstance().getForm().assurePacketVisible(grv.getBounds());
		}
		else if (obj instanceof PacketView)
		{
			PacketView pkv = (PacketView) obj;
			if (pkv.getParentGroupView().isCollapsed())
			{
				MainFrame.getInstance().getForm().assurePacketVisible(pkv.getParentGroupView().getBounds());
			}
			else
			{
				MainFrame.getInstance().getForm().assurePacketVisible(pkv.getBounds());
			}
		}
		else
		{
			Util.printf("Invalid tree user-data: %s", obj);
		}
	}

	public void addPacketGroup()
	{
		PacketGroup grp = PacketGroup.createNew();
		PacketGroupDlg dlg = new PacketGroupDlg(grp);
		if (dlg.doModal())
		{
			Protocol doc = MainFrame.getInstance().getDocument();
			doc.getPacketGroups().add(grp);

			PacketGroupView vwGrp = new PacketGroupView(grp);
			m_treeRoot.add(vwGrp.getTreeNode());

			m_groupViews.add(vwGrp);
			add(vwGrp);

			m_treeModel.reload();
			expandTree();

			layoutSubviews();
		}
	}

	public void addPacketOnLastGroup()
	{
		if (m_groupViews.isEmpty())
		{
			MainFrame.showMsgBox("Please add a Packet Group first", JOptionPane.INFORMATION_MESSAGE);
		}
		else
		{
			m_groupViews.get(m_groupViews.size() -1).addNewPacketAt(-1);
		}
	}

	public void deleteGroup(PacketGroupView grv)
	{
		if (!grv.getData().getPackets().isEmpty())
			throw new RuntimeException("PacketGroup not empty");

		Protocol doc = MainFrame.getInstance().getDocument();

		remove(grv);
		m_groupViews.remove(grv);
		m_treeRoot.remove(grv.getTreeNode());
		doc.getPacketGroups().remove(grv.getData());

		m_treeModel.reload();
		expandTree();
		layoutSubviews();
	}

	public void expandTree()
	{
		Enumeration itr = m_treeRoot.breadthFirstEnumeration();
		while(itr.hasMoreElements())
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) itr.nextElement();
			if(!node.isLeaf())
			{
				m_tree.expandPath(new TreePath(node.getPath()));
			}
		}
	}

	public void initData()
	{
		if (!m_groupViews.isEmpty())
		{
			m_treeRoot.removeAllChildren();
			m_treeModel.reload();

			m_groupViews.clear();
			removeAll();
			repaint();
		}

		Protocol doc = MainFrame.getInstance().getDocument();

		for (PacketGroup grp : doc.getPacketGroups())
		{
			PacketGroupView vwGrp = new PacketGroupView(grp);
			m_treeRoot.add(vwGrp.getTreeNode());

			m_groupViews.add(vwGrp);
			add(vwGrp);

			for (Packet pkt : grp.getPackets())
			{
				PacketView vwPkt = new PacketView(pkt, vwGrp);
				vwGrp.addPacketView(vwPkt);
				add(vwPkt);
			}
		}

		expandTree();
	}

	public void layoutSubviews()
	{
//		m_treeModel.reload();

		// (DEBUG) data validation
		{
			Protocol doc = MainFrame.getInstance().getDocument();

			int iGrp = 0;
			for (PacketGroup grp : doc.getPacketGroups())
			{
				PacketGroupView vwGrp = m_groupViews.get(iGrp++);
				if (vwGrp.getData() != grp)
					throw new RuntimeException("PacketGroup data mismatch");

				int iPkt = 0;
				for (Packet pkt : grp.getPackets())
				{
					PacketView vwPkt = vwGrp.getPacketViews().get(iPkt++);
					if (vwPkt.getData() != pkt)
						throw new RuntimeException("Packet data mismatch");
				}
			}
		}

		int width = getWidth();
		int y = 0;
		for (PacketGroupView grv : m_groupViews)
		{
			grv.recalcLayout(width, false);
			grv.setBounds(0, y, width, grv.getHeight());
			//System.out.println(String.format("GRV bound: %d,%d,%d,%d", 0, y, width, grv.getHeight()));
			y += grv.getHeight();

			if (grv.isCollapsed())
			{
				for (PacketView pkv : grv.getPacketViews())
				{
					pkv.setVisible(false);
				}
			}
			else
			{
				for (PacketView pkv : grv.getPacketViews())
				{
					pkv.setVisible(true);
					pkv.recalcLayout(width, false);
					pkv.setBounds(0, y, width, pkv.getHeight());
					//System.out.println(String.format("PKV bound: %d,%d,%d,%d", 0, y, width, pkv.getHeight()));
					y += pkv.getHeight();
				}
			}
		}

		setSize(width, y);
		setPreferredSize(getSize());
	}

}
