package bpdtool.codegen;

import bpdtool.Main;
import bpdtool.Util;
import bpdtool.data.*;
import bpdtool.gui.MainFrame;

import java.util.ArrayList;

// Node.js JavaScript code generator
public class JsCodeGenerator extends CodeGenerator
{
	private final String PREFIX_PACKETRECEIVER = "recv_";
	private final String VAR_PACKETDISPATCHTABLE = "PacketDispatchTable";
	private final String VAR_PACKETDISPATCHTABLESTAGES = "PacketDispatchTableStages";
	private final String SUFFIX_FILEEXT = ".js";

	private boolean m_useComment;
	private String m_LEorBE;
	private String m_jsWideStringEncoding;
	private String m_prefixSendPacketID;
	private String m_prefixRecvPacketID;
	private int m_nPktHeaderLen;    // 3 or 4
	private boolean m_useStage;

	@Override
	public boolean prepare(ITextWriter logger, Protocol doc)
	{
		m_useComment = !doc.getConfig().NoExportComment;
		m_useStage = !doc.getConfig().NoUsePacketDispatchStage && (doc.getPacketStages().size() > 1);

		// FIXME, TODO
		m_LEorBE = "LE";
		m_jsWideStringEncoding = "'utf16le'";

		m_nPktHeaderLen = doc.getConfig().Use16BitPacketID ? 4 : 3;

		if (getCsRole() == Protocol.ROLE_CLIENT)
		{
			if (Util.isNullOrEmpty(doc.getConfig().Js.Client.ClassName))
			{
				logger.writeln("JavaScript client class name is empty.");
				return false;
			}
		}
		else
		{
			if (Util.isNullOrEmpty(doc.getConfig().Js.Server.ClassName))
			{
				logger.writeln("JavaScript server class name is empty.");
				return false;
			}
		}

		for (PacketGroup grp : doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				for (PacketField fld : pk.getFields())
				{
					if (fld.isCustomType())
					{
						logger.writeln("JavaScript: Custom packet field type is not supported(implemented) yet.");
						return false;
					}
					else
					{
						PrimitiveType pt = fld.getPrimitiveType();
						if (pt.getSizeBytes() >= 8)
						{
							logger.writeln("JavaScript: 64-bit numeric value type is not supported.");
							return false;
						}
					}
				}
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
			m_prefixSendPacketID = m_doc.getConfig().Prefix.S2CPacketID;
			m_prefixRecvPacketID = m_doc.getConfig().Prefix.C2SPacketID;
			generateServer();
		}
		else
		{
			m_prefixSendPacketID = m_doc.getConfig().Prefix.C2SPacketID;
			m_prefixRecvPacketID = m_doc.getConfig().Prefix.S2CPacketID;
			generateClient();
		}

		return true;
	}

	private String getConstants()
	{
		StringWriter sw = new StringWriter();

		for (Constant ct : m_doc.getConstants())
		{
			if (!Util.isNullOrEmpty(ct.getComment()) && m_useComment)
				sw.writeln("// " + ct.getComment());

			if (ct.isEnum())
			{
				int nEnumValue = 0;
				getTabWriteBuffer().begin();
				for (int i = 0; i < ct.getFields().size(); i++)
				{
					ConstantField fld = ct.getFields().elementAt(i);
					getTabWriteBuffer().write("exports.{0}\t = ", fld.getName());

					if (fld.isValueSet())
					{
						getTabWriteBuffer().write("{0};", fld.getValue());

						if(fld.getValue().startsWith("0x"))
							nEnumValue = Integer.parseInt(fld.getValue().substring(2), 16);
						else
							nEnumValue = Integer.parseInt(fld.getValue());
					}
					else
					{
						getTabWriteBuffer().write("{0};", nEnumValue);
					}

					++nEnumValue;

					if (!Util.isNullOrEmpty(fld.getComment()) && m_useComment)
						getTabWriteBuffer().writeln(" // " + fld.getComment());
					else
						getTabWriteBuffer().writeln("");
				}
				getTabWriteBuffer().end(sw, 1);
			}
			else
			{
				getTabWriteBuffer().begin();
				for (ConstantField fld : ct.getFields())
				{
					getTabWriteBuffer().write("exports.{0}\t= {1};", fld.getName(), fld.getValue());

					if (!Util.isNullOrEmpty(fld.getComment()) && m_useComment)
						getTabWriteBuffer().writeln(" // " + fld.getComment());
					else
						getTabWriteBuffer().writeln("");
				}
				getTabWriteBuffer().end(sw, 0);
			}
		}

		if (m_useStage && getCsRole() == Protocol.ROLE_SERVER)
		{
			sw.writeln("");

			if (m_useComment)
				sw.writeln("// Packet Stages");

			getTabWriteBuffer().begin();
			int i = 0;
			for(PacketStage ps : m_doc.getPacketStages())
			{
				getTabWriteBuffer().writeln("exports.{0}\t={1}; // {2}", ps.getName(), ps.getIndex(), ps.getAbbr());
			}
			getTabWriteBuffer().end(sw, 0);
		}

		return sw.toString();
	}

	private String getPacketIDs()
	{
		StringWriter swC2S = new StringWriter();
		StringWriter swS2C = new StringWriter();

		if (m_useComment)
		{
			swC2S.writeln("// Client->Server Packet IDs:");
			swS2C.writeln("// Server->Client Packet IDs:");
		}

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_S2C)
				{
					swC2S.writeln("var {0}{1}\t= {2};", m_doc.getConfig().Prefix.C2SPacketID, pk.getName(), getIdStr(pk.getExportC2SID()));
				}

				if (pk.getFlow() != Packet.FLOW_C2S)
				{
					swS2C.writeln("var {0}{1}\t= {2};", m_doc.getConfig().Prefix.S2CPacketID, pk.getName(), getIdStr(pk.getExportS2CID()));
				}
			}
		}

		swC2S.writeln("var {0}{1}\t= {2};", m_doc.getConfig().Prefix.C2SPacketID, SUFFIX_LASTPACKETID, getIdStr(getExporter().getMaxC2SPacketID()));
		swS2C.writeln("var {0}{1}\t= {2};", m_doc.getConfig().Prefix.S2CPacketID, SUFFIX_LASTPACKETID, getIdStr(getExporter().getMaxS2CPacketID()));

		getTabWriteBuffer().begin();
		getTabWriteBuffer().writeln(swC2S.toString());
		getTabWriteBuffer().writeln(swS2C.toString());
		StringWriter sw = new StringWriter();
		getTabWriteBuffer().end(sw, 0);

		return sw.toString();
	}

	private TemplateBuilder getTemplate(String templFilename) throws Exception
	{
		TemplateBuilder templ = new TemplateBuilder(Main.getCodeTemplate(templFilename));

		templ.substitute("$$LEBE$$", m_LEorBE);
		templ.substitute("$$PacketIdType$$", getJsBufferDtypeForLengthPrefix(m_nPktHeaderLen == 3));
		templ.substitute("$$PacketDispatchTable$$", VAR_PACKETDISPATCHTABLE);
		templ.substitute("$$PacketDispatchTableStages$$", VAR_PACKETDISPATCHTABLESTAGES);
		templ.substitute("$$PacketHeaderLen$$", Integer.toString(m_nPktHeaderLen));
		templ.substitute("$$PacketLengthOffset$$", Integer.toString(m_nPktHeaderLen -2));
		templ.substitute("$$HandlerPrefix$$", m_doc.getConfig().Prefix.HandlerMethod);
		templ.substitute("$$Recv_LastPacketID$$", m_prefixRecvPacketID + SUFFIX_LASTPACKETID);

		templ.substitute("$$Constants$$", getConstants());
		templ.substitute("$$PacketIDs$$", getPacketIDs());

		return templ;
	}

	private void writeReceivers(TemplateBuilder templ, int flowToSkip)
	{
		StringWriter wr = new StringWriter();

		StringWriter swCHE = new StringWriter();
		swCHE.writeln("\tif(!isCheckedHandlers)");
		swCHE.writeln("\t{");

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != flowToSkip)
				{
					writePacketReceiver(wr, pk);
					swCHE.writeln("\t\tcheckHandlerExists(this.{0}{1}, '{1}');",
						m_doc.getConfig().Prefix.HandlerMethod, pk.getName());
				}
			}
		}

		swCHE.writeln("\t\tisCheckedHandlers = true;");
		swCHE.writeln("\t}");

		templ.substitute("$$Receivers$$", wr.toString());
		templ.substitute("$$CheckHandlersExist$$", swCHE.toString());
	}

	private void writeSenders(TemplateBuilder templ, int flowToSkip)
	{
		StringWriter wr = new StringWriter();

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != flowToSkip)
					writePacketSender(wr, pk);
			}
		}

		templ.substitute("$$Senders$$", wr.toString());
	}

	public void generateServer() throws Exception
	{
		TemplateBuilder templ = getTemplate("nodesock.js");

		ArrayList<String> conds = new ArrayList<>();
		conds.add("server");
		if (m_useStage)
			conds.add("useStage");
		templ.selectCondition(conds);

		writeReceivers(templ, Packet.FLOW_S2C);
		writeSenders(templ, Packet.FLOW_C2S);
		writePacketDispatchTable(templ, m_useStage);

		String filename = '_' + m_doc.getConfig().Js.Server.ClassName + SUFFIX_FILEEXT;
		StreamWriter sw = openCodeStream(filename);
		sw.write(templ.getResult());
		closeCodeStream(sw);

		m_logger.writeln("Generated server file: " + filename);
	}

	public void generateClient() throws Exception
	{
		TemplateBuilder templ = getTemplate("nodesock.js");
		templ.selectCondition("client");

		writeReceivers(templ, Packet.FLOW_C2S);
		writeSenders(templ, Packet.FLOW_S2C);
		writePacketDispatchTable(templ, false);

		String filename = '_' + m_doc.getConfig().Js.Client.ClassName + SUFFIX_FILEEXT;
		StreamWriter sw = openCodeStream(filename);
		sw.write(templ.getResult());
		closeCodeStream(sw);

		m_logger.writeln("Generated client file: " + filename);
	}

	private String getJsBufferNumericDataType(PrimitiveType pt)
	{
		String dtype = null;

		final int sizeBytes = pt.getSizeBytes();
		switch (pt.getCategory())
		{
		case PrimitiveType.SIGNED_INTEGER:
			switch (sizeBytes)
			{
			case 1:
				dtype = "Int8";
				break;
			case 2:
				dtype = "Int16";
				break;
			case 4:
				dtype = "Int32";
				break;
			}
			break;

		case PrimitiveType.UNSIGNED_INTEGER:
			switch (sizeBytes)
			{
			case 1:
				dtype = "UInt8";
				break;
			case 2:
				dtype = "UInt16";
				break;
			case 4:
				dtype = "UInt32";
				break;
			}
			break;

		case PrimitiveType.BOOLEAN:
			dtype = "Int8";
			break;

		case PrimitiveType.FLOAT:
			switch (sizeBytes)
			{
			case 4:
				dtype = "Float";
				break;
			case 8:
				dtype = "Double";
				break;
			}
			break;
		}

		if (dtype == null)
		{
			throw new RuntimeException("Invalid packet field type: " + pt.getDescription());
		}

		return (dtype + m_LEorBE);
/*
		case PrimitiveType.STRING:
		case PrimitiveType.STRING_TINY:
		case PrimitiveType.WIDESTRING:
		case PrimitiveType.WIDESTRING_TINY:
			return "String";

		case PrimitiveType.BUFFER:
		case PrimitiveType.BUFFER_TINY:
			return "ByteArray";
*/
	}

	private String getJsBufferDtypeForLengthPrefix(boolean isTiny)
	{
		return (isTiny ? "UInt8" : ("UInt16" + m_LEorBE));
	}

	private void writeReader_String(ITextWriter sw, PacketField fld, int lengthBytes, boolean doAdvanceOffset)
	{
		String dtype = getJsBufferDtypeForLengthPrefix(lengthBytes == 1);
		String enc = (lengthBytes == 1) ? "undefined" : m_jsWideStringEncoding;

		sw.writeln("\tvar len_{0} = buf.read{1}(offset); offset += {2};", fld.getName(), dtype, lengthBytes);
		sw.writeln("\tif (len_{0} > 0)", fld.getName());
		sw.writeln("\t{");
		sw.writeln("\t\tpkt.{0} = buf.toString({1}, offset, offset + len_{0});", fld.getName(), enc);
		if (doAdvanceOffset)
			sw.writeln("\t\toffset += len_{0} + {1};", fld.getName(), lengthBytes);
		sw.writeln("\t}");
	}

	private void writePacketReceiver(ITextWriter sw, Packet pk)
	{
		if (!Util.isNullOrEmpty(pk.getComment()) && m_useComment)
			sw.writeln("// " + pk.getComment());

		sw.writeln("function {0}{1}(self, buf, offset)\n{", PREFIX_PACKETRECEIVER, pk.getName());

		if (pk.getFields().size() == 0)
		{
			sw.writeln("\tself.{0}{1}();", m_doc.getConfig().Prefix.HandlerMethod, pk.getName());
		}
		else
		{
			sw.writeln("\tvar pkt = new Object();\n");

			int iFld = 0;
			for (PacketField fld : pk.getFields())
			{
				boolean isLastField = (++iFld == pk.getFields().size());

				if (fld.isCustomType())
				{
					throw new RuntimeException("JS Custom Packet Field: NOT IMPL");
				}
				else
				{
					PrimitiveType pt = fld.getPrimitiveType();
					if (pt.getSizeBytes() > 0)
					{
						sw.write("\tpkt.{0} = buf.read{1}(offset);", fld.getName(), getJsBufferNumericDataType(pt));
						if (isLastField)
							sw.writeln("");
						else
							sw.writeln(" offset += {0};", pt.getSizeBytes());
					}
					else if (pt.getCategory() == PrimitiveType.STRING || pt.getCategory() == PrimitiveType.STRING_TINY)
					{
						writeReader_String(sw, fld, 1, !isLastField);
					}
					else if (pt.getCategory() == PrimitiveType.STRING || pt.getCategory() == PrimitiveType.STRING_TINY)
					{
						writeReader_String(sw, fld, 2, !isLastField);
					}
				}

				sw.writeln("");
			}

			sw.writeln("\tself.{0}{1}(self, pkt);", m_doc.getConfig().Prefix.HandlerMethod, pk.getName());
		}

		sw.writeln("}\n");
	}

	private void writePacketDispatchTable(TemplateBuilder templ, boolean useStage)
	{
		StringWriter sw = new StringWriter();

		int skipFlow = (getCsRole() == Protocol.ROLE_SERVER) ? Packet.FLOW_S2C : Packet.FLOW_C2S;

		String tableArraySize = (m_prefixRecvPacketID + SUFFIX_LASTPACKETID + " +1");

		if (useStage)
		{
			sw.writeln("var {0};", VAR_PACKETDISPATCHTABLE);
			sw.writeln("var {0} = new Array({1});", VAR_PACKETDISPATCHTABLESTAGES, m_doc.getPacketStages().size());
			for(PacketStage ps : m_doc.getPacketStages())
			{
				sw.writeln("{0}[exports.{1}] = new Array({2})", VAR_PACKETDISPATCHTABLESTAGES, ps.getName(), tableArraySize);

				int ii = 0;
				for (PacketGroup grp : m_doc.getPacketGroups())
				{
					for (Packet pk : grp.getPackets())
					{
						if (pk.getFlow() != skipFlow)
						{
							if (pk.isAllStage() || pk.getStages().indexOf(ps) >= 0)
							{
								sw.writeln("{0}[exports.{1}][{2}{3}] = {4}{3};", VAR_PACKETDISPATCHTABLESTAGES, ps.getName(),
									m_prefixRecvPacketID, pk.getName(), PREFIX_PACKETRECEIVER);
							}
						}
					}
				}
			}

			templ.substitute("$$MaxPacketStage$$", Integer.toString(m_doc.getPacketStages().size()));
		}
		else
		{
			sw.writeln("var {0} = new Array({1});", VAR_PACKETDISPATCHTABLE, tableArraySize);

			for (PacketGroup grp : m_doc.getPacketGroups())
			{
				for (Packet pk : grp.getPackets())
				{
					if (pk.getFlow() != skipFlow)
					{
						sw.writeln("{0}[{1}{2}] = {3}{2};", VAR_PACKETDISPATCHTABLE,
							m_prefixRecvPacketID, pk.getName(), PREFIX_PACKETRECEIVER);
					}
				}
			}
		}

		templ.substitute("$$PacketDispatchTableSetup$$", sw.toString());

		if (useStage)
		{
			sw.reset();
			sw.writeln("\n\t{0} = {1}[0];", VAR_PACKETDISPATCHTABLE, VAR_PACKETDISPATCHTABLESTAGES);
			templ.substitute("$$ResetPacketDispatchTable$$", sw.toString());
		}
		else
		{
			templ.substitute("$$ResetPacketDispatchTable$$", "\n");
		}
	}

	private void writePacketSender(ITextWriter sw, Packet pk)
	{
		if (!Util.isNullOrEmpty(pk.getComment()) && m_useComment)
			sw.writeln("// " + pk.getComment());

		sw.write("Instance.prototype.{0}{1} = function(", m_doc.getConfig().Prefix.SenderMethod, pk.getName());
		if (pk.getFields().size() > 0)
		{
			for (int i = 0; i < pk.getFields().size();)
			{
				PacketField fld = pk.getFields().get(i);
				sw.write(fld.getName());

				if (++i < pk.getFields().size())
					sw.write(", ");
			}
		}

		sw.writeln(")\n{");

		if (pk.isFixedLengthPacket())
		{
			sw.writeln("\tvar buf = new Buffer({0});", m_nPktHeaderLen + pk.getDirectCastPacketLen());
			sw.writeln("\tbuf.write{0}({1}{2}, 0);", getJsBufferDtypeForLengthPrefix(m_nPktHeaderLen == 3), m_prefixSendPacketID, pk.getName());
			sw.writeln("\tbuf.writeInt16LE(buf.length, {0});", m_nPktHeaderLen -2);
			sw.writeln("");

			int offset = m_nPktHeaderLen;
			for (PacketField fld : pk.getFields())
			{
				PrimitiveType pt = fld.getPrimitiveType();
				sw.writeln("\tbuf.write{0}({1}, {2});", getJsBufferNumericDataType(pt), fld.getName(), offset);
				offset += fld.getPrimitiveType().getSizeBytes();
			}

			sw.writeln("\n\tthis._socket.write(buf);");
		}
		else
		{
			int fixedLen = m_nPktHeaderLen;
			StringBuilder sbLenVars = new StringBuilder();
			for (PacketField fld : pk.getFields())
			{
				PrimitiveType pt = fld.getPrimitiveType();

				if (pt.getSizeBytes() > 0)
				{
					fixedLen += pt.getSizeBytes();
				}
				else
				{
					fixedLen += pt.isVariableLengthTiny() ? 1:2;

					switch (pt.getCategory())
					{
					case PrimitiveType.STRING:
					case PrimitiveType.STRING_TINY:
						sw.writeln("\tvar len_{0} = Buffer.byteLength({0});", fld.getName());
						fixedLen += 1;
						break;

					case PrimitiveType.WIDESTRING:
					case PrimitiveType.WIDESTRING_TINY:
						sw.writeln("\tvar len_{0} = Buffer.byteLength({0}, {1});", fld.getName(), m_jsWideStringEncoding);
						fixedLen += 2;
						break;

					case PrimitiveType.BUFFER:
					case PrimitiveType.BUFFER_TINY:
						sw.writeln("\tvar len_{0} = {0}.length;", fld.getName());
						break;

					default:
						throw new RuntimeException("Unimplemented data type: " + fld.getDescription());
					}

					sbLenVars.append(" + len_");
					sbLenVars.append(fld.getName());
				}
			}

			sw.writeln("\tvar buf = new Buffer({0}{1});", fixedLen, sbLenVars.toString());
			sw.writeln("");
			sw.writeln("\tvar offset = {0};", m_nPktHeaderLen);

			for (PacketField fld : pk.getFields())
			{
				PrimitiveType pt = fld.getPrimitiveType();

				if (pt.getSizeBytes() > 0)
				{
					sw.writeln("\tbuf.write{0}({1}, offset); offset += {2}",
						getJsBufferNumericDataType(pt), fld.getName(), pt.getSizeBytes());
				}
				else
				{
					sw.writeln("\tbuf.write{0}(len_{1}, offset); offset += {2};",
						getJsBufferDtypeForLengthPrefix(pt.isVariableLengthTiny()),
						fld.getName(), pt.isVariableLengthTiny() ? 1:2);
					sw.writeln("\tif(len_{0} > 0)", fld.getName());
					sw.writeln("\t{");

					switch (pt.getCategory())
					{
					case PrimitiveType.STRING:
					case PrimitiveType.STRING_TINY:
						sw.writeln("\t\toffset += buf.write({0}, offset);", fld.getName());
						sw.writeln("\t\tbuf.writeInt8(0, offset); offset += 1;");
						break;

					case PrimitiveType.WIDESTRING:
					case PrimitiveType.WIDESTRING_TINY:
						sw.writeln("\t\toffset += buf.write({0}, offset, {1});", fld.getName(), m_jsWideStringEncoding);
						sw.writeln("\t\tbuf.writeInt16(0, offset); offset += 2;");
						break;

					case PrimitiveType.BUFFER:
					case PrimitiveType.BUFFER_TINY:
						sw.writeln("\t\t{0}.copy(buf, offset);", fld.getName());
						sw.writeln("\t\toffset += {0}.length;", fld.getName());
						break;
					}

					sw.writeln("\t}");
				}
			}

			sw.writeln("\n\tsendPacket(this, {0}{1}, buf, offset);", m_prefixSendPacketID, pk.getName());
		}

		sw.writeln("}");
		sw.writeln("");
	}
}
