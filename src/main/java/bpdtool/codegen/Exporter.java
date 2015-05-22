package bpdtool.codegen;

import bpdtool.Util;
import bpdtool.data.*;
import bpdtool.gui.ExportDlg;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;

public class Exporter
{
	private Protocol m_doc;
	private ArrayList<CodeGenerator> m_preparedCodeGenerators;
	private BufferedTabWriter m_btw;
	private ExportDlg m_previewDlg;

	private int m_nMaxC2SPacketID;
	private int m_nMaxS2CPacketID;

	public Exporter(Protocol doc)
	{
		m_doc = doc;
	}

	public boolean prepare(ErrorLogger logger)
	{
		HashSet<Integer> langsSet = new HashSet<>();
		for (Protocol.ExportEntry ee : m_doc.getExportEntries())
		{
			if (ee.enabled)
				langsSet.add(Integer.valueOf(ee.langAndRole.getLanguage()));
		}

		if (langsSet.isEmpty())
		{
			logger.writeln("No enabled entry to export.");
			return false;
		}

		if (Util.isNullOrEmpty(m_doc.getConfig().Prefix.C2SPacketID))
			logger.writeln("C->S PacketID prefix is empty.");

		if (Util.isNullOrEmpty(m_doc.getConfig().Prefix.S2CPacketID))
			logger.writeln("S->C PacketID prefix is empty.");

		if (Util.isNullOrEmpty(m_doc.getConfig().Prefix.PacketStruct))
			logger.writeln("Packet struct prefix is empty.");

		if (Util.isNullOrEmpty(m_doc.getConfig().Prefix.SenderMethod))
			logger.writeln("Sender method prefix is empty.");

		if (Util.isNullOrEmpty(m_doc.getConfig().Prefix.BuilderMethod))
			logger.writeln("Builder method prefix is empty.");

		if (Util.isNullOrEmpty(m_doc.getConfig().Prefix.HandlerMethod))
			logger.writeln("Handler method prefix is empty.");

		// UserTypes check
		HashSet<String> uniqueNames = new HashSet<>();
		for (UserType ut : m_doc.getUserTypes())
		{
			if (uniqueNames.contains(ut.getName()))
			{
				logger.writeln("UserType identifier name '{0}' conflicts.", ut.getName());
			}
			else
			{
				uniqueNames.add(ut.getName());
			}

			if (ut instanceof Struct)
			{
				Struct st = (Struct)ut;
				st.prepareExport(logger);
			}
		}

		// Packets check
		uniqueNames.clear();
		m_nMaxC2SPacketID = 0;
		m_nMaxS2CPacketID = 0;
		HashSet<Integer> c2sIds = new HashSet<>();
		HashSet<Integer> s2cIds = new HashSet<>();
		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			String changeStartId = grp.getStartId();
			if (!Util.isNullOrEmpty(changeStartId))
			{
				int startId = Util.getStringNumber(changeStartId);
				m_nMaxC2SPacketID = startId;
				m_nMaxS2CPacketID = startId;
			}

			for (Packet pkt : grp.getPackets())
			{
				if (uniqueNames.contains(pkt.getName()))
				{
					logger.writeln("Packet identifier name '{0}' conflicts.", pkt.getName());
				}
				else
				{
					uniqueNames.add(pkt.getName());
				}

				pkt.prepareExport(logger, langsSet, m_doc.getConfig());

				if (pkt.getFlow() != Packet.FLOW_C2S)
				{
					Integer pkId = Integer.valueOf(m_nMaxS2CPacketID++);
					if (s2cIds.contains(pkId))
					{
						logger.writeln("S->C Packet ({0}) ID conflicts: {1}", pkt.getName(), pkId);
					}
					else
					{
						s2cIds.add(pkId);
						pkt.setExportS2CID(pkId.intValue());
					}
				}

				if (pkt.getFlow() != Packet.FLOW_S2C)
				{
					Integer pkId = Integer.valueOf(m_nMaxC2SPacketID++);
					if (c2sIds.contains(pkId))
					{
						logger.writeln("C->S Packet ({0}) ID conflicts: {1}", pkt.getName(), pkId);
					}
					else
					{
						c2sIds.add(pkId);
						pkt.setExportC2SID(pkId.intValue());
					}
				}
			}
		}
		--m_nMaxC2SPacketID;
		--m_nMaxS2CPacketID;

		if (uniqueNames.isEmpty())
		{
			logger.writeln("No packet defined.");
		}

		m_preparedCodeGenerators = new ArrayList<CodeGenerator>();
		for (Protocol.ExportEntry ee : m_doc.getExportEntries())
		{
			if (!ee.enabled)
				continue;

			CodeGenerator gen;
			switch(ee.langAndRole.getLanguage())
			{
			case Protocol.LANGUAGE_CPP:
				gen = new CppCodeGenerator();
				break;
			case Protocol.LANGUAGE_ACTIONSCRIPT:
				gen = new AsCodeGenerator();
				break;
			case Protocol.LANGUAGE_JAVASCRIPT:
				gen = new JsCodeGenerator();
				break;
/*
			case Protocol.LANGUAGE_JAVA:
				gen = new JavaCodeGenerator();
				break;
			case Protocol.LANGUAGE_CSHARP:
				gen = new CsharpCodeGenerator();
				break;
*/
			default:
				logger.writeln("Unsupported language id: " + ee.langAndRole.getLanguage());
				continue;
			}

			if (ee.langAndRole.getRole() != Protocol.ROLE_SERVER && ee.langAndRole.getRole() != Protocol.ROLE_CLIENT)
			{
				logger.writeln("Invalid Client/Server role id: " + ee.langAndRole.getRole());
				continue;
			}

			gen.setCsRole(ee.langAndRole.getRole());
			gen.setNewLineChar(ee.nlchar);

			if (Charset.availableCharsets().keySet().contains(ee.encoding))
			{
				gen.setEncoding(ee.encoding);
			}
			else
			{
				logger.writeln("[{0}] Unsupported export encoding: {1}", ee.langAndRole, ee.encoding);
				continue;
			}

			String abspath = m_doc.getAbsPath(ee.outputDir);
			File dirfile = new File(abspath);
			if (dirfile.exists() && dirfile.isDirectory())
			{
				gen.setOutputDir(dirfile);
			}
			else
			{
				logger.writeln("[{0}] Directory not exists: {1}", ee.langAndRole, abspath);
				continue;
			}

			if (gen.prepare(logger, m_doc))
				m_preparedCodeGenerators.add(gen);
		}

		return !logger.isLogged();
	}

	public boolean preview(ExportDlg dlg)
	{
		m_previewDlg = dlg;
		return export(dlg);
	}

	public boolean export(ITextWriter logger)
	{
		try
		{
			m_btw = new BufferedTabWriter();

			for (CodeGenerator gen : m_preparedCodeGenerators)
			{
				gen.init(this, m_doc);
				gen.export(logger);
			}

			return true;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.writeln("Exception: " + ex.getMessage());
			return false;
		}
	}

	public int getMaxC2SPacketID()
	{
		return m_nMaxC2SPacketID;
	}

	public int getMaxS2CPacketID()
	{
		return m_nMaxS2CPacketID;
	}

	public boolean isPreviewMode()
	{
		return (m_previewDlg != null);
	}

	public void onPreviewItemReady(String filename, String result)
	{
		m_previewDlg.addPreviewTab(filename, result);
	}

	public BufferedTabWriter getTabWriteBuffer()
	{
		return m_btw;
	}
}
