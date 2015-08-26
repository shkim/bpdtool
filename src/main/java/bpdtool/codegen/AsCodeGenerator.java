package bpdtool.codegen;

import bpdtool.Main;
import bpdtool.data.*;
import bpdtool.Util;
import bpdtool.gui.MainFrame;

// (ADOBE FLASH) ActionScript3 Code Generator 
public class AsCodeGenerator extends CodeGenerator
{
	private final String PREFIX_PACKETRECEIVER = "_recv_";

	private boolean m_bServerIsBigEndian;
	private String m_strPacketPackageName;
	private String m_strBasePackageName;

	private boolean m_useComment;

	public AsCodeGenerator()
	{
		m_bServerIsBigEndian = false;
	}

	@Override
	public boolean prepare(ITextWriter logger, Protocol doc)
	{
		m_useComment = !doc.getConfig().NoExportComment;

		if (getCsRole() == Protocol.ROLE_CLIENT)
		{
			if (Util.isNullOrEmpty(doc.getConfig().As3.Client.ClassName))
			{
				logger.writeln("ActionScript client class name is empty.");
				return false;
			}
		}

		return true;
	}
	
	@Override
	public boolean export(ITextWriter logger) throws Exception
	{
		m_logger = logger;

		if (getCsRole() == Protocol.ROLE_CLIENT)
		{
			generateClient();
		}
		else
		{
			throw new Exception("ActionScript is not supported as a server.");
		}

		return true;
	}

	public void setBigEndian()
	{
		m_bServerIsBigEndian = true;
	}

	public void generateClient() throws Exception
	{
		String classname = "_" + m_doc.getConfig().As3.Client.ClassName;
		String filename = classname + (m_doc.getConfig().As3.Client.MergeAll ? ".merged.as" : ".as");
		String lsnrclass = "_I" + m_doc.getConfig().As3.Client.ClassName + "Listener";
		String lsnr_fname = lsnrclass + ".as";
		m_strPacketPackageName = classname + "Pkts";
		m_strBasePackageName = m_doc.getConfig().As3.Client.UsePackageName ? m_doc.getConfig().As3.Client.PackageName : "";

		StreamWriter sw = openCodeStream(filename);

		TemplateBuilder tw = new TemplateBuilder(Main.getCodeTemplate("main.as"));
		
		if(m_doc.getConfig().As3.Client.ForStressTester)
		{
			//tw.substitute("$$StressImports$$", "import StressTestEnv;\n");
			tw.substitute("$$StressEnvArg$$", ", stEnv:StressTestEnv");
			tw.substitute("$$StressVars$$", "\tprivate var m_stEnv:StressTestEnv;\n"
				+ "\tprivate var m_HandlerTable:Object;\n"
				+ "\tprivate var m_curState:String;\n"
				+ "\tprivate var m_states:Object;\n"
				+ "\tprivate var m_name2id:Object;");

			StringWriter n2i = new StringWriter();
			n2i.writeln("\t\tm_stEnv = stEnv;");
			n2i.writeln("\t\tm_states = new Object();");
			n2i.writeln("\t\tm_name2id = new Object();");
			for (PacketGroup grp : m_doc.getPacketGroups())
			{
				for (Packet pkt : grp.getPackets())
				{
					if (pkt.getFlow() != Packet.FLOW_C2S)
					{
						n2i.writeln("\t\tm_name2id['{0}'] = {1};", pkt.getName(), pkt.getExportS2CID());
					}
				}
			}
			tw.substitute("$$Name2IDs$$", n2i.toString());
			
			TemplateBuilder tw2 = new TemplateBuilder(Main.getCodeTemplate("stt.as"));
			tw.substitute("$$StressFuncs$$", tw2.getResult());
		}
		else
		{
			tw.substitute("$$TestEnvArg$$", "");
			tw.substitute("$$StressVars$$", "");
			tw.substitute("$$Name2IDs$$", "");
			tw.substitute("$$StressFuncs$$", "");
		}

		tw.substitute("$$PackageName$$", " " + m_strBasePackageName);
		tw.substitute("$$PacketPkgName$$", m_strPacketPackageName);
		tw.substitute("$$ClassName$$", classname);
		tw.substitute("$$ListenerName$$", lsnrclass);
		tw.substitute("$$LsnrMethodPrefix$$", m_doc.getConfig().As3.Client.ClassName);
		tw.substitute("$$PacketIdType$$", m_doc.getConfig().Use16BitPacketID ? "Short" : "Byte");
		tw.substitute("$$PacketHeaderLen$$", m_doc.getConfig().Use16BitPacketID ? "4" : "3");
		tw.substitute("$$Endian$$", m_bServerIsBigEndian ? "BIG_ENDIAN" : "LITTLE_ENDIAN");
		tw.substitute("$$CommonConstants$$", getCommonConstants());
		tw.substitute("$$PacketIDs$$", getConstants());
		tw.substitute("$$HandlerFill$$", getHandlerArray());

		sw.write(tw.getResult());

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_C2S)
					writePacketReceiver(sw, pk);
			}
		}

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_S2C)
					writePacketSender(sw, pk);
			}
		}

		sw.writeln("} // {0}", classname);

		if(!m_doc.getConfig().As3.Client.MergeAll)
		{
			sw.writeln("}\n");	// package close

			closeCodeStream(sw);
			m_logger.writeln("Generated client socket main class file: " + filename);


			// Listener interface file
			sw = openCodeStream(lsnr_fname);
			if(m_doc.getConfig().As3.Client.UsePackageName)
				sw.writeln("package {0}", m_strBasePackageName);
			else
				sw.writeln("package");

			sw.writeln("{");
			sw.writeln("\timport flash.events.Event;");
			sw.writeln("\timport {0}.*;", m_strPacketPackageName);
		}

		sw.writeln("");
		sw.writeln("\tpublic interface {0}", lsnrclass);
		sw.writeln("\t{");
		sw.writeln("\t\tfunction {0}_OnConnect(e:Event):void;", m_doc.getConfig().As3.Client.ClassName);
		sw.writeln("\t\tfunction {0}_OnClose(e:Event):void;", m_doc.getConfig().As3.Client.ClassName);
		sw.writeln("\t\tfunction {0}_OnError(e:Event):void;", m_doc.getConfig().As3.Client.ClassName);
		sw.writeln("\t\tfunction {0}_OnInvalidPacket(packet_id:int):void;", m_doc.getConfig().As3.Client.ClassName);
		sw.writeln("");
		getHandlerDecls(sw);

/*
		tw.Reset(ProtocolDesigner.Main.GetCodeTemplate("lsnr.as"));
		tw.substitute("$$PackageName$$", " "+m_strBasePackageName);
		tw.substitute("$$PacketPkgName$$", m_strPacketPackageName);
		tw.substitute("$$ClassName$$", lsnrclass);
		tw.substitute("$$HandlerFuncs$$", m_doc.OptAs3StressTest ? "" : GetHandlerDecls());
		sw.Write(tw.GetResult());
 */

		sw.writeln("\t}");
		sw.writeln("}");

		if(!m_doc.getConfig().As3.Client.MergeAll)
		{
			closeCodeStream(sw);
			m_logger.writeln("Generated client socket listener interface file: " + lsnr_fname);

			// Packet structures (class)
			for (PacketGroup grp : m_doc.getPacketGroups())
			{
				for (Packet pk : grp.getPackets())
				{
					if (pk.getFlow() != Packet.FLOW_C2S)
					{
						if (pk.getFields().size() > 0)
							makePacketClass(pk);
					}
				}
			}
		}
		else
		{
			String basepkg = m_doc.getConfig().As3.Client.UsePackageName ? (m_doc.getConfig().As3.Client.PackageName + ".") : "";
			sw.writeln("\npackage " + basepkg + m_strPacketPackageName);
			sw.write("{");

			// Packet structures (class)
			for (PacketGroup grp : m_doc.getPacketGroups())
			{
				for (Packet pk : grp.getPackets())
				{
					if (pk.getFlow() != Packet.FLOW_C2S)
					{
						if (pk.getFields().size() > 0)
						{
							sw.writeln("");
							String className = m_doc.getConfig().Prefix.PacketStruct + pk.getName();
							writePacketClass(sw, pk, className);
						}
					}
				}
			}
			
			sw.writeln("}");
			closeCodeStream(sw);
			m_logger.writeln("Generated client socket all-in-one file: " + filename);
		}
	}

	private String getConstants()
	{
		StringWriter sw = new StringWriter();
		int id;

		String idvtype = "int";//m_doc.Opt16BitID ? "ushort" : "byte";

		// C2S and INTER
		if (m_useComment)
		{
			sw.writeln("\t// Client->Server Packet IDs:");
		}

		getTabWriteBuffer().begin();
		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_S2C)
				{
					getTabWriteBuffer().writeln("public static const {0}{1}\t:{2} = {3};", m_doc.getConfig().Prefix.C2SPacketID, pk.getName(), idvtype, getIdStr(pk.getExportC2SID()));
				}
			}
		}
		getTabWriteBuffer().writeln("private static const {0}{1}\t:{2} = {3};", m_doc.getConfig().Prefix.C2SPacketID, SUFFIX_LASTPACKETID, idvtype, getIdStr(getExporter().getMaxC2SPacketID()));
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
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_C2S)
				{
					getTabWriteBuffer().writeln("public static const {0}{1}\t:{2} = {3};", m_doc.getConfig().Prefix.S2CPacketID, pk.getName(), idvtype, getIdStr(pk.getExportS2CID()));
				}
			}
		}
		getTabWriteBuffer().writeln("private static const {0}{1}\t:{2} = {3};", m_doc.getConfig().Prefix.S2CPacketID, SUFFIX_LASTPACKETID, idvtype, getIdStr(getExporter().getMaxS2CPacketID()));
		getTabWriteBuffer().end(sw, 1);

		return sw.toString();
	}

	private String getHandlerArray()
	{
		StringWriter sw = new StringWriter();

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_C2S)
				{
					sw.writeln("\t\tm_receiverTable[{0}{1}] = {2}{1};",
						m_doc.getConfig().Prefix.S2CPacketID, pk.getName(), PREFIX_PACKETRECEIVER);
				}
			}
		}

		return sw.toString();
	}

	private void getHandlerDecls(StreamWriter sw)
	{
		if(m_doc.getConfig().As3.Client.ForStressTester)
			sw.writeln("\t\t// Packet Handlers: (commented for Stress Test Script)");
		else
			sw.writeln("\t\t// Packet Handlers:");

		for (PacketGroup grp : m_doc.getPacketGroups())
		{
			for (Packet pk : grp.getPackets())
			{
				if (pk.getFlow() != Packet.FLOW_C2S)
				{
					sw.write("\t\t");
					if (m_doc.getConfig().As3.Client.ForStressTester)
						sw.write("//");

					sw.write("function {0}{1}(", m_doc.getConfig().Prefix.HandlerMethod, pk.getName());
					if (pk.getFields().size() > 0)
						sw.write("pkt:{0}{1}", m_doc.getConfig().Prefix.PacketStruct, pk.getName());
					sw.writeln("):Boolean;");
				}
			}
		}
	}

	private String getCommonConstants()
	{
		StringWriter sw = new StringWriter();
		sw.writeln("");

		for (Constant ct : m_doc.getConstants())
		{
			if (!Util.isNullOrEmpty(ct.getComment()) && m_useComment)
				sw.writeln("\t// " + ct.getComment());

			if (ct.isEnum())
			{
				int nEnumValue = 0;
				getTabWriteBuffer().begin();
				for (int i = 0; i < ct.getFields().size(); i++)
				{
					ConstantField fld = ct.getFields().elementAt(i);
					getTabWriteBuffer().write("public static const {0}\t:int = ", fld.getName());

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
					getTabWriteBuffer().write("public static const {0}\t:int = {1};", fld.getName(), fld.getValue());

					if (!Util.isNullOrEmpty(fld.getComment()) && m_useComment)
						getTabWriteBuffer().writeln(" // " + fld.getComment());
					else
						getTabWriteBuffer().writeln("");
				}
				getTabWriteBuffer().end(sw, 1);
			}
		}

		return sw.toString();
	}

	private static String getAsVartype(StructField fld)
	{
		if (fld.getPrimitiveType() != null)
		{
			switch (fld.getPrimitiveType().getCategory())
			{
			case PrimitiveType.SIGNED_INTEGER:
				return "int";

			case PrimitiveType.UNSIGNED_INTEGER:
				return "uint";

			case PrimitiveType.BOOLEAN:
				return "Boolean";

			case PrimitiveType.FLOAT:
				return "Number";

			case PrimitiveType.STRING:
			case PrimitiveType.STRING_TINY:
			case PrimitiveType.WIDESTRING:
			case PrimitiveType.WIDESTRING_TINY:
				return "String";

			case PrimitiveType.BUFFER:
			case PrimitiveType.BUFFER_TINY:
				return "ByteArray";
			}
		}

		return fld.getType();
	}

	private void writePacketClass(StreamWriter sw, Packet pk, String className) //throws Exception
	{
		if (!Util.isNullOrEmpty(pk.getComment()) && m_useComment)
		{
			sw.writeln("\t// {0}", pk.getComment());
		}
		sw.writeln("\tpublic class {0}", className);
		sw.writeln("\t{");

		getTabWriteBuffer().begin();
		for (PacketField fld : pk.getFields())
		{
			getTabWriteBuffer().write("public var {0}:{1}", fld.getName(),
				fld.getRepeatInfo().isOnce() ? getAsVartype(fld) : "Array");

			if (!Util.isNullOrEmpty(fld.getComment()) && m_useComment)
				getTabWriteBuffer().writeln(";\t// {0}", fld.getComment());
			else
				getTabWriteBuffer().writeln(";");
		}
		getTabWriteBuffer().end(sw, 2);

		sw.writeln("\t}");
	}

	private void makePacketClass(Packet pk) throws Exception
	{
		String className = m_doc.getConfig().Prefix.PacketStruct + pk.getName();
		String fileName = className + ".as";
		StreamWriter sw = openCodeStream(fileName, m_strPacketPackageName);

		String basepkg = m_doc.getConfig().As3.Client.UsePackageName ? (m_doc.getConfig().As3.Client.PackageName + ".") : "";
		sw.writeln("package " + basepkg + m_strPacketPackageName);
		sw.writeln("{");

		writePacketClass(sw, pk, className);

		sw.writeln("}");

		closeCodeStream(sw);
		m_logger.writeln("Generated client packet definition file: " + fileName);
	}


	private String getFieldNsWhat(StructField fld, boolean isAutoRepeat, NsWhat nswhat) throws Exception
	{
		nswhat.hasRepeat = fld.getRepeatInfo().hasRepeat();
		nswhat.pt = null;

		if (fld.getPrimitiveType() != null)
		{
			nswhat.pt = fld.getPrimitiveType();
		}
		else if (fld.getCustomType() instanceof Struct)
		{
			Struct st = (Struct) fld.getCustomType();
			if (st.getFields().size() == 1 && st.getFields().get(0).getPrimitiveType() != null)
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
					dtype = "Byte";
					break;
				case 2:
					dtype = "Short";
					break;
				case 4:
					dtype = "Int";
					break;
				case 8:
					dtype = "(ERROR)";
					break;
			}
			break;

		case PrimitiveType.UNSIGNED_INTEGER:
			switch (nswhat.pt.getSizeBytes())
			{
				case 1:
					dtype = "UnsignedByte";
					break;
				case 2:
					dtype = "UnsignedShort";
					break;
				case 4:
					dtype = "UnsignedInt";
					break;
				case 8:
					dtype = "(ERROR)";
					break;
			}
			break;

		case PrimitiveType.BOOLEAN:
			switch (nswhat.pt.getSizeBytes())
			{
				case 1:
					dtype = "Boolean";
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
			if (false == isAutoRepeat)
			{
				dtype += 's';

				if (tiny != null)
					dtype += tiny;
			}
		}

		return dtype;
	}

	private void printTabs(StreamWriter sw, int tabs)
	{
		while (tabs-- > 0)
			sw.write("\t");
	}

	private void printStructReader(StreamWriter sw, int depth, int tabs, Struct st, String instance) throws Exception
	{
		for (StructField fld : st.getFields())
		{
			NsWhat nswhat = new NsWhat();
			String dtype = getFieldNsWhat(fld, false, nswhat);

			if (dtype != null)
			{
				printTabs(sw, tabs);

				if (nswhat.hasRepeat)
				{
					sw.writeln("_nsr.Read{0}({3}.{1}, {2});", dtype, fld.getName(), fld.getRepeatInfo().getCount(), instance);
				}
				else
				{
					if (nswhat.pt.getSizeBytes() > 0)
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

	private void writePacketReceiver(StreamWriter sw, Packet pk) throws Exception
	{
		if (!Util.isNullOrEmpty(pk.getComment()) && m_useComment)
			sw.writeln("\t// " + pk.getComment());

		sw.writeln("\tprivate function {0}{1}(_nsr:ByteArray):Boolean",
			PREFIX_PACKETRECEIVER, pk.getName());
		sw.writeln("\t{");

		if (pk.getFields().size() > 0)
		{
			sw.writeln("\t\tvar _pkt:{0}{1} = new {0}{1}();", m_doc.getConfig().Prefix.PacketStruct, pk.getName());
			if (pk.hasAutoRepeat())
			{
				sw.writeln("\t\tvar _rep:int;");
			}

			//sw.writeln("");

			for (PacketField fld : pk.getFields())
			{
				boolean isAutoRepeat = false;

				if (fld.getRepeatInfo().getType() == RepeatInfo.TYPE_AUTO_VAR)
				{
					isAutoRepeat = true;
					assert(fld.getRepeatInfo().getLimit() > 1);
					if (fld.getRepeatInfo().getLimit() <= 0xFF)
					{
						sw.writeln("\t\t_rep = _nsr.readByte();");
					}
					else
					{
						sw.writeln("\t\t_rep = _nsr.readShort();");
					}
				}
				// TODO:Array

				sw.write("\t\t_pkt.{0} = ", fld.getName());
				String dtype;
				switch(fld.getPrimitiveType().getCategory())
				{
				case PrimitiveType.WIDESTRING:
					sw.writeln("readWideString(m_buff, false);");
					break;
				case PrimitiveType.WIDESTRING_TINY:
					sw.writeln("readWideString(m_buff, true);");
					break;
				case PrimitiveType.STRING:
					sw.writeln("readString(m_buff, false);");
					break;
				case PrimitiveType.STRING_TINY:
					sw.writeln("readString(m_buff, true);");
					break;
				default:
					dtype = getBaPrimitiveType(fld.getPrimitiveType(), true);
					sw.writeln("m_buff.read{0}();", dtype);
					break;
				}


/*
				if (fld.Repeat. Equals(Protocol.REPEAT_AUTO))
				{
					isAutoRepeat = true;
					sw.writeln("\t\t_rep = _nsr.readShort();");
				}
				else if (fld.Repeat. Equals(Protocol.REPEAT_AUTOTINY))
				{
					isAutoRepeat = true;
					sw.writeln("\t\t_rep = _nsr.readByte();");
				}
				else
				{
					isAutoRepeat = false;
				}


				NsWhat nswhat = new NsWhat();
				String dtype = GetFieldNsWhat(fld, false, nswhat);

				if (dtype != null)
				{
					if (isAutoRepeat)
					{
						// CHECK ME
						sw.writeln("\t\t_pkt.{0} = new Array(_rep);", fld.Name);
						sw.writeln("\t\t_Read{1}(_pkt.{0}, _rep);", fld.Name, dtype);
					}
					else if (nswhat.hasRepeat)
					{
						if (fld.Repeat.type == RepeatInfo.RepeatTypes.FixedNumber)
						{
							sw.writeln("\t\t_pkt.{0} = new Array({1});", fld.Name, fld.Repeat.count);
							sw.writeln("\t\t_Read{0}(_pkt.{1}, {2});", dtype, fld.Name, fld.Repeat);
						}
						else
						{
							// variable-length repeat
							sw.writeln("\t\t_pkt.{0} = new Array({1});", fld.Name, fld.Repeat);
							sw.writeln("\t\t_Read{0}(_pkt.{1}, _pkt.{2});", dtype, fld.Name, fld.Repeat);
						}
					}
					else
					{
						if (nswhat.pt.Bytes > 0)
							sw.writeln("\t\t_pkt.{0} = _nsr.read{1}();", fld.Name, dtype);
						else
							sw.writeln("\t\t_nsr.read{1}(&_pkt.{0});", fld.Name, dtype);
					}
				}
				else if (fld.getCustomType() instanceof BlindClass)
				{
					if (isAutoRepeat)
					{
						sw.writeln("\t\t_p = _pkt.{0}.Alloc(_rep, _p);", fld.Name);
						sw.writeln("\t\tfor(int _i=0; _i<_rep; _i++)");
						sw.writeln("\t\t\t_pkt.{0}[_i].SerializeFrom(_nsr);", fld.Name);
					}
					else if (nswhat.hasRepeat)
					{
						if (fld.Repeat.type == RepeatInfo.RepeatTypes.FixedNumber)
						{
							sw.writeln("\t\tfor(int _i=0; _i<{0}; _i++)", fld.Repeat.count);
						}
						else
						{
							sw.writeln("\t\t_p = _pkt.{0}.Alloc(_pkt.{1}, _p);", fld.Name, fld.Repeat);
							sw.writeln("\t\tfor(int _i=0; _i<_pkt.{0}; _i++)", fld.Repeat);
						}

						sw.writeln("\t\t\t_pkt.{0}[_i].SerializeFrom(_nsr);", fld.Name);
					}
					else
					{
						sw.writeln("\t\t_pkt.{0}.SerializeFrom(_nsr);", fld.Name);
					}
				}
				else
				{
					Struct st = (Struct) fld.getCustomType();

					if (isAutoRepeat)
					{
						sw.writeln("\t\t_p = _pkt.{0}.Alloc(_rep, _p);", fld.Name);
						sw.writeln("\t\tfor(int _i=0; _i<_rep; _i++)");
						sw.writeln("\t\t{");
						printStructReader(sw, 1, 2, st, "_pkt." + fld.Name + "[_i]");
						sw.writeln("\t}");
					}
					else if (nswhat.hasRepeat)
					{
						if (fld.Repeat.type == RepeatInfo.RepeatTypes.FixedNumber)
						{
							sw.writeln("\t\tfor(int _i=0; _i<{0}; _i++)", fld.Repeat.count);
							sw.writeln("\t\t{");
							printStructReader(sw, 1, 2, st, "_pkt." + fld.Name + "[_i]");
							sw.writeln("\t\t}");
						}
						else
						{
							sw.writeln("\t\t_p = _pkt.{0}.Alloc(_pkt.{1}, _p);", fld.Name, fld.Repeat);
							sw.writeln("\t\tfor(int _i=0; _i<_pkt.{0}; _i++)", fld.Repeat);
							sw.writeln("\t\t{");
							printStructReader(sw, 1, 2, st, "_pkt." + fld.Name + "[_i]");
							sw.writeln("\t\t}");
						}
					}
					else
					{
						printStructReader(sw, 1, 1, st, "_pkt." + fld.Name);
					}
				}
*/
			}

			sw.writeln("");
		}

		if(m_doc.getConfig().As3.Client.ForStressTester)
		{
			sw.writeln("\t\treturn CallPacketHandler({0}{1}, {2});",
				m_doc.getConfig().Prefix.S2CPacketID, pk.getName(),
				(pk.getFields().size() == 0) ? "null" : "_pkt");
		}
		else
		{
			sw.writeln("\t\treturn m_listener.{0}{1}({2});",
				m_doc.getConfig().Prefix.HandlerMethod, pk.getName(),
				(pk.getFields().size() == 0) ? "" : "_pkt");
		}

		sw.writeln("\t}\n");
	}

	private String getBaPrimitiveType(PrimitiveType pt, boolean bUnsignedAware)
	{
		String dtype = "(ERROR)";

		switch (pt.getCategory())
		{
		case PrimitiveType.SIGNED_INTEGER:
			switch (pt.getSizeBytes())
			{
				case 1:
					dtype = "Byte";
					break;
				case 2:
					dtype = "Short";
					break;
				case 4:
					dtype = "Int";
					break;
			}
			break;

		case PrimitiveType.UNSIGNED_INTEGER:
			switch (pt.getSizeBytes())
			{
				case 1:
					dtype = bUnsignedAware ? "UnsignedByte" : "Byte";
					break;
				case 2:
					dtype = bUnsignedAware ? "UnsignedShort" : "Short";
					break;
				case 4:
					dtype = "UnsignedInt";
					break;
			}
			break;

		case PrimitiveType.BOOLEAN:
			switch (pt.getSizeBytes())
			{
				case 1:
					dtype = "Boolean";
					break;
			}
			break;

		case PrimitiveType.FLOAT:
			switch (pt.getSizeBytes())
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

		return dtype;
	}

	private void writePacketSender(StreamWriter sw, Packet pk) throws Exception
	{
		if (!Util.isNullOrEmpty(pk.getComment()) && m_useComment)
			sw.writeln("\t// " + pk.getComment());

		sw.write("\tpublic function {0}{1}", m_doc.getConfig().Prefix.SenderMethod, pk.getName());

		if (pk.getFields().size() == 0)
		{
			sw.writeln("():void");
		}
		else
		{
			sw.writeln("\n\t(");

			int i = 0;
			while (i < pk.getFields().size())
			{
				PacketField fld = pk.getFields().get(i);

				String dtype;
				switch(fld.getPrimitiveType().getCategory())
				{
				case PrimitiveType.SIGNED_INTEGER:
					dtype = "int";
					break;
				case PrimitiveType.UNSIGNED_INTEGER:
					dtype = "uint";
					break;
				case PrimitiveType.BOOLEAN:
					dtype = "Boolean";
					break;
				case PrimitiveType.WIDESTRING:
				case PrimitiveType.WIDESTRING_TINY:
				case PrimitiveType.STRING:
				case PrimitiveType.STRING_TINY:
					dtype = "String";
					break;
				default:
					dtype = "(ERROR)";
				}

				sw.write("\t\t{0}:{1}", fld.getName(), dtype);

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

			sw.writeln("\t):void");
		}

		sw.writeln("\t{");
		sw.writeln("\t\tm_buff.position = 0;");

		for(int i=0; i < pk.getFields().size(); i++)
		{
			PacketField fld = pk.getFields().get(i);

			String dtype;
			switch(fld.getPrimitiveType().getCategory())
			{
			case PrimitiveType.WIDESTRING:
				sw.writeln("\t\twriteWideString(m_buff, {0}, false);", fld.getName());
				break;
			case PrimitiveType.WIDESTRING_TINY:
				sw.writeln("\t\twriteWideString(m_buff, {0}, true);", fld.getName());
				break;
			case PrimitiveType.STRING:
				sw.writeln("\t\twriteString(m_buff, {0}, false);", fld.getName());
				break;
			case PrimitiveType.STRING_TINY:
				sw.writeln("\t\twriteString(m_buff, {0}, true);", fld.getName());
				break;
			default:
				dtype = getBaPrimitiveType(fld.getPrimitiveType(), false);
				sw.writeln("\t\tm_buff.write{0}({1});", dtype, fld.getName());
				break;
			}
		}

		sw.writeln("\n\t\tSendPacket({0}{1}, m_buff, m_buff.position);", m_doc.getConfig().Prefix.C2SPacketID, pk.getName());
		sw.writeln("\t}\n");
	}
}
