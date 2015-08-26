package bpdtool.gui;

import bpdtool.Main;
import bpdtool.Util;

import javax.swing.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import bpdtool.data.Protocol;
import com.apple.eawt.*;
import com.apple.eawt.AppEvent.AboutEvent;


class PdFileFilter extends javax.swing.filechooser.FileFilter
{
    @Override
    public boolean accept(File f)
    {
        if(f.isDirectory())
            return false;

        String s = f.getName();
        int i = s.lastIndexOf('.');

        if(i > 0 && i < s.length() -1)
        {
            if(s.substring(i+1).toLowerCase().equals("xml"))
                return true;
        }

        return false;
    }

    @Override
    public String getDescription()
    {
        return "Protocol XML file (*.xml)";
    }
}

public class MainFrame extends JFrame
    implements DropTargetListener
{
    public static final String APP_TITLE = "Binary Protocol Designer";
    public static final String APP_VERSION = "0.1";

    private static MainFrame s_this;
    private static String s_startupFilenameToEdit;

    private MainForm m_form;
    private Protocol m_doc;

    public static MainFrame getInstance()
    {
        return s_this;
    }

    private MainFrame()
    {
        s_this = this;

        setTitle(APP_TITLE);

        m_form = new MainForm();
        setContentPane(m_form.rootPanel);
        setSize(800, 480);

        setupMenuBar();

        new DropTarget(this, this);

        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent winEvt)
            {
                onMenuFileExit();
            }
        });

        if(s_startupFilenameToEdit != null)
        {
            if(loadFile(s_startupFilenameToEdit))
                return;
        }

        // set new document
        m_doc = new Protocol();
        updateDocument();
    }

    private JMenuItem m_menuFileOpen;

    private void setupMenuBar()
    {
        JMenu fileMenu = new JMenu("File");
        {
            JMenuItem miFileNew = new JMenuItem("New", 'n');
            miFileNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
            miFileNew.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuFileNew();
                }
            });
            fileMenu.add(miFileNew);

            JMenuItem miFileOpen = new JMenuItem("Open", 'o');
            miFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
            miFileOpen.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuFileOpen();
                }
            });
            fileMenu.add(miFileOpen);

            JMenuItem miFileSave = new JMenuItem("Save", 's');
            miFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
            miFileSave.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuFileSave();
                }
            });
            fileMenu.add(miFileSave);

            JMenuItem miFileSaveAs = new JMenuItem("Save As", 'a');
            miFileSaveAs.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuFileSaveAs();
                }
            });
            fileMenu.add(miFileSaveAs);

            fileMenu.addSeparator();
            JMenuItem miFileExport = new JMenuItem("Export", 'e');
            miFileExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
            miFileExport.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuFileExport();
                }
            });
            fileMenu.add(miFileExport);

            JMenuItem miCopyNetstreamH = new JMenuItem("Copy netstream.h file to", 'c');
            miCopyNetstreamH.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuFileCopyNetstreamH();
                }
            });
            fileMenu.add(miCopyNetstreamH);

            fileMenu.addSeparator();
            JMenuItem miFileExit = new JMenuItem("Exit", 'x');
            miFileExit.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuFileExit();
                }
            });
            fileMenu.add(miFileExit);
        }

        JMenu editMenu = new JMenu("Edit");
        {
            JMenuItem miEditPacketStages = new JMenuItem("Edit Packet Stages");
            miEditPacketStages.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuEditPacketStages();
                }
            });
            editMenu.add(miEditPacketStages);

            JMenuItem miAddPacketGroup = new JMenuItem("Add Packet Group");
            miAddPacketGroup.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    m_form.onMenuAddPacketGroup();
                }
            });
            editMenu.add(miAddPacketGroup);

            JMenuItem miAddPacket = new JMenuItem("Add Packet");
            miAddPacket.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    m_form.onMenuAddPacketOnLastGroup();
                }
            });
            editMenu.add(miAddPacket);

            editMenu.addSeparator();

            JMenuItem miAddStruct = new JMenuItem("Add Struct");
            miAddStruct.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    m_form.onMenuAddStruct();
                }
            });
            editMenu.add(miAddStruct);

            JMenuItem miAddBlindClass= new JMenuItem("Add Blind Class");
            miAddBlindClass.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    m_form.onMenuAddBlindClass();
                }
            });
            editMenu.add(miAddBlindClass);

            editMenu.addSeparator();

            JMenuItem miAddDefine = new JMenuItem("Add Define");
            miAddDefine.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    m_form.onMenuAddDefine();
                }
            });
            editMenu.add(miAddDefine);

            JMenuItem miAddEnum = new JMenuItem("Add Enum");
            miAddEnum.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    m_form.onMenuAddEnum();
                }
            });
            editMenu.add(miAddEnum);

            editMenu.addSeparator();

            JMenuItem miAddExportEntry = new JMenuItem("Add Export Entry");
            miAddExportEntry.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    m_form.onMenuAddExportEntry();
                }
            });
            editMenu.add(miAddExportEntry);
        }

        JMenu helpMenu = new JMenu("Help");
        {
            JMenuItem miHelpCheckUpdate = new JMenuItem("Check for Update ...");
            miHelpCheckUpdate.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuHelpCheckUpdate();
                }
            });
            helpMenu.add(miHelpCheckUpdate);

            JMenuItem miHelpAbout = new JMenuItem("About ...");
            miHelpAbout.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    onMenuHelpAbout();
                }
            });
            helpMenu.add(miHelpAbout);

        }

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void onMenuFileNew()
    {
        if(JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(this,
            "Discard the current protocol?", APP_TITLE,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE))
        {
            m_doc = new Protocol();
            updateDocument();
        }
    }

    private void onMenuFileOpen()
    {
        if(!confirmNotDirty())
            return;

        JFileChooser fc = new JFileChooser();
        fc.setDragEnabled(true);
        fc.setMultiSelectionEnabled(false);
        fc.setDialogTitle("Open Protocol XML file");
        if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            loadFile(fc.getSelectedFile().getPath());
        }
    }

    private void onMenuFileSave()
    {
        if (Util.isNullOrEmpty(m_doc.getFilename()))
        {
            onMenuFileSaveAs();
        }
        else
        {
            try
            {
                m_form.updateToDocument();
                m_doc.save();
                //lbBaseDir.setText(s_doc.Basedir);
                showMsgBox("Saved: " + m_doc.getFilename(), JOptionPane.INFORMATION_MESSAGE);
                updateTitle();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                showMsgBox("Save failed: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onMenuFileSaveAs()
    {
        JFileChooser fc = new JFileChooser();
        fc.addChoosableFileFilter(new PdFileFilter());
        if(fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        String pathname;
        String filename = fc.getSelectedFile().getName();
        if(filename.indexOf('.') < 0)
            pathname = fc.getSelectedFile().getAbsolutePath() + ".xml";
        else
            pathname = fc.getSelectedFile().getAbsolutePath();

        m_doc.setFilename(pathname);
        onMenuFileSave();
    }

    private void onMenuFileExport()
    {
        m_form.updateToDocument();
        ExportDlg.doModal();
    }

    private void onMenuFileCopyNetstreamH()
    {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("."));
        fc.setDialogTitle("Select a directory");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

	    try
	    {
		    copyResourceTo("netstream.h", fc.getSelectedFile());
	    }
	    catch(Exception ex)
	    {
		    showMsgBox("Failed: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
	    }
    }

    private void onMenuFileExit()
    {
        if(confirmNotDirty())
        {
            dispose();

            if (Util.isMacOSX())
            {
                // FIXME: on OSX, app does not quit, so call exit()
                System.exit(0);
            }
        }
    }

    private void onMenuEditPacketStages()
    {
        EditStageDlg dlg = new EditStageDlg(this);
        if(dlg.doModal())
        {
            m_form.refreshPacketList();
        }
    }

    private void onMenuHelpCheckUpdate()
    {
        showMsgBox("TODO", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onMenuHelpAbout()
    {
        AboutDlg.doModal();
    }

    public MainForm getForm()
    {
        return m_form;
    }

    public Protocol getDocument()
    {
        return m_doc;
    }

    private boolean confirmNotDirty()
    {
        m_form.updateToDocument();
        if(m_doc.isDirty())
        {
            int opt = JOptionPane.showConfirmDialog(this,
                "Protocol is modified. Save it?",
                APP_TITLE, JOptionPane.YES_NO_CANCEL_OPTION);

            if(JOptionPane.CANCEL_OPTION == opt)
                return false;

            if(JOptionPane.YES_OPTION == opt)
            {
                onMenuFileSave();
                return !m_doc.isDirty();
            }
        }

        return true;
    }

    private boolean loadFile(String path)
    {
        Protocol doc;

        try
        {
            doc = new Protocol(path);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                APP_TITLE + " - " + "Loading failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        m_doc = doc;
        updateDocument();
        return true;
    }

    private void updateTitle()
    {
        if (Util.isNullOrEmpty(m_doc.getFilename()))
        {
            setTitle(APP_TITLE);
        }
        else
        {
            setTitle(APP_TITLE + " - " + m_doc.getFilename());
        }
    }

    private void updateDocument()
    {
        updateTitle();
        m_form.updateFromDocument();
    }

    @Override
    public void dragEnter(DropTargetDragEvent dropTargetDragEvent)
    {
        //System.out.println("Drag Enter");
    }

    @Override
    public void dragOver(DropTargetDragEvent dropTargetDragEvent)
    {
        //System.out.println("Drag Over");
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dropTargetDragEvent)
    {
        //System.out.println("Drag ActionChanged");
    }

    @Override
    public void dragExit(DropTargetEvent dropTargetEvent)
    {
        //System.out.println("Drag Exit");
    }

    @Override
    public void drop(DropTargetDropEvent dtde)
    {
        try
        {
            // Ok, get the dropped object and try to figure out what it is
            Transferable tr = dtde.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();
            for (int i = 0; i < flavors.length; i++)
            {
                //System.out.println("Possible flavor: " + flavors[i].getMimeType());
                // Check for file lists specifically
                if (flavors[i].isFlavorJavaFileListType())
                {
                    // Great!  Accept copy drops...
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    //tbMemo.setText("Successful file list drop.\n\n");

                    // And add the list of file names to our text area
                    java.util.List list = (java.util.List) tr.getTransferData(flavors[i]);
                    {
                        String filename = list.get(0).toString();

                        if(!confirmNotDirty())
                            break;

                        if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                            String.format("Load protocol file '%s' ?", filename),
                            APP_TITLE, JOptionPane.YES_NO_OPTION))
                        {
                            loadFile(filename);
                        }
                    }

                    // If we made it this far, everything worked.
                    dtde.dropComplete(true);
                    return;
                } // Ok, is it another Java object?
                else if (flavors[i].isFlavorSerializedObjectType())
                {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    //tbMemo.setText("Successful text drop.\n\n");
                    //Object o = tr.getTransferData(flavors[i]);
                    //tbMemo.append("Object: " + o);
                    dtde.dropComplete(true);
                    return;
                } // How about an input stream?
                else if (flavors[i].isRepresentationClassInputStream())
                {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
/*
					tbMemo.setText("Successful text drop.\n\n");
					tbMemo.read(new InputStreamReader(
						(InputStream) tr.getTransferData(flavors[i])),
						"from system clipboard");
 */
                    dtde.dropComplete(true);
                    return;
                }
            }
            // Hmm, the user must not have dropped a file list
//			System.out.println("Drop failed: " + dtde);
            dtde.rejectDrop();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            dtde.rejectDrop();
        }
    }

    public static void showMsgBox(String msg, int msgType)
    {
        JOptionPane.showMessageDialog(s_this, msg, APP_TITLE, msgType);
    }

    public static int showConfirmBox(String msg, int msgType)
    {
        return JOptionPane.showConfirmDialog(s_this, msg, APP_TITLE, msgType);
    }

    public static void copyResourceTo(String path, File destDir) throws Exception
    {
        InputStream isr = s_this.getClass().getResourceAsStream("/" + path);
        if (isr != null)
        {
            byte[] buff = new byte[isr.available()];
            isr.read(buff);
            isr.close();

            File destFile = new File(destDir, path);
            FileOutputStream fos = new FileOutputStream(destFile);
            fos.write(buff);
            fos.close();

            showMsgBox("File saved: " + destFile.getAbsolutePath(), JOptionPane.INFORMATION_MESSAGE);
        }
        else
        {
            throw new Exception("Resource not found: " + path);
        }
    }


    public static void launch(String filename)
    {
        s_startupFilenameToEdit = filename;

        try
        {
            if (Util.isMacOSX())
            {
                System.setProperty("Quaqua.TextComponent.autoSelect", "true");
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_TITLE);   // not working

                Application macApp = Application.getApplication();

                macApp.setAboutHandler(new AboutHandler()
                {
                    public void handleAbout(AboutEvent aboutEvent)
                    {
                        s_this.onMenuHelpAbout();
                    }
                });

                macApp.setQuitHandler(new QuitHandler()
                {
                    @Override
                    public void handleQuitRequestWith(AppEvent.QuitEvent quitEvent, QuitResponse quitResponse)
                    {
                        s_this.onMenuFileExit();
                    }
                });

                //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            else
            {
                UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName());
                    //UIManager.getSystemLookAndFeelClassName());
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return;
        }

        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                MainFrame frm = new MainFrame();
                frm.setVisible(true);
            }
        });
    }

}
