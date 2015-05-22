package bpdtool.codegen;

import bpdtool.data.*;
import bpdtool.Util;
import bpdtool.gui.MainFrame;

import java.util.ArrayList;

public class CppCodeGenerator  extends CodeGenerator
{
	private final String VAR_SOCKETPTR = "m_pSocket";
	private final String VAR_PACKETDISPATCHTABLE = "m_pPacketDispatchTable";
	private final String VAR_PACKETDISPATCHTABLEBEGIN = "s_aPacketDispatchTable";
	private final String FUNC_SETUPDISPATCHTABLE = "SetupPacketDispatchTable";
	private final String FUNC_SETDISPATCHTABLE = "SetPacketDispatchStage";
	private final String SUFFIX_PACKETRECVFUNCT = "PacketReceiverFuncT";
	private final String PREFIX_PACKETRECEIVER = "_Recv_";

	private boolean m_useComment;
	private boolean m_useNamespace;
	private boolean m_exportCommon;
	private boolean m_useDirectCast;
	private boolean m_useStdVector;
	private String m_namespace;
	private String m_fileExt;
	private String m_swapEndian;
	private String m_prefixSenderMethod;
	private String m_prefixBuilderMethod;
	private String m_prefixHandlerMethod;

	public CppCodeGenerator()
	{
	}

	@Override
	public boolean prepare(ITextWriter logger, Protocol doc)
	{
		m_useComment = !doc.getConfig().NoExportComment;

		m_useDirectCast = !doc.getConfig().Cpp.DisableDirectCasting;
		m_useStdVector = doc.getConfig().Cpp.UseStdVector;

		if (doc.getConfig().Cpp.CapitalizeMethodName)
		{
			m_prefixSenderMethod = Util.capitalize(doc.getConfig().Prefix.SenderMethod);
			m_prefixBuilderMethod = Util.capitalize(doc.getConfig().Prefix.BuilderMethod);
			m_prefixHandlerMethod = Util.capitalize(doc.getConfig().Prefix.HandlerMethod);
		}
		else
		{
			m_prefixSenderMethod = doc.getConfig().Prefix.SenderMethod;
			m_prefixBuilderMethod = doc.getConfig().Prefix.BuilderMethod;
			m_prefixHandlerMethod = doc.getConfig().Prefix.HandlerMethod;
		}

		PxConfig._CppCS cs = (getCsRole() == Protocol.ROLE_SERVER) ? doc.getConfig().Cpp.Server : doc.getConfig().Cpp.Client;

		m_useNamespace = cs.UseNamespace;
		if (m_useNamespace)
		{
			// TODO: check if valid namespace
			m_namespace = cs.Namespace;
		}

		if (cs.OverrideFileExt)
		{
			m_fileExt = cs.FileExt;
			if (m_fileExt.startsWith("."))
				m_fileExt = m_fileExt.substring(1);
		}
		else
		{
			m_fileExt = "cpp";
		}

		m_exportCommon = !cs.SkipGenerateCommon;
		m_swapEndian = cs.SwapEndian ? "_SwapEndian" : "";

		if (getCsRole() == Protocol.ROLE_SERVER)
		{
			if (Util.isNullOrEmpty(doc.getConfig().Cpp.Server.ClassName))
			{
				logger.writeln("C++ Server class name is empty.");
				return false;
			}
		}
		else
		{
			if (Util.isNullOrEmpty(doc.getConfig().Cpp.Client.ClassName))
			{
				logger.writeln("C++ Client class name is empty.");
				return false;
			}
		}

		if (m_exportCommon)
		{
			if (Util.isNullOrEmpty(doc.getConfig().Cpp.CommonFileName))
			{
				logger.writeln("C++ Struct and Constant file name is empty.");
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean export(ITextWriter logger) throws Exception
	{
		m_logger = logger;

		if (getCsRole() == Protocol.ROLE_SERVER)
		{
			generateServer();
		}
		else
		{
			generateClient();
		}

		if (m_exportCommon)
			generateCommon();

		return true;
	}

	// types for struct, output
	private static String getRecvCppVarType(StructField fld, boolean isPacket)
	{
		if (fld.getPrimitiveType() != null)
		{
			if (fld.getPrimitiveType().getSizeBytes() > 0)
				return fld.getType();

			switch (fld.getPrimitiveType().getCategory())
			{
			case PrimitiveType.STRING:
			case PrimitiveType.STRING_TINY:
				return "NsString";

			case PrimitiveType.WIDESTRING:
			case PrimitiveType.WIDESTRING_TINY:
				return "NsWideString";

/*
			case PrimitiveType.TSTRING:
			case PrimitiveType.TSTRING_TINY:
				return "NsTString";

			case PrimitiveType.STD_STRING:
			case PrimitiveType.STD_STRING_TINY:
				return isPacket ? "NsString" : "std::string";

			case PrimitiveType.STD_WIDESTRING:
			case PrimitiveType.STD_WIDESTRING_TINY:
				return isPacket ? "NsWideString" : "std::wstring";

			case PrimitiveType.STD_TSTRING:
			case PrimitiveType.STD_TSTRING_TINY:
				return isPacket ? "NsTString" : "tstring";
*/
			case PrimitiveType.BUFFER:
			case PrimitiveType.BUFFER_TINY:
				return "NsBuffer";
			}
		}

		return fld.getType();
	}

	private void writeNamespaceBegin(StreamWriter sw)
	{
		if (m_useNamespace)
		{
			sw.writeln("namespace {0} {", m_namespace);
			sw.writeln("");
		}
	}

	private void writeNamespaceEnd(StreamWriter sw)
	{
		if (m_useNamespace)
		{
			sw.writeln("} // namespace {0}", m_namespace);
			sw.writeln("");
		}
	}

	private void generateCommon() throws Exception
	{
		if(m_doc.getConstants().size() == 0 && m_doc.getUserTypes().size() == 0)
		{
			m_logger.writeln("Skip common header file generation. (nothing to export)");
			return;
		}

		// common header file
		String filename = "_" + m_doc.getConfig().Cpp.CommonFileName + ".h";
		StreamWriter sw = openCodeStream(filename);

		sw.writeln("#pragma once");
		sw.writeln("");
		writeNamespaceBegin(sw);

		for (Constant ct : m_doc.getConstants())
		{
			if (!Util.isNullOrEmpty(ct.getComment()) && m_useComment)
				sw.writeln("// " + ct.getComment());

			if (ct.isEnum())
			{
				sw.writeln("enum {0}", ct.getName());
				sw.writeln("{");

				getTabWriteBuffer().begin();
				for (int i = 0; i < ct.getFields().size(); i++)
				{
					ConstantField fld = ct.getFields().get(i);
					getTabWriteBuffer().write("{0}", fld.getName());

					if (fld.isValueSet())
					{
						getTabWriteBuffer().write(" = {0}", fld.getValue());
					}

					if (i < ct.getFields().size() - 1)
					{
						getTabWriteBuffer().write(",");
					}

					if (!Util.isNullOrEmpty(fld.getComment()) && m_useComment)
						getTabWriteBuffer().writeln("\t// " + fld.getComment());
					else
						getTabWriteBuffer().writeln("");
				}
				getTabWriteBuffer().end(sw, 1);
				sw.writeln("};");
			}
			else
			{
				getTabWriteBuffer().begin();
				for (ConstantField fld : ct.getFields())
				{
					getTabWriteBuffer().write("#define {0}\t{1}", fld.getName(), fld.getValue());

					if (!Util.isNullOrEmpty(fld.getComment()) && m_useComment)
						getTabWriteBuffer().writeln("\t// " + fld.getComment());
					else
						getTabWriteBuffer().writeln("");
				}
				getTabWriteBuffer().end(sw, 0);
			}

			sw.writeln("");
		}

		// Usertypes
		if (m_useComment && m_doc.getUserTypes().size() > 0)
		{
			sw.writeln("// User-defined types:\n");
		}

		for (UserType ut : m_doc.getUserTypes())
		{
			if (!Util.isNullOrEmpty(ut.getComment()) && m_useComment)
			{
				sw.writeln("// {0}", ut.getComment());
			}

			if (ut instanceof BlindClass)
			{
				sw.writeln("// class {0};", ut.getName());
			}
			else
			{
				Struct st = (Struct)ut;

				if (st.getFields().size() == 1)
				{
					sw.writeln("typedef {0} {1};", getRecvCppVarType(st.getFields().get(0), false), st.getName());
				}
				else
				{
					sw.writeln("struct {0}", st.getName());
					sw.writeln("{");

					getTabWriteBuffer().begin();
					for (StructField fld : st.getFields())
					{
						getTabWriteBuffer().write("{0} {1}", getRecvCppVarType(fld, false), fld.getName());
						if (fld.getRepeatInfo().getType() == RepeatInfo.TYPE_FIXED)
						{
							getTabWriteBuffer().write("[{0}]", fld.getRepeatInfo().getCount());
						}

						if (!Util.isNullOrEmpty(fld.getComment()) && m_useComment)
							getTabWriteBuffer().writeln(";\t// {0}", fld.getComment());
						else
							getTabWriteBuffer().writeln(";");
					}
					getTabWriteBuffer().end(sw, 1);

					sw.writeln("};");
				}
			}

			sw.writeln("");
		}

		writeNamespaceEnd(sw);

		closeCodeStream(sw);
		m_logger.writeln("Generated common header file: " + filename);
	}

	private void writePacketIDs_InHeader(StreamWriter sw)
	{
		int id;

		// C2S and INTER
		if (m_useComment)
		{
			sw.writeln("\t// Client->Server Packet IDs:");
		}

		String idvtype = m_doc.getConfig().Use16BitPacketID ? "unsigned short" : "unsigned char";

		getTabWriteBuffer().begin();
		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pkt : grp.getPackets())
			{
				if (pkt.getFlow() != Packet.FLOW_S2C)
				{
					getTabWriteBuffer().writeln("static const {0} {1}{2}\t= {3};", idvtype,
						m_doc.getConfig().Prefix.C2SPacketID, pkt.getName(), getIdStr(pkt.getExportC2SID()));
				}
			}
		}
		getTabWriteBuffer().writeln("static const {0} {1}{2}\t= {3};", idvtype,
			m_doc.getConfig().Prefix.C2SPacketID, SUFFIX_LASTPACKETID, getIdStr(getExporter().getMaxC2SPacketID()));
		getTabWriteBuffer().end(sw, 1);

		sw.writeln("");


		// S2C and INTER
		if (m_useComment)
		{
			sw.writeln("\t// Server->Client Packet IDs:");
		}

		getTabWriteBuffer().begin();
		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pkt : grp.getPackets())
			{
				if (pkt.getFlow() != Packet.FLOW_C2S)
				{
					getTabWriteBuffer().writeln("static const {0} {1}{2}\t= {3};", idvtype,
						m_doc.getConfig().Prefix.S2CPacketID, pkt.getName(), getIdStr(pkt.getExportS2CID()));
				}
			}
		}
		getTabWriteBuffer().writeln("static const {0} {1}{2}\t= {3};", idvtype,
			m_doc.getConfig().Prefix.S2CPacketID, SUFFIX_LASTPACKETID, getIdStr(getExporter().getMaxS2CPacketID()));
		getTabWriteBuffer().end(sw, 1);

		sw.writeln("");
	}

	private void writePacketDecls_InHeader(StreamWriter sw, boolean isServer)
	{
		if (m_useComment)
		{
			sw.writeln("\t// Packet data types:\n");
		}

		String prefix = m_doc.getConfig().Prefix.PacketStruct;

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if(isServer)
				{
					if (pk.getFlow() == Packet.FLOW_S2C)
						continue;
				}
				else
				{
					if (pk.getFlow() == Packet.FLOW_C2S)
						continue;
				}

				if (!Util.isNullOrEmpty(pk.getComment()) && m_useComment)
				{
					sw.writeln("\t// {0}", pk.getComment());
				}

				if (pk.getFields().size() == 0)
				{
					sw.writeln("\t// {0}{1}: NO PACKET DATA", prefix, pk.getName());
				}
				else
				{
					sw.writeln("\tstruct {0}{1}", prefix, pk.getName());
					sw.writeln("\t{");

					getTabWriteBuffer().begin();
					for (PacketField fld : pk.getFields())
					{
						if (fld.getRepeatInfo().isVariableRepeat())
						{
							boolean isFixedLength = false;

							if (fld.getPrimitiveType() != null)
							{
								if (fld.getPrimitiveType().getSizeBytes() > 0)
									isFixedLength = true;
							}
							else
							{
								if (fld.getCustomType().getSizeBytes() > 0)
									isFixedLength = true;
							}

							getTabWriteBuffer().write("{0}<{1}> {2}", (isFixedLength ? "NsPrimitiveArray" : "NsClassArray"),
								getRecvCppVarType(fld, true), fld.getName());
						}
						else
						{
							getTabWriteBuffer().write("{0} {1}", getRecvCppVarType(fld, true), fld.getName());

							if (fld.getRepeatInfo().hasRepeat())
							{
								getTabWriteBuffer().write("[{0}]", fld.getRepeatInfo().getCount());
							}
						}

						if (!Util.isNullOrEmpty(fld.getComment()) && m_useComment)
							getTabWriteBuffer().writeln(";\t// {0}", fld.getComment());
						else
							getTabWriteBuffer().writeln(";");
					}
					getTabWriteBuffer().end(sw, 2);

					sw.writeln("\t};");
				}

				sw.writeln("");
			}
		}
	}

	private void substituteTemplateCommon(TemplateBuilder tw, String cname, String lastpktid, String incinl, String stgsuffix)
	{
		tw.substitute("$$ClassName$$", cname);
		tw.substitute("$$CommonHeader$$", m_doc.getConfig().Cpp.CommonFileName);
		tw.substitute("$$SwapEndian$$", "");
		tw.substitute("$$MaxPacketID$$", lastpktid);
		tw.substitute("$$IncludeInlineFile$$", incinl);
		tw.substitute("$$VarDispatchTable$$", VAR_PACKETDISPATCHTABLE);
		tw.substitute("$$VarDispatchTableBegin$$", VAR_PACKETDISPATCHTABLEBEGIN + stgsuffix);

		if (m_useNamespace)
		{
			tw.substitute("$$NamespaceBegin$$", "namespace " + m_namespace + " {\n");
			tw.substitute("$$NamespaceEnd$$", "} // namespace " + m_namespace + "\n");
		}
		else
		{
			tw.substitute("$$NamespaceBegin$$", "");
			tw.substitute("$$NamespaceEnd$$", "");
		}

		if (m_doc.getConfig().Use16BitPacketID)
		{
			tw.substitute("$$PacketIdGet$$", "_nsr._ReadWordAt(0)");
			tw.substitute("$$PacketHeaderSize$$", "4");
			tw.substitute("$$PacketLengthPos$$", "2");
		}
		else
		{
			tw.substitute("$$PacketIdGet$$", "_nsr._ReadByteAt(0)");
			tw.substitute("$$PacketHeaderSize$$", "3");
			tw.substitute("$$PacketLengthPos$$", "1");
		}
	}

	private ArrayList<String> splitDirComponent(String dirname)
	{
		ArrayList<String> dirlist = new ArrayList<String>();
		String[] toks = dirname.split("/|\\\\");
		for (String t : toks)
		{
			if (!Util.isNullOrEmpty(t))
			{
				dirlist.add(t);
			}
		}

		return dirlist;
	}

	public void generateServer() throws Exception
	{
		String svrClassName = m_doc.getConfig().Cpp.Server.ClassName;
		String svr_filename = "_" + svrClassName;
		String svrh_filename = svr_filename + ".h";
		String svrinl_filename = svr_filename + ".h.inl";
		String svrcpp_filename = svr_filename + "." + m_fileExt;
		StreamWriter sw;

		boolean bUseStage = (!m_doc.getConfig().NoUsePacketDispatchStage && m_doc.getPacketStages().size() > 1);

		// server header body (.h)
		if(m_doc.getConfig().Cpp.Server.GenerateSampleImpl)
		{
			TemplateBuilder tw = new TemplateBuilder(MainFrame.getCodeTemplate("server.h"));
			substituteTemplateCommon(tw, svrClassName, (m_doc.getConfig().Prefix.C2SPacketID + SUFFIX_LASTPACKETID),
				svrinl_filename, bUseStage ? "[0]" : "");

			sw = openCodeStream(svrh_filename);
			sw.write(tw.getResult());
			closeCodeStream(sw);

			m_logger.writeln("Generated server header file: " + svrh_filename);
		}


		// server header inline (.inl)
		sw = openCodeStream(svrinl_filename);

		sw.writeln("\npublic:");
		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_C2S)
					writeDecl_PacketSender(sw, pk);
			}
		}

		sw.writeln("");
		sw.writeln("\tstatic void {0}();\n", FUNC_SETUPDISPATCHTABLE);

		// Packet stages
		if(bUseStage)
		{
			sw.writeln("\tenum PacketStages");
			sw.writeln("\t{");

			getTabWriteBuffer().begin();
			int i = 0;
			for(PacketStage ps : m_doc.getPacketStages())
			{
				String comma = (++i < m_doc.getPacketStages().size()) ? "," : "";
				getTabWriteBuffer().writeln("{0}\t={1}{2}\t// {3}", ps.getName(), ps.getIndex(), comma, ps.getAbbr());
			}
			getTabWriteBuffer().end(sw, 2);

			sw.writeln("\t};");
			sw.writeln("");
		}

		// packet ids
		writePacketIDs_InHeader(sw);

		// packet structs
		writePacketDecls_InHeader(sw, true);

		sw.writeln("private:");
		if(bUseStage)
			sw.writeln("\tvoid {0}(int stage);", FUNC_SETDISPATCHTABLE);

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pkt : grp.getPackets())
			{
				if (pkt.getFlow() != Packet.FLOW_S2C)
					writeDecl_PacketReceiver(sw, pkt);
			}
		}
		sw.writeln("");
		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pkt : grp.getPackets())
			{
				if (pkt.getFlow() != Packet.FLOW_S2C)
					writeDecl_PacketHandler(sw, pkt);
			}
		}

		sw.writeln("");
		sw.writeln("\tITcpSocket* {0};", VAR_SOCKETPTR);
		sw.writeln("\ttypedef bool ({0}::*{1})(NetStreamReader&);", svrClassName, SUFFIX_PACKETRECVFUNCT);
		sw.writeln("\t{0}* {1};", SUFFIX_PACKETRECVFUNCT, VAR_PACKETDISPATCHTABLE);
		sw.write("\tstatic {0} {1}", SUFFIX_PACKETRECVFUNCT, VAR_PACKETDISPATCHTABLEBEGIN);
		if (bUseStage)
			sw.write("[{0}]", m_doc.getPacketStages().size());
		sw.writeln("[{0}];", getExporter().getMaxC2SPacketID());
		sw.writeln("");

		closeCodeStream(sw);
		m_logger.writeln("Generated server header inline file: " + svrinl_filename);



		// server cpp file
		sw = openCodeStream(svrcpp_filename);

		if (!Util.isNullOrEmpty(m_doc.getConfig().Cpp.Server.CustomHeader))
			sw.writeln(m_doc.getConfig().Cpp.Server.CustomHeader);

		sw.writeln("");
		writeNamespaceBegin(sw);

		// dispatch table setup func
		{
			sw.write("{0}::{1} {0}::{2}", svrClassName, SUFFIX_PACKETRECVFUNCT, VAR_PACKETDISPATCHTABLEBEGIN);
			if (bUseStage)
			{
				sw.writeln("[{0}][{1}];", m_doc.getPacketStages().size(), getExporter().getMaxC2SPacketID());
			}
			else
			{
				sw.writeln("[{0}];", getExporter().getMaxC2SPacketID());
			}

			sw.writeln("");
			sw.writeln("void {0}::{1}()", svrClassName, FUNC_SETUPDISPATCHTABLE);
			sw.writeln("{");
			sw.writeln("\tmemset({0}, 0, sizeof({0}));", VAR_PACKETDISPATCHTABLEBEGIN);
			sw.writeln("");

			if (!bUseStage)
			{
				for (PacketGroup grp : m_doc.getPacketGroups())
				{
					for (Packet pk : grp.getPackets())
					{
						if (pk.getFlow() != Packet.FLOW_S2C)
							writeImpl_PacketReceiverTableItem(sw, svrClassName, null, m_doc.getConfig().Prefix.C2SPacketID, pk);
					}
				}
			}
			else
			{
				int ips =0;
				for (PacketStage ps : m_doc.getPacketStages())
				{
					int ii = 0;
					for (PacketGroup grp : m_doc.getPacketGroups())
					{
						for (Packet pk : grp.getPackets())
						{
							if (pk.getFlow() != Packet.FLOW_S2C)
							{
								if (pk.isAllStage() || pk.getStages().indexOf(ps) >= 0)
								{
									writeImpl_PacketReceiverTableItem(sw, svrClassName, ps.getName(), m_doc.getConfig().Prefix.C2SPacketID, pk);
									ii++;
								}
							}
						}
					}

					if (++ips < m_doc.getPacketStages().size() && ii > 0)
						sw.writeln("");
				}
			}

			sw.writeln("}\n");

			if (bUseStage)
			{
				sw.writeln("void {0}::{1}(int stage)", svrClassName, FUNC_SETDISPATCHTABLE);
				sw.writeln("{");
				sw.writeln("\t{0} = {1}[stage];", VAR_PACKETDISPATCHTABLE, VAR_PACKETDISPATCHTABLEBEGIN);
				sw.writeln("}\n");
			}

		}

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_C2S)
				{
					writeImpl_PacketSender(sw, svrClassName, m_doc.getConfig().Prefix.S2CPacketID, pk, false);
				}
			}
		}

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_S2C)
					writeImpl_PacketReceiver(sw, svrClassName, pk);
			}
		}

		writeNamespaceEnd(sw);

		closeCodeStream(sw);
		m_logger.writeln("Generated server implementation file: " + svrcpp_filename);
	}

	public void generateClient() throws Exception
	{
		String cliClassName = m_doc.getConfig().Cpp.Client.ClassName;
		String filename = "_" + cliClassName;
		StreamWriter sw;

		// client header file
		String clih_filename = filename + ".h";
		String cliinl_filename = filename + ".h.inl";
		String clicpp_filename = filename + "." + m_fileExt;

		// client header body (.h)
		if(m_doc.getConfig().Cpp.Client.GenerateSampleImpl)
		{
			TemplateBuilder tw = new TemplateBuilder(MainFrame.getCodeTemplate("client.h"));
			substituteTemplateCommon(tw, cliClassName, (m_doc.getConfig().Prefix.S2CPacketID + SUFFIX_LASTPACKETID),
				cliinl_filename, "");

			sw = openCodeStream(clih_filename);
			sw.write(tw.getResult());
			closeCodeStream(sw);

			m_logger.writeln("Generated client header file: " + clih_filename);
		}


		// client header inline (.inl)
		sw = openCodeStream(cliinl_filename);

		sw.writeln("\npublic:");
		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_S2C)
					writeDecl_PacketSender(sw, pk);
			}
		}

		sw.writeln("");
		sw.writeln("\tstatic void {0}();\n", FUNC_SETUPDISPATCHTABLE);

		// packet ids
		writePacketIDs_InHeader(sw);

		// packet structs
		writePacketDecls_InHeader(sw, false);

		sw.writeln("private:");
		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_C2S)
					writeDecl_PacketReceiver(sw, pk);
			}
		}
		sw.writeln("");
		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_C2S)
					writeDecl_PacketHandler(sw, pk);
			}
		}

		sw.writeln("");
		sw.writeln("\tITcpSocket* {0};", VAR_SOCKETPTR);
		sw.writeln("\ttypedef bool ({0}::*{1})(NetStreamReader&);", cliClassName, SUFFIX_PACKETRECVFUNCT);
		sw.writeln("\t{0}* {1};", SUFFIX_PACKETRECVFUNCT, VAR_PACKETDISPATCHTABLE);
		sw.writeln("\tstatic {0} {1}[{2}];", SUFFIX_PACKETRECVFUNCT, VAR_PACKETDISPATCHTABLEBEGIN, getExporter().getMaxS2CPacketID());
//		if(m_doc.getConfig().Cpp.ManualFlush)
//			sw.writeln("\tbool {0};", VAR_SENDPACKETFLUSH);
		sw.writeln("");

		closeCodeStream(sw);
		m_logger.writeln("Generated client header inline file: " + cliinl_filename);



		// client cpp file
		sw = openCodeStream(clicpp_filename);

		if (!Util.isNullOrEmpty(m_doc.getConfig().Cpp.Client.CustomHeader))
			sw.writeln(m_doc.getConfig().Cpp.Client.CustomHeader);
		sw.writeln("");

		writeNamespaceBegin(sw);

		// dispatch table setup func
		{
			sw.writeln("{0}::{1} {0}::{2}[{3}];", cliClassName, SUFFIX_PACKETRECVFUNCT,
				VAR_PACKETDISPATCHTABLEBEGIN, getExporter().getMaxS2CPacketID());

			sw.writeln("");
			sw.writeln("void {0}::{1}()", cliClassName, FUNC_SETUPDISPATCHTABLE);
			sw.writeln("{");
			sw.writeln("\tmemset({0}, 0, sizeof({0}));", VAR_PACKETDISPATCHTABLEBEGIN);
			sw.writeln("");

			for (PacketGroup grp : m_doc.getPacketGroups())
			{
				for (Packet pk : grp.getPackets())
				{
					if (pk.getFlow() != Packet.FLOW_C2S)
						writeImpl_PacketReceiverTableItem(sw, cliClassName, null, m_doc.getConfig().Prefix.S2CPacketID, pk);
				}
			}

			sw.writeln("}\n");
		}

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_S2C)
				{
					writeImpl_PacketSender(sw, cliClassName, m_doc.getConfig().Prefix.C2SPacketID, pk, false);
				}
			}
		}

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_C2S)
					writeImpl_PacketReceiver(sw, cliClassName, pk);
			}
		}

		writeNamespaceEnd(sw);

		closeCodeStream(sw);
		m_logger.writeln("Generated client implementation file: " + clicpp_filename);
	}


	private String sender_GetAutoVarDefinition(String ctype, String name)
	{
		if (m_useStdVector)
			return Util.stringFormat("const std::vector<{0}>& {1}", ctype, name);
		else
			return Util.stringFormat("const {0}* {1}, int {1}_length", ctype, name);
	}

	// types for input parameter, vector argument..
	private String sender_GetCppInputVartype(StructField fld, PrimitiveType pt) throws Exception
	{
		if (pt.getSizeBytes() > 0)
			return fld.getType();

		String basetype;

		switch (pt.getCategory())
		{
		case PrimitiveType.STRING:
		case PrimitiveType.STRING_TINY:
			return "const char*";

		case PrimitiveType.WIDESTRING:
		case PrimitiveType.WIDESTRING_TINY:
			return "const WCHAR*";

/*
		case PrimitiveType.TSTRING:
		case PrimitiveType.TSTRING_TINY:
			return "const TCHAR*";

		case PrimitiveType.STD_STRING:
		case PrimitiveType.STD_STRING_TINY:
			basetype = "const std::string";
			break;

		case PrimitiveType.STD_WIDESTRING:
		case PrimitiveType.STD_WIDESTRING_TINY:
			basetype = "const std::wstring";
			break;

		case PrimitiveType.STD_TSTRING:
		case PrimitiveType.STD_TSTRING_TINY:
			basetype = "const tstring";
			break;
*/
		case PrimitiveType.BUFFER:
		case PrimitiveType.BUFFER_TINY:
			basetype = "NsBuffer";
			break;

			default:
				throw new Exception("Invalid packet field primitive type: " + fld.getType());
		}

		if (fld.getRepeatInfo().isOnce())
			return (basetype + '&');
		else
			return basetype;
	}

	// sender parameter variable declaration
	private String sender_GetVarDefinition(PacketField fld) throws Exception
	{
		boolean bAutoRepeat = (fld.getRepeatInfo().getType() == RepeatInfo.TYPE_AUTO_VAR);
/*
		if (fld.getRepeatInfo().Equals(Protocol.REPEAT_AUTO)
		|| fld.getRepeatInfo().Equals(Protocol.REPEAT_AUTOTINY))
		{
			bAutoRepeat = true;
		}
		else
		{
			bAutoRepeat = false;
		}
*/
		String arrmark = (fld.getRepeatInfo().hasRepeat()) ? "*" : "";

		PrimitiveType pt = null;

		if (fld.getPrimitiveType() != null)
		{
			pt = fld.getPrimitiveType();
		}
		else if(fld.getCustomType() instanceof Struct)
		{
			Struct st = (Struct) fld.getCustomType();
			if (st.getFields().size() == 1 &&  st.getFields().get(0).getPrimitiveType() != null)
			{
				pt = st.getFields().get(0).getPrimitiveType();
			}
		}

		if(pt == null)
		{
			if (bAutoRepeat)
			{
				return sender_GetAutoVarDefinition(fld.getType(), fld.getName());
			}
			else
			{
				return Util.stringFormat("const {0}{1}& {2}", fld.getType(), arrmark, fld.getName());
			}
		}
		else
		{
			String ctype = sender_GetCppInputVartype(fld, pt);

			if (bAutoRepeat)
			{
				return sender_GetAutoVarDefinition(ctype, fld.getName());
			}
			else
			{
				return Util.stringFormat("{0}{1} {2}", ctype, arrmark, fld.getName());
			}
		}
	}

	private void writeDecl_PacketSender(StreamWriter sw, Packet pk) throws Exception
	{
		sw.write("\tvoid {0}{1}(", m_prefixSenderMethod, pk.getName());

		int i = 0;
		while (i < pk.getFields().size())
		{
			sw.write(sender_GetVarDefinition(pk.getFields().get(i)));
			if (++i < pk.getFields().size())
				sw.write(", ");
		}

		sw.writeln(");");

		if(pk.isGenerateBuilder())
		{
			sw.write("\tstatic int {0}{1}(NetStreamWriter{2}&, ", m_prefixBuilderMethod, pk.getName(), m_swapEndian);

			i = 0;
			while (i < pk.getFields().size())
			{
				sw.write(sender_GetVarDefinition(pk.getFields().get(i)));
				if (++i < pk.getFields().size())
					sw.write(", ");
			}

			sw.writeln(");");
		}
	}

	private void writeDecl_PacketReceiver(StreamWriter sw, Packet pk)
	{
		sw.writeln("\tbool {0}{1}(NetStreamReader{2}&);", PREFIX_PACKETRECEIVER, pk.getName(), m_swapEndian);
	}

	private void writeDecl_PacketHandler(StreamWriter sw, Packet pk)
	{
		sw.write("\tbool {0}{1}(", m_prefixHandlerMethod, pk.getName());
		if(pk.getFields().size() > 0)
			sw.write(m_doc.getConfig().Prefix.PacketStruct + pk.getName() + "*");
		sw.writeln(");");
	}

	private void writeImpl_PacketReceiverTableItem(StreamWriter sw, String clsname, String stage, String idprefix, Packet pk)
	{
		stage = (stage == null) ? "" : ("[" + stage + "]");
		sw.writeln("\t{0}{1}[{2}{3}] = &{4}::{5}{6};",
			VAR_PACKETDISPATCHTABLEBEGIN, stage, idprefix, pk.getName(),
			clsname, PREFIX_PACKETRECEIVER, pk.getName());
	}

	// get proper method name of NetStream(Ns) class
	private String getFieldNsWhat(StructField fld, NsWhat nswhat) throws Exception
	{
		nswhat.hasRepeat = fld.getRepeatInfo().hasRepeat();
		nswhat.pt = null;

		if (fld.getPrimitiveType() != null)
		{
			nswhat.pt = fld.getPrimitiveType();
		}
		else if(fld.getCustomType() instanceof Struct)
		{
			Struct st = (Struct) fld.getCustomType();
			if (st.getFields().size() == 1 &&  st.getFields().get(0).getPrimitiveType() != null)
			{
				nswhat.pt = st.getFields().get(0).getPrimitiveType();
			}
			else
			{
				return null;
			}
		}
		else
		{
			// blind class
			return null;
		}


		String dtype = null;
		String tiny = null;

		switch (nswhat.pt.getCategory())
		{
		case PrimitiveType.SIGNED_INTEGER:
			switch (nswhat.pt.getSizeBytes())
			{
				case 1:
					dtype = "Char";
					break;
				case 2:
					dtype = "Short";
					break;
				case 4:
					dtype = "Int";
					break;
				case 8:
					dtype = "Int64";
					break;
			}
			break;

		case PrimitiveType.UNSIGNED_INTEGER:
			switch (nswhat.pt.getSizeBytes())
			{
				case 1:
					dtype = "Byte";
					break;
				case 2:
					dtype = "Word";
					break;
				case 4:
					dtype = "Dword";
					break;
				case 8:
					dtype = "Qword";
					break;
			}
			break;

		case PrimitiveType.BOOLEAN:
			switch (nswhat.pt.getSizeBytes())
			{
				case 1:
					dtype = "Bool";
					break;
			}
			break;

		case PrimitiveType.FLOAT:
			switch (nswhat.pt.getSizeBytes())
			{
				case 4:
					dtype = "Float";
					break;
				case 8:
					dtype = "Double";
					break;
			}
			break;

		case PrimitiveType.STRING_TINY:
		//case PrimitiveType.STD_STRING_TINY:
			tiny = "Tiny";
			//goto case PrimitiveType.STRING;
		case PrimitiveType.STRING:
		//case PrimitiveType.STD_STRING:
			dtype = "String";
			break;

		case PrimitiveType.WIDESTRING_TINY:
		//case PrimitiveType.STD_WIDESTRING_TINY:
			tiny = "Tiny";
			//goto case PrimitiveType.WIDESTRING;
		case PrimitiveType.WIDESTRING:
		//case PrimitiveType.STD_WIDESTRING:
			dtype = "WideString";
			break;
/*
		case PrimitiveType.TSTRING_TINY:
		case PrimitiveType.STD_TSTRING_TINY:
			tiny = "Tiny";
			//goto case PrimitiveType.TSTRING;
		case PrimitiveType.TSTRING:
		case PrimitiveType.STD_TSTRING:
			dtype = "TString";
			break;
*/
		case PrimitiveType.BUFFER_TINY:
			tiny = "Tiny";
			//goto case PrimitiveType.BUFFER;
		case PrimitiveType.BUFFER:
			dtype = "Buffer";
			break;
		}

		if (dtype == null)
			throw new Exception("Invalid primitive type: " + nswhat.pt);

		if (nswhat.hasRepeat)
		{
//			if (false == isAutoRepeat)
			{
				dtype += 's';
			}
		}
		
		if (tiny != null)
			dtype += tiny;

		return dtype;
	}

	private void printTabs(StreamWriter sw, int tabs)
	{
		while (tabs-- > 0)
			sw.write("\t");
	}

	private void printStructWriter(StreamWriter sw, int depth, int tabs, Struct st, String instance) throws Exception
	{
		for(StructField fld : st.getFields())
		{
			NsWhat nswhat = new NsWhat();
			assert(fld.getRepeatInfo().getType() != RepeatInfo.TYPE_AUTO_VAR);
			String dtype = getFieldNsWhat(fld, nswhat);

			if(dtype != null)
			{
				printTabs(sw, tabs);

				if (nswhat.hasRepeat)
				{
					sw.writeln("_nsw.Write{0}({3}.{1}, {2});", dtype, fld.getName(), fld.getRepeatInfo().getCount(), instance);
				}
				else
				{
					sw.writeln("_nsw.Write{0}({2}.{1});", dtype, fld.getName(), instance);
				}
			}
			else if(fld.getCustomType() instanceof BlindClass)
			{
				if (nswhat.hasRepeat)
				{
					printTabs(sw, tabs);
					sw.writeln("for(int _i{0}=0; _i{0}<{0}; _i{0}++)", depth, fld.getRepeatInfo().getCount());
					printTabs(sw, tabs + 1);
					sw.writeln("{1}.{0}[_i{2}].SerializeTo(_nsw);", fld.getName(), instance, depth);
				}
				else
				{
					printTabs(sw, tabs);
					sw.writeln("{1}.{0}.SerializeTo(_nsw);", fld.getName(), instance);
				}
			}
			else
			{
				Struct st2 = (Struct) fld.getCustomType();

				if (nswhat.hasRepeat)
				{
					printTabs(sw, tabs);
					sw.writeln("for(int _i{1}=0; _i{1}<{0}; _i{1}++)", fld.getRepeatInfo().getCount(), depth);
					printTabs(sw, tabs);
					sw.writeln("{");
					printStructWriter(sw, depth + 1, tabs + 1, st2, instance + "." + fld.getName() + "[_i" + depth + "]");
					printTabs(sw, tabs);
					sw.writeln("}");
				}
				else
				{
					printStructWriter(sw, depth + 1, tabs, st2, instance + "." + fld.getName());
				}
			}
		}
	}

	private void writeImpl_PacketSender(StreamWriter sw, String cname, String idprefix, Packet pk, boolean bBuilderMethod) throws Exception
	{
		if (!Util.isNullOrEmpty(pk.getComment()) && m_useComment)
			sw.writeln("// " + pk.getComment());

		if(bBuilderMethod)
			sw.write("int {0}::{1}{2}", cname, m_prefixBuilderMethod, pk.getName());
		else
			sw.write("void {0}::{1}{2}", cname, m_prefixSenderMethod, pk.getName());

		if (pk.getFields().size() == 0)
		{
			sw.writeln("()");
		}
		else
		{
			sw.writeln("\n(");

			if(bBuilderMethod)
			{
				sw.writeln("\tNetStreamWriter{0}& _nsw,", m_swapEndian);
			}

			int i = 0;
			while (i < pk.getFields().size())
			{
				PacketField fld = pk.getFields().get(i);

				sw.write("\t" + sender_GetVarDefinition(fld));
				if (++i < pk.getFields().size())
					sw.write(", ");

				if (!Util.isNullOrEmpty(fld.getComment()) && m_useComment)
				{
					sw.writeln("\t// " + fld.getComment());
				}
				else
				{
					sw.writeln("");
				}
			}

			sw.writeln(")");
		}

		sw.writeln("{");

		if(bBuilderMethod)
		{
			sw.writeln("\tchar* _buff = _nsw.GetCurrent();");
		}
		else
		{
			// TODO: calculate the required maximum packet buffer size
			sw.writeln("\tchar _buff[{0}];", pk.getMaxPacketBufferSize());
			sw.writeln("\tNetStreamWriter{0} _nsw(_buff, sizeof(_buff));", m_swapEndian);
		}

/*
		if (pk._RepeatDepth > 0)//pk._HasAutoRepeat)
		{
			sw.writeln("\tint _len;");
		}

		if(pk._RepeatDepth > 0)
		{
			sw.write("\tint ");
			for(int r=0; r<pk._RepeatDepth; r++)
			{
				sw.write("r{0}", r);
				if (r < pk._RepeatDepth - 1)
					sw.write(", ");
			}
			sw.writeln(";");
		}
		sw.writeln("\tint _len;");
*/
		sw.writeln("");

		if (m_doc.getConfig().Use16BitPacketID)
		{
			sw.writeln("\t_nsw.WriteWord({0}{1});", idprefix, pk.getName());
			sw.writeln("\t_nsw.Skip(2);");
		}
		else
		{
			sw.writeln("\t_buff[0] = {0}{1};", idprefix, pk.getName());
			sw.writeln("\t_nsw.Skip(3);");
		}

		for (PacketField fld : pk.getFields())
		{
			boolean isAutoRepeat = (fld.getRepeatInfo().getType() == RepeatInfo.TYPE_AUTO_VAR);

			if (isAutoRepeat)
			{
				if (m_useStdVector)
				{
					sw.writeln("\t_len = (int) {0}.size(); ASSERT(_len <= {1});", fld.getName(), fld.getRepeatInfo().getLimit());
					sw.writeln("\t_nsw.Write{0}(_len);",
						(fld.getRepeatInfo().getLimit() <= 0xFF) ? "Byte" : "Word");
				}
				else
				{
					sw.writeln("\tASSERT({0}_length <= {1});", fld.getName(), fld.getRepeatInfo().getLimit());
					sw.writeln("\t_nsw.Write{1}({0}_length);", fld.getName(),
						(fld.getRepeatInfo().getLimit() <= 0xFF) ? "Byte" : "Word");
				}
			}

			NsWhat nswhat = new NsWhat();
			String dtype = getFieldNsWhat(fld, nswhat);

			if(dtype != null)
			{
				if(isAutoRepeat)
				{
					if(m_useStdVector)
					{
						String ctype = fld.getType();
						if(nswhat.pt != null)
							ctype = sender_GetCppInputVartype(fld, nswhat.pt);

						if (nswhat.pt.getSizeBytes() > 0)
						{
							sw.writeln("\t\t_nsw.Write{0}(&{1}[0], (int){1}.size());", dtype, fld.getName());
						}
						else
						{
							sw.writeln("\tfor(std::vector<{0}>::const_iterator _itr={1}.begin(); _itr!={1}.end(); ++_itr)", ctype, fld.getName());
							sw.writeln("\t\t_nsw.Write{0}(*_itr);", dtype);
						}
					}
					else
					{
						if (nswhat.pt.getSizeBytes() > 0)
						{
							sw.writeln("\t_nsw.Write{0}({1}, {1}_length);", dtype, fld.getName());
						}
						else
						{
							sw.writeln("\tfor(_len=0; _len<{0}_length; _len++)", fld.getName());
							sw.writeln("\t\t_nsw.Write{0}({1}[_len]);", dtype, fld.getName());
						}
					}
				}
				else if (nswhat.hasRepeat)
				{
					sw.writeln("\t_nsw.Write{0}({1}, {2});", dtype, fld.getName(), fld.getRepeatInfo().toCodeString());
				}
				else
				{
					sw.writeln("\t_nsw.Write{0}({1});", dtype, fld.getName());
				}
			}
			else if(fld.getCustomType() instanceof BlindClass)
			{
				if (isAutoRepeat)
				{
					if (m_useStdVector)
					{
						sw.writeln("\tfor(std::vector<{0}>::const_iterator _itr={1}.begin(); _itr!={1}.end(); ++_itr)", fld.getType(), fld.getName());
						sw.writeln("\t\t(*_itr).SerializeTo(_nsw);", fld.getName());
					}
					else
					{
						sw.writeln("\tfor(_len=0; _len<{0}_length; _len++)", fld.getName());
						sw.writeln("\t\t{0}[_len].SerializeTo(_nsw);", fld.getName());
					}
				}
				else if (nswhat.hasRepeat)
				{
					sw.writeln("\tfor(_len=0; _len<{0}; _len++)", fld.getRepeatInfo().toCodeString());
					sw.writeln("\t\t{0}[_len].SerializeTo(_nsw);", fld.getName());
				}
				else
				{
					sw.writeln("\t{0}.SerializeTo(_nsw);", fld.getName());
				}
			}
			else
			{
				Struct st = (Struct) fld.getCustomType();

				if (isAutoRepeat)
				{
					if (m_useStdVector)
					{
						sw.writeln("\tfor(std::vector<{0}>::const_iterator _itr={1}.begin(); _itr!={1}.end(); ++_itr)", fld.getType(), fld.getName());
						sw.writeln("\t{");
						sw.writeln("\t\tconst {0}& _{1} = *_itr;", fld.getType(), fld.getName());
						printStructWriter(sw, 1, 2, st, '_' + fld.getName());
						sw.writeln("\t}");
					}
					else
					{
						sw.writeln("\tfor(_len=0; _len<{0}_length; _len++)", fld.getName());
						sw.writeln("\t{");
						printStructWriter(sw, 1, 2, st, fld.getName() + "[_len]");
						sw.writeln("\t}");
					}
				}
				else if (nswhat.hasRepeat)
				{
					sw.writeln("\tfor(_len=0; _len<{0}; _len++)", fld.getRepeatInfo().toCodeString());
					sw.writeln("\t{");
					printStructWriter(sw, 1, 2, st, fld.getName() + "[_len]");
					sw.writeln("\t}");
				}
				else
				{
					printStructWriter(sw, 1, 1, st, fld.getName());
				}
			}
		}

		sw.writeln("");

		if(bBuilderMethod)
		{
			sw.writeln("\treturn _nsw.ClosePacket(_buff, {0});", m_doc.getConfig().Use16BitPacketID ? 2 : 1);
			sw.writeln("}\n");
		}
		else
		{
			sw.writeln("\t{0}->Send(_buff, _nsw.ClosePacket(_buff, {1}));",
				VAR_SOCKETPTR, m_doc.getConfig().Use16BitPacketID ? 2 : 1);
			sw.writeln("}\n");
			
			if(pk.isGenerateBuilder())
				writeImpl_PacketSender(sw, cname, idprefix, pk, true);
		}
	}

	private void printStructReader(StreamWriter sw, int depth, int tabs, Struct st, String instance) throws Exception
	{
		for (StructField fld : st.getFields())
		{
			assert(fld.getRepeatInfo().getType() != RepeatInfo.TYPE_AUTO_VAR);
			NsWhat nswhat = new NsWhat();
			String dtype = getFieldNsWhat(fld, nswhat);

			if (dtype != null)
			{
				printTabs(sw, tabs);

				if (nswhat.hasRepeat)
				{
					sw.writeln("_nsr.Read{0}({3}.{1}, {2});", dtype, fld.getName(), fld.getRepeatInfo().getCount(), instance);
				}
				else
				{
					if(nswhat.pt.getSizeBytes() > 0)
						sw.writeln("{2}.{1} = _nsr.Read{0}();", dtype, fld.getName(), instance);
					else
						sw.writeln("_nsr.Read{0}(&{2}.{1});", dtype, fld.getName(), instance);
				}
			}
			else if (fld.getCustomType() instanceof BlindClass)
			{
				if (nswhat.hasRepeat)
				{
					printTabs(sw, tabs);
					sw.writeln("for(int _i{0}=0; _i{0}<{1}; _i{0}++)", depth, fld.getRepeatInfo().getCount());
					printTabs(sw, tabs + 1);
					sw.writeln("{1}.{0}[_i{2}].SerializeFrom(_nsw);", fld.getName(), instance, depth);
				}
				else
				{
					printTabs(sw, tabs);
					sw.writeln("{1}.{0}.SerializeFrom(_nsw);", fld.getName(), instance);
				}
			}
			else
			{
				Struct st2 = (Struct) fld.getCustomType();

				if (nswhat.hasRepeat)
				{
					printTabs(sw, tabs);
					sw.writeln("for(int _i{1}=0; _i{1}<{0}; _i{1}++)", fld.getRepeatInfo().getCount(), depth);
					printTabs(sw, tabs);
					sw.writeln("{");
					printStructReader(sw, depth + 1, tabs + 1, st2, instance + "." + fld.getName() + "[_i" + depth + "]");
					printTabs(sw, tabs);
					sw.writeln("}");
				}
				else
				{
					printStructReader(sw, depth + 1, tabs, st2, instance + "." + fld.getName());
				}
			}
		}
	}

	private void writeImpl_PacketReceiver(StreamWriter sw, String cname, Packet pk) throws Exception
	{
		if (!Util.isNullOrEmpty(pk.getComment()) && m_useComment)
			sw.writeln("// " + pk.getComment());

		sw.writeln("bool {0}::{1}{2}(NetStreamReader{3}& _nsr)",
			cname, PREFIX_PACKETRECEIVER, pk.getName(), m_swapEndian);

		sw.writeln("{");

		if (pk.getFields().size() == 0)
		{
			sw.writeln("\tif(_nsr.IsValid())");
			sw.writeln("\t\treturn {0}{1}();", m_prefixHandlerMethod, pk.getName());
			sw.writeln("");
		}
		else if (m_useDirectCast && pk.isDirectCasting())
		{
			sw.writeln("\tchar* p = _nsr.PrepareDirectCast({0});", pk.getDirectCastPacketLen());
			sw.writeln("\tif(p != NULL)");
			sw.writeln("\t\treturn {0}{1}(({2}{1}*)p);", m_prefixHandlerMethod, pk.getName(), m_doc.getConfig().Prefix.PacketStruct);
		}
		else
		{
			if (pk.hasVarLenRepeat())
			{
				sw.writeln("\tchar _buff[{0}], *_p = _buff;", pk.getMaxPacketBufferSize());
			}
			sw.writeln("\t{0}{1} _pkt;", m_doc.getConfig().Prefix.PacketStruct, pk.getName());
			if (pk.hasAutoRepeat())
			{
				sw.writeln("\tint _rep;");
			}
/*
			if (pk._RepeatDepth > 0)
			{
				sw.write("\tint ");
				for (int r = 0; r < pk._RepeatDepth; r++)
				{
					sw.write("r{0}", r);
					if (r < pk._RepeatDepth - 1)
						sw.write(", ");
				}
				sw.writeln(";");
			}
*/
			sw.writeln("");

			for (PacketField fld : pk.getFields())
			{
				boolean isAutoRepeat = (fld.getRepeatInfo().getType() == RepeatInfo.TYPE_AUTO_VAR);

				if(isAutoRepeat)
				{
					sw.writeln("\t_rep = _nsr.Read{0}();",
						fld.getRepeatInfo().getLimit() <= 0xFF ? "Byte" : "Word");
				}
/*
				if (fld.getRepeatInfo().Equals(Protocol.REPEAT_AUTO))
				{
					isAutoRepeat = true;
					sw.writeln("\t_rep = _nsr.ReadWord();");
				}
				else if (fld.getRepeatInfo().Equals(Protocol.REPEAT_AUTOTINY))
				{
					isAutoRepeat = true;
					sw.writeln("\t_rep = _nsr.ReadByte();");
				}
				else
				{
					isAutoRepeat = false;
				}
*/

				NsWhat nswhat = new NsWhat();
				String dtype = getFieldNsWhat(fld, nswhat);

				if(dtype != null)
				{
					if(isAutoRepeat)
					{
						sw.writeln("\t_p = _pkt.{0}._Alloc(_rep, _p);", fld.getName());
						sw.writeln("\t_nsr.Read{1}(_pkt.{0}.items, _rep);", fld.getName(), dtype);
/*
						if (pt.Bytes > 0)
						{
							sw.writeln("\t_nsr.Read{1}(_pkt.{0}.items, _rep);", fld.getName(), dtype);
						}
						else
						{
							sw.writeln("\tfor(int _i=0; _i<_rep; _i++)");
							sw.writeln("\t\t_nsr.Read{1}(&_pkt.{0}[_i]);", fld.getName(), dtype);
						}
*/
					}
					else if (nswhat.hasRepeat)
					{
						if(fld.getRepeatInfo().isVariableRepeat())
						{
							// variable-length repeat
							sw.writeln("\t_p = _pkt.{0}._Alloc(_pkt.{1}, _p);", fld.getName(), fld.getRepeatInfo().getReference());
							sw.writeln("\t_nsr.Read{0}(_pkt.{1}.items, _pkt.{2});", dtype, fld.getName(), fld.getRepeatInfo().getReference());
						}
						else
						{
							sw.writeln("\t_nsr.Read{0}(_pkt.{1}, {2});", dtype, fld.getName(), fld.getRepeatInfo().toCodeString());
						}
					}
					else
					{
						if (nswhat.pt.getSizeBytes() > 0)
							sw.writeln("\t_pkt.{0} = _nsr.Read{1}();", fld.getName(), dtype);
						else
							sw.writeln("\t_nsr.Read{1}(&_pkt.{0});", fld.getName(), dtype);
					}
				}
				else if(fld.getCustomType() instanceof BlindClass)
				{
					if (isAutoRepeat)
					{
						sw.writeln("\t_p = _pkt.{0}._Alloc(_rep, _p);", fld.getName());
						sw.writeln("\tfor(int _i=0; _i<_rep; _i++)");
						sw.writeln("\t\t_pkt.{0}[_i].SerializeFrom(_nsr);", fld.getName());
					}
					else if (nswhat.hasRepeat)
					{
						if(fld.getRepeatInfo().isVariableRepeat())
						{
							sw.writeln("\t_p = _pkt.{0}._Alloc(_pkt.{1}, _p);", fld.getName(), fld.getRepeatInfo().getReference());
							sw.writeln("\tfor(int _i=0; _i<_pkt.{0}; _i++)", fld.getRepeatInfo().getReference());
						}
						else
						{
							sw.writeln("\tfor(int _i=0; _i<{0}; _i++)", fld.getRepeatInfo().getCount());
						}

						sw.writeln("\t\t_pkt.{0}[_i].SerializeFrom(_nsr);", fld.getName());
					}
					else
					{
						sw.writeln("\t_pkt.{0}.SerializeFrom(_nsr);", fld.getName());
					}
				}
				else
				{
					Struct st = (Struct) fld.getCustomType();

					if (isAutoRepeat)
					{
						sw.writeln("\t_p = _pkt.{0}._Alloc(_rep, _p);", fld.getName());
						sw.writeln("\tfor(int _i=0; _i<_rep; _i++)");
						sw.writeln("\t{");
						printStructReader(sw, 1, 2, st, "_pkt." + fld.getName() + "[_i]");
						sw.writeln("\t}");
					}
					else if (nswhat.hasRepeat)
					{
						if(fld.getRepeatInfo().isVariableRepeat())
						{
							sw.writeln("\t_p = _pkt.{0}._Alloc(_pkt.{1}, _p);", fld.getName(), fld.getRepeatInfo().getReference());
							sw.writeln("\tfor(int _i=0; _i<_pkt.{0}; _i++)", fld.getRepeatInfo().getReference());
							sw.writeln("\t{");
							printStructReader(sw, 1, 2, st, "_pkt." + fld.getName() + "[_i]");
							sw.writeln("\t}");
						}
						else
						{
							sw.writeln("\tfor(int _i=0; _i<{0}; _i++)", fld.getRepeatInfo().getCount());
							sw.writeln("\t{");
							printStructReader(sw, 1, 2, st, "_pkt." + fld.getName() + "[_i]");
							sw.writeln("\t}");
						}
					}
					else
					{
						printStructReader(sw, 1, 1, st, "_pkt." + fld.getName());
					}
				}
			}

			if (pk.getFields().size() > 0)
				sw.writeln("");

			sw.writeln("\tif(_nsr.IsValid())");
			sw.writeln("\t\treturn {0}{1}(&_pkt);", m_prefixHandlerMethod, pk.getName());
			sw.writeln("");
		}

		sw.writeln("\treturn false;");
		sw.writeln("}\n");
	}
}
