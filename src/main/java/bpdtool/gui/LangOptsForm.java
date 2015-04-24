package bpdtool.gui;

import bpdtool.data.Protocol;
import bpdtool.data.PxConfig;

import javax.swing.*;

public class LangOptsForm
{
	public JPanel rootPanel;
	private JTextField m_tfCppServerClass;
	private JCheckBox m_chkCppServerNamespace;
	private JCheckBox m_chkCppServerFileExt;
	private JTextField m_tfCppServerNamespace;
	private JTextField m_tfCppServerFileExt;
	private JTextArea m_taCppServerHeader;
	private JTextField m_tfCppClientClass;
	private JCheckBox m_chkCppClientNamespace;
	private JCheckBox m_chkCppClientFileExt;
	private JTextField m_tfCppClientFileExt;
	private JTextField m_tfCppClientNamespace;
	private JTextArea m_taCppClientHeader;
	private JCheckBox m_chkCppCapitalize;
	private JTextField m_tfAsClass;
	private JCheckBox m_chkAsPackage;
	private JCheckBox m_chkAsMergeAll;
	private JTextField m_tfAsPackage;
	private JTextField m_tfJsServerClass;
	private JTextField m_tfJsClientClass;
	private JCheckBox m_chkCppServerSkipCommon;
	private JCheckBox m_chkCppClientSkipCommon;
	private JCheckBox m_chkCppServerBE;
	private JTextField m_tfCppCommonFile;
	private JCheckBox m_chkCppClientBE;
	private JCheckBox m_chkCppNoDirectCast;
	private JCheckBox m_chkCppUseStdVector;

	public void updateFromDocument(Protocol doc)
	{
		PxConfig config = doc.getConfig();

		m_tfCppServerClass.setText(config.Cpp.Server.ClassName);
		m_tfCppServerNamespace.setText(config.Cpp.Server.Namespace);
		m_chkCppServerNamespace.setSelected(config.Cpp.Server.UseNamespace);
		m_tfCppServerFileExt.setText(config.Cpp.Server.FileExt);
		m_chkCppServerFileExt.setSelected(config.Cpp.Server.OverrideFileExt);
		m_taCppServerHeader.setText(config.Cpp.Server.CustomHeader);
		m_chkCppServerSkipCommon.setSelected(config.Cpp.Server.SkipGenerateCommon);
		m_chkCppServerBE.setSelected(config.Cpp.Server.SwapEndian);

		m_tfCppClientClass.setText(config.Cpp.Client.ClassName);
		m_tfCppClientNamespace.setText(config.Cpp.Client.Namespace);
		m_chkCppClientNamespace.setSelected(config.Cpp.Client.UseNamespace);
		m_tfCppClientFileExt.setText(config.Cpp.Client.FileExt);
		m_chkCppClientFileExt.setSelected(config.Cpp.Client.OverrideFileExt);
		m_taCppClientHeader.setText(config.Cpp.Client.CustomHeader);
		m_chkCppClientSkipCommon.setSelected(config.Cpp.Client.SkipGenerateCommon);
		m_chkCppClientBE.setSelected(config.Cpp.Client.SwapEndian);

		m_tfCppCommonFile.setText(config.Cpp.CommonFileName);
		m_chkCppCapitalize.setSelected(config.Cpp.CapitalizeMethodName);
		m_chkCppNoDirectCast.setSelected(config.Cpp.DisableDirectCasting);
		m_chkCppUseStdVector.setSelected(config.Cpp.UseStdVector);
		
		m_tfAsClass.setText(config.As3.Client.ClassName);
		m_tfAsPackage.setText(config.As3.Client.PackageName);
		m_chkAsPackage.setSelected(config.As3.Client.UsePackageName);
		m_chkAsMergeAll.setSelected(config.As3.Client.MergeAll);		
		
		m_tfJsServerClass.setText(config.Js.Server.ClassName);
		m_tfJsClientClass.setText(config.Js.Client.ClassName);
	}

	public void updateToDocument(Protocol doc)
	{
		PxConfig config = doc.getConfig();

		config.Cpp.Server.ClassName = m_tfCppServerClass.getText().trim();
		config.Cpp.Server.Namespace = m_tfCppServerNamespace.getText().trim();
		config.Cpp.Server.UseNamespace = m_chkCppServerNamespace.isSelected();
		config.Cpp.Server.FileExt = m_tfCppServerFileExt.getText().trim();
		config.Cpp.Server.OverrideFileExt = m_chkCppServerFileExt.isSelected();
		config.Cpp.Server.CustomHeader = m_taCppServerHeader.getText().trim();
		config.Cpp.Server.SkipGenerateCommon = m_chkCppServerSkipCommon.isSelected();
		config.Cpp.Server.SwapEndian = m_chkCppServerBE.isSelected();

		config.Cpp.Client.ClassName = m_tfCppClientClass.getText().trim();
		config.Cpp.Client.Namespace = m_tfCppClientNamespace.getText().trim();
		config.Cpp.Client.UseNamespace = m_chkCppClientNamespace.isSelected();
		config.Cpp.Client.FileExt = m_tfCppClientFileExt.getText().trim();
		config.Cpp.Client.OverrideFileExt = m_chkCppClientFileExt.isSelected();
		config.Cpp.Client.CustomHeader = m_taCppClientHeader.getText().trim();
		config.Cpp.Client.SkipGenerateCommon = m_chkCppClientSkipCommon.isSelected();
		config.Cpp.Client.SwapEndian = m_chkCppClientBE.isSelected();

		config.Cpp.CommonFileName = m_tfCppCommonFile.getText().trim();
		config.Cpp.CapitalizeMethodName = m_chkCppCapitalize.isSelected();
		config.Cpp.DisableDirectCasting = m_chkCppNoDirectCast.isSelected();
		config.Cpp.UseStdVector = m_chkCppUseStdVector.isSelected();

		config.As3.Client.ClassName = m_tfAsClass.getText().trim();
		config.As3.Client.PackageName = m_tfAsPackage.getText().trim();
		config.As3.Client.UsePackageName = m_chkAsPackage.isSelected();
		config.As3.Client.MergeAll = m_chkAsMergeAll.isSelected();

		config.Js.Server.ClassName = m_tfJsServerClass.getText().trim();
		config.Js.Client.ClassName = m_tfJsClientClass.getText().trim();
	}
}
