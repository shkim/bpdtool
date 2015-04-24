package bpdtool.gui;

import bpdtool.Util;
import bpdtool.data.BlindClass;
import bpdtool.data.ItemCommons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


class BlindClassPopupMenu
{
	private BlindClassView m_owner;
	private JPopupMenu m_popupMenu;
	private JMenuItem m_miProperty;
	private JMenuItem m_miCopy;
	private JMenuItem m_miDelete;
	private JMenuItem m_miMoveUp;
	private JMenuItem m_miMoveDown;

	public BlindClassPopupMenu()
	{
		m_miProperty = new JMenuItem("Edit Property", 'p');
		m_miCopy = new JMenuItem("Copy", 'c');
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
				m_owner.onItemCopy();
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

		m_miMoveDown.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_owner.onItemMoveDown();
			}
		});
	}

	public void show(BlindClassView owner, Point pt)
	{
		m_popupMenu.removeAll();

		m_popupMenu.add(m_miProperty);
		m_popupMenu.addSeparator();
		m_popupMenu.add(m_miCopy);
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
		m_popupMenu.show(owner, pt.x, pt.y);
	}
}

public class BlindClassView extends ItemView
{
	private static BlindClassPopupMenu s_popupBcMenu = new BlindClassPopupMenu();

	private BlindClass m_data;

	public BlindClassView(BlindClass d)
	{
		m_data = d;

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				onMouseClick(e);
			}
		});
	}

	private void onMouseClick(MouseEvent e)
	{
		if (Util.isRightMouseButton(e))
		{
			s_popupBcMenu.show(this, e.getPoint());
			return;
		}

		if(e.getClickCount() >= 2 && !e.isConsumed())
		{
			onMenuEditProp();
		}
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

		int xLabel = drawBigName(gr, m_data.getName(), ColorDarkRed);
		drawItemComment(gr, m_data.getComment(), xLabel);

		drawItemDescription(gr, Color.GRAY);
	}

}
