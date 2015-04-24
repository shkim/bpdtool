package bpdtool.gui;

import bpdtool.Util;
import bpdtool.data.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;


class ExportTableModel implements TableModel
{

    @Override
    public int getColumnCount()
    {
        return 5;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        switch(columnIndex)
        {
        case 0:
            return "Enabled";
        case 1:
            return "Language and Role";
        case 2:
            return "File Encoding";
        case 3:
            return "NewLine character";
        case 4:
            return "Output Directory";
        }

        throw new RuntimeException("getColumnName: Invalid ExportEntry column: " + columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        if (columnIndex == 0)
            return Boolean.class;

        return String.class;
    }

    @Override
    public int getRowCount()
    {
        return MainFrame.getInstance().getDocument().getExportEntries().size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        Protocol.ExportEntry entry = MainFrame.getInstance().getDocument().getExportEntries().get(rowIndex);

        switch (columnIndex)
        {
        case 0:
            return Boolean.valueOf(entry.enabled);
        case 1:
            return entry.langAndRole;
        case 2:
            return entry.encoding;
        case 3:
            return entry.nlchar;
        case 4:
            return entry.outputDir;
        }

        throw new RuntimeException("getValueAt: Invalid ExportEntry column: " + columnIndex);
    }

    @Override
    public void setValueAt(Object obj, int rowIndex, int columnIndex)
    {
        Protocol.ExportEntry entry = MainFrame.getInstance().getDocument().getExportEntries().get(rowIndex);

        String value = obj.toString();
        switch (columnIndex)
        {
        case 0:
            entry.enabled = Boolean.parseBoolean(value);
            break;
        case 1:
            entry.langAndRole = Protocol.findLanguageAndRole(value);
            break;
        case 2:
            entry.encoding = value;
            break;
        case 3:
            entry.nlchar = value;
            break;
        case 4:
            entry.outputDir = value;
            break;

        default:
            throw new RuntimeException("setValueAt: Invalid ExportEntry column: " + columnIndex);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return true;
    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener)
    {
    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener)
    {
    }
}

class LangAndRoleCellEditor extends DefaultCellEditor
{
    private JComboBox m_combo;

    public LangAndRoleCellEditor(JComboBox combo)
    {
        super(combo);
        m_combo = combo;
    }

    public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int rowIndex, int colIndex )
    {
        Protocol.ExportEntry entry = MainFrame.getInstance().getDocument().getExportEntries().get(rowIndex);
        m_combo.setSelectedIndex(entry.langAndRole.getIndex());

        return m_combo;
    }
}

public class MainForm implements ActionListener
{
    JPanel rootPanel;
    private JTabbedPane m_tabs;
    private JTree m_treePackets;
    private JScrollPane m_scrPackets;
    private JScrollPane m_scrUserTypes;
    private JScrollPane m_scrConstants;
    private JTextField m_tfTitle;
    private JTextField m_tfAuthor;
    private JTextArea m_taMemo;
    private JTable m_tblExports;
    private JScrollPane m_scrConfigs;
    private JScrollPane m_scrLangOpts;
    private JLabel m_lbBaseDir;

    private ConfigForm m_panelConfigs;
    private LangOptsForm m_panelLangOpts;
    private PacketListPanel m_panelPackets;
    private ViewListPanel m_panelUserTypes;
    private ViewListPanel m_panelConstants;

    private JPopupMenu m_popupExportTblMenu;
    private Timer m_scrollTimer;
    private int m_nScrollDest;

    public MainForm()
    {
        m_scrollTimer = new Timer(60, this);


        m_panelConfigs = new ConfigForm();
        setupScrollPane(m_scrConfigs, m_panelConfigs.rootPanel, true);

        m_panelLangOpts = new LangOptsForm();
        setupScrollPane(m_scrLangOpts, m_panelLangOpts.rootPanel, true);

        m_panelPackets = new PacketListPanel(m_treePackets);
        setupScrollPane(m_scrPackets, m_panelPackets, false);

        m_panelUserTypes = new ViewListPanel(true);
        setupScrollPane(m_scrUserTypes, m_panelUserTypes, false);

        m_panelConstants = new ViewListPanel(false);
        setupScrollPane(m_scrConstants, m_panelConstants, false);

        setupExportTable();

        m_tabs.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent componentEvent)
            {
                super.componentResized(componentEvent);
                onResized();
            }
        });

        m_tabs.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent changeEvent)
            {
                //JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
                //int index = sourceTabbedPane.getSelectedIndex();
                //System.out.println("Tab changed to: " + sourceTabbedPane.getTitleAt(index));
                onResized();
            }
        });

    }

    private void setupScrollPane(JScrollPane scr, JPanel pnl, boolean isFormPanel)
    {
        if (isFormPanel)
        {
            scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        }
        else
        {
            scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        }

        scr.setViewportView(pnl);
        scr.setWheelScrollingEnabled(true);
        scr.getVerticalScrollBar().setUnitIncrement(32);
    }

    private void setupExportTable()
    {
        m_popupExportTblMenu = new JPopupMenu();
        JMenuItem miAddExportEntry = new JMenuItem("Add Export Entry", 'a');
        miAddExportEntry.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                onMenuAddExportEntry();
            }
        });
        m_popupExportTblMenu.add(miAddExportEntry);

        MouseAdapter addEntryLsnr = new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (Util.isRightMouseButton(e))
                    m_popupExportTblMenu.show(m_tblExports, e.getX(), e.getY());
            }
        };
        m_tblExports.addMouseListener(addEntryLsnr);
        m_tblExports.getParent().addMouseListener(addEntryLsnr);

        m_tblExports.setModel(new ExportTableModel());
        m_tblExports.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_tblExports.setRowHeight(20);


        TableColumnModel cm = m_tblExports.getColumnModel();
        cm.getColumn(0).setPreferredWidth(25);
        cm.getColumn(1).setPreferredWidth(120);
        cm.getColumn(2).setPreferredWidth(60);
        cm.getColumn(3).setPreferredWidth(60);
        cm.getColumn(4).setPreferredWidth(150);

        TableColumn langColumn = cm.getColumn(1);
        JComboBox cbLangs = new JComboBox(Protocol.getLanguageAndRoles().toArray());
        langColumn.setCellEditor(new LangAndRoleCellEditor(cbLangs));

        JComboBox cbNLs = new JComboBox();
        cbNLs.addItem("LF");
        cbNLs.addItem("CR+LF");
        cbNLs.addItem("CR");
        cm.getColumn(3).setCellEditor(new DefaultCellEditor(cbNLs));
    }

    public void onMenuAddPacketGroup()
    {
        m_tabs.setSelectedIndex(3);
        getPacketsPanel().addPacketGroup();
    }

    public void onMenuAddPacketOnLastGroup()
    {
        m_tabs.setSelectedIndex(3);
        getPacketsPanel().addPacketOnLastGroup();
    }

    public void onMenuAddStruct()
    {
        m_tabs.setSelectedIndex(4);
        m_panelUserTypes.addStruct();
    }

    public void onMenuAddBlindClass()
    {
        m_tabs.setSelectedIndex(4);
        m_panelUserTypes.addBlindClass();
    }

    public void onMenuAddDefine()
    {
        m_tabs.setSelectedIndex(5);
        m_panelConstants.addConstant(false);
    }

    public void onMenuAddEnum()
    {
        m_tabs.setSelectedIndex(5);
        m_panelConstants.addConstant(true);
    }

    public void onMenuAddExportEntry()
    {
        m_tabs.setSelectedIndex(0);
        MainFrame.getInstance().getDocument().addExportEntry();
        m_tblExports.updateUI();
    }

    public JTree getPacketsTree()
    {
        return m_treePackets;
    }

    public void updateFromDocument()
    {
        Protocol doc = MainFrame.getInstance().getDocument();

        m_tfTitle.setText(doc.getConfig().Title);
        m_tfAuthor.setText(doc.getConfig().Author);
        m_taMemo.setText(doc.getConfig().Memo);
        m_lbBaseDir.setText(doc.getBaseDir());

        m_tblExports.updateUI();

        m_panelConfigs.updateFromDocument(doc);
        m_panelLangOpts.updateFromDocument(doc);

        m_panelPackets.initData();

        m_panelUserTypes.clear();
        for (UserType ut : doc.getUserTypes())
        {
            if (ut instanceof BlindClass)
            {
                m_panelUserTypes.addView(new BlindClassView((BlindClass) ut));
            }
            else if (ut instanceof Struct)
            {
                m_panelUserTypes.addView(new StructView((Struct) ut));
            }
        }

        m_panelConstants.clear();
        for (Constant ct : doc.getConstants())
        {
            m_panelConstants.addView(new ConstantView(ct));
        }

        // DEBUG
        //m_tabs.setSelectedIndex(3);
    }

    public void updateToDocument()
    {
        Protocol doc = MainFrame.getInstance().getDocument();

        doc.getConfig().Title = m_tfTitle.getText().trim();
        doc.getConfig().Author = m_tfAuthor.getText().trim();
        doc.getConfig().Memo = m_taMemo.getText().trim();

        m_panelConfigs.updateToDocument(doc);
        m_panelLangOpts.updateToDocument(doc);
    }

    public void refreshPacketList()
    {
        m_panelPackets.layoutSubviews();
    }

    public PacketListPanel getPacketsPanel()
    {
        return m_panelPackets;
    }

    private void onResized()
    {
        switch (m_tabs.getSelectedIndex())
        {
        case 3:
            refreshPacketList();
            break;
        case 4:
            m_panelUserTypes.layoutSubviews();
            break;
        case 5:
            m_panelConstants.layoutSubviews();
            break;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == m_scrollTimer)
        {
            JScrollBar vscr = m_scrPackets.getVerticalScrollBar();
            int scrollpos = vscr.getValue();

            int diff = Math.abs(m_nScrollDest - scrollpos);
            if(diff < 10)
            {
                vscr.setValue(m_nScrollDest);
                m_scrollTimer.stop();
            }
            else
            {
                diff = diff >> 1;
                if(m_nScrollDest < scrollpos)
                {
                    vscr.setValue(scrollpos - diff);
                }
                else
                {
                    vscr.setValue(scrollpos + diff);
                }
            }
        }
    }

    public void assurePacketVisible(Rectangle bounds)
    {
        //Rectangle rcItem = pkv.getBounds();
        JScrollBar vscr = m_scrPackets.getVerticalScrollBar();
        final int yScrollMax = m_panelPackets.getHeight() - vscr.getVisibleAmount();
        int curScrollPos = vscr.getValue();

        m_nScrollDest = bounds.y > yScrollMax ? yScrollMax : bounds.y;
        if(m_nScrollDest != curScrollPos)
            m_scrollTimer.start();
    }

}
