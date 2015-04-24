package bpdtool.gui;

import bpdtool.Util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

public class InplaceTextEditor
	implements FocusListener, ActionListener, KeyListener
{
	public interface Listener
	{
		void onInplaceTextChanged(ItemView_WithTable.HitResult hit, String text);
	}

	private static InplaceTextEditor s_this;

	private boolean m_isMacOSX;
	private JTextField m_tf;
	private JTextArea m_ta;
	private JTextComponent m_current;
	private Popup m_popup;

	private Listener m_lsnr;
	private ItemView_WithTable.HitResult m_hit;

	private InplaceTextEditor()
	{
		m_isMacOSX = Util.isMacOSX();
		m_popup = null;

		m_tf = new JTextField();
		m_tf.setBorder(null);
		m_tf.addActionListener(this);
		m_tf.addKeyListener(this);

		m_ta = new JTextArea();
		m_ta.setBorder(null);
		m_ta.addKeyListener(this);

		Insets zero = new Insets(0,0,0,0);
		m_tf.setMargin(zero);
		m_ta.setMargin(zero);
		m_tf.setLocation(0,0);
	}

	public static void show(Container owner, String str, ItemView_WithTable.HitResult hit)
	{
		if(s_this == null)
			s_this = new InplaceTextEditor();

		if (s_this.m_isMacOSX)
			s_this._showOSX(owner, str, hit);
		else
			s_this._show(owner, str, hit);
	}

	private void _show(Container owner, String str, ItemView_WithTable.HitResult hit)
	{
		if(m_popup != null)
		{
			closePopup(false);
		}

		m_lsnr = (Listener) owner;
		m_hit = hit;

		int yPosAdd;

		if (hit.area == ItemView_WithTable.HitResult.DESCRIPTION)
		{
			yPosAdd = 2;
			m_current = m_ta;
			m_ta.setText(str);
			m_ta.selectAll();

			//m_current.setPreferredSize(hit.box.getSize());
			m_current.setPreferredSize(new Dimension(hit.box.width, hit.box.height -3));
//			m_current.setSize(hit.box.width, hit.box.height);
		}
		else
		{
			yPosAdd = 1;
			m_current = m_tf;
			m_tf.setText(str);
			m_tf.selectAll();
			m_tf.setHorizontalAlignment((hit.area == ItemView_WithTable.HitResult.COMMENT) ? JTextField.LEFT : JTextField.CENTER);

			m_current.setPreferredSize(hit.box.getSize());
//			m_current.setSize(hit.box.width, hit.box.height +15);

		}

//		m_current.setPreferredSize(hit.box.getSize());
		//m_current.setPreferredSize(new Dimension(hit.box.width, hit.box.height -1));
//		m_current.setSize(hit.box.width, hit.box.height);

		Point ptBase = owner.getLocationOnScreen();
		m_popup = PopupFactory.getSharedInstance().getPopup(owner, m_current,
			ptBase.x + hit.box.x, ptBase.y + hit.box.y + yPosAdd);
		m_popup.show();

		m_current.addFocusListener(s_this);
		m_current.requestFocus();
	}

	private void _showOSX(Container owner, String str, ItemView_WithTable.HitResult hit)
	{
		m_lsnr = (Listener) owner;
		m_hit = hit;

		int cyBox;

		if (hit.area == ItemView_WithTable.HitResult.DESCRIPTION)
		{
			m_current = m_ta;
			m_ta.setText(str);
			m_ta.selectAll();
			cyBox = hit.box.height -3;
		}
		else
		{
			m_current = m_tf;
			m_tf.setText(str);
			m_tf.selectAll();
			m_tf.setHorizontalAlignment((hit.area == ItemView_WithTable.HitResult.COMMENT) ? JTextField.LEFT : JTextField.CENTER);
			cyBox = hit.box.height - 1;
		}

		m_current.addFocusListener(s_this);
		m_current.setBounds(hit.box.x + 1, hit.box.y + 2, hit.box.width - 2, cyBox);
		owner.add(m_current);
		m_current.requestFocusInWindow();
	}

	public void focusGained(FocusEvent e)
	{
		//System.out.println("focusGained");
	}

	public void focusLost(FocusEvent e)
	{
		//System.out.println("focusLost");
		closePopup(false);
	}

	public void actionPerformed(ActionEvent e)
	{
		//System.out.println("actionPerformed");
		closePopup(true);
	}

	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	public void keyPressed(KeyEvent e)
	{
		if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			closePopup(false);
		}
		else if(e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			if(m_ta.isVisible())
			{
				if(e.getModifiers() == 0)
					closePopup(true);
				else
					m_ta.append("\n");
			}
		}
	}

	private void closePopup(boolean bApply)
	{
		Util.printf("closePopup(%b)", bApply);
		if(m_isMacOSX)
		{
			m_current.removeFocusListener(this);
			Container parent = m_current.getParent();
			parent.remove(m_current);
			parent.repaint();
		}
		else
		{
			if(m_popup == null)
				return;

			m_current.removeFocusListener(this);
			m_popup.hide();
			m_popup = null;
		}

		if(bApply)
		{
			m_lsnr.onInplaceTextChanged(m_hit, m_current.getText());
		}
	}

}
