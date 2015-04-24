package bpdtool.data;

import bpdtool.Util;
import bpdtool.codegen.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.stream.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class Protocol
{
	public static final int LANGUAGE_CPP = 10;
	public static final int LANGUAGE_CSHARP = 20;
	public static final int LANGUAGE_JAVA = 30;
	public static final int LANGUAGE_JAVASCRIPT = 40;
	public static final int LANGUAGE_ACTIONSCRIPT = 50;

	public static final int ROLE_SERVER = 0x100;
	public static final int ROLE_CLIENT = 0x200;

	public static class LanguageAndRole
	{
		private int m_index;
		private int m_language;
		private int m_role;
		private String m_name;

		public LanguageAndRole(int lang, int role, String name)
		{
			m_index = s_langAndRoles.size();
			m_language = lang;
			m_role = role;
			m_name = name;
		}

		public int getLanguage()
		{
			return m_language;
		}

		public int getRole()
		{
			return m_role;
		}

		public int getIndex()
		{
			return m_index;
		}

		@Override
		public String toString()
		{
			return m_name;
		}
	}

	public static class ExportEntry
	{
		public boolean enabled;
		public LanguageAndRole langAndRole;
		public String nlchar;   // new line character
		public String encoding;
		public String outputDir;
	}

	private static ArrayList<LanguageAndRole> s_langAndRoles;
	private static HashMap<String, PrimitiveType> s_allPrimitiveNames;
	private static HashMap<PrimitiveType, String> s_recommendedPrimitiveNames;

	static
	{
		s_langAndRoles = new ArrayList<>();
		s_langAndRoles.add(new LanguageAndRole(LANGUAGE_CPP, ROLE_SERVER, "C++ Server"));
		s_langAndRoles.add(new LanguageAndRole(LANGUAGE_CPP, ROLE_CLIENT, "C++ Client"));
		//s_langAndRoles.add(new LanguageAndRole(LANGUAGE_CSHARP, ROLE_SERVER, "C# Server"));
		//s_langAndRoles.add(new LanguageAndRole(LANGUAGE_CSHARP, ROLE_CLIENT, "C# Client"));
		//s_langAndRoles.add(new LanguageAndRole(LANGUAGE_JAVA, ROLE_SERVER, "Java Server"));
		//s_langAndRoles.add(new LanguageAndRole(LANGUAGE_JAVA, ROLE_CLIENT, "Java Client"));
		s_langAndRoles.add(new LanguageAndRole(LANGUAGE_JAVASCRIPT, ROLE_SERVER, "JavaScript Server"));
		s_langAndRoles.add(new LanguageAndRole(LANGUAGE_JAVASCRIPT, ROLE_CLIENT, "JavaScript Client"));
		s_langAndRoles.add(new LanguageAndRole(LANGUAGE_ACTIONSCRIPT, ROLE_CLIENT, "ActionScript3 Client"));


		PrimitiveType b8 = new PrimitiveType(PrimitiveType.BOOLEAN, 1, "8-bit Boolean");
		PrimitiveType i8 = new PrimitiveType(PrimitiveType.SIGNED_INTEGER, 1, "Signed 8-bit Integer");
		PrimitiveType i16 = new PrimitiveType(PrimitiveType.SIGNED_INTEGER, 2, "Signed 16-bit Integer");
		PrimitiveType i32 = new PrimitiveType(PrimitiveType.SIGNED_INTEGER, 4, "Signed 32-bit Integer");
		PrimitiveType i64 = new PrimitiveType(PrimitiveType.SIGNED_INTEGER, 8, "Signed 64-bit Integer");
		PrimitiveType ui8 = new PrimitiveType(PrimitiveType.UNSIGNED_INTEGER, 1, "Unsigned 8-bit Integer");
		PrimitiveType ui16 = new PrimitiveType(PrimitiveType.UNSIGNED_INTEGER, 2, "Unsigned 16-bit Integer");
		PrimitiveType ui32 = new PrimitiveType(PrimitiveType.UNSIGNED_INTEGER, 4, "Unsigned 32-bit Integer");
		PrimitiveType ui64 = new PrimitiveType(PrimitiveType.UNSIGNED_INTEGER, 8, "Unsigned 64-bit Integer");
		PrimitiveType f32 = new PrimitiveType(PrimitiveType.FLOAT, 4, "32-bit Float");
		PrimitiveType f64 = new PrimitiveType(PrimitiveType.FLOAT, 8, "64-bit Float");
		PrimitiveType str = new PrimitiveType(PrimitiveType.STRING, 0, "8-bit String (Max 64K)");
		PrimitiveType strT = new PrimitiveType(PrimitiveType.STRING_TINY, 0, "8-bit String (Max 255)");
		PrimitiveType strW = new PrimitiveType(PrimitiveType.WIDESTRING, 0, "16-bit String (Max 64K)");
		PrimitiveType strWT = new PrimitiveType(PrimitiveType.WIDESTRING_TINY, 0, "16-bit String (Max 255)");
		PrimitiveType buff = new PrimitiveType(PrimitiveType.BUFFER, 0, "Byte Array (Max 64K)");
		PrimitiveType buffT = new PrimitiveType(PrimitiveType.BUFFER_TINY, 0, "Byte Array (Max 255)");

		s_recommendedPrimitiveNames = new HashMap<>();
/*
		s_recommendedPrimitiveNames.put(b8, "BOOL8");
		s_recommendedPrimitiveNames.put(i8, "INT8");
		s_recommendedPrimitiveNames.put(i16, "INT16");
		s_recommendedPrimitiveNames.put(i32, "INT32");
		s_recommendedPrimitiveNames.put(i64, "INT64");
		s_recommendedPrimitiveNames.put(ui8, "UINT8");
		s_recommendedPrimitiveNames.put(ui16, "UINT16");
		s_recommendedPrimitiveNames.put(ui32, "UINT32");
		s_recommendedPrimitiveNames.put(ui64, "UINT64");
		s_recommendedPrimitiveNames.put(f32, "FLOAT32");
		s_recommendedPrimitiveNames.put(f64, "FLOAT64");
*/
		s_recommendedPrimitiveNames.put(b8, "bool");
		s_recommendedPrimitiveNames.put(i8, "char");
		s_recommendedPrimitiveNames.put(i16, "short");
		s_recommendedPrimitiveNames.put(i32, "int");
		s_recommendedPrimitiveNames.put(i64, "INT64");
		s_recommendedPrimitiveNames.put(ui8, "BYTE");
		s_recommendedPrimitiveNames.put(ui16, "WORD");
		s_recommendedPrimitiveNames.put(ui32, "DWORD");
		s_recommendedPrimitiveNames.put(ui64, "UINT64");
		s_recommendedPrimitiveNames.put(f32, "float");
		s_recommendedPrimitiveNames.put(f64, "double");

		s_allPrimitiveNames = new HashMap<>();
		s_allPrimitiveNames.put("bool", b8);
		s_allPrimitiveNames.put("BOOL", b8);

		s_allPrimitiveNames.put("char", i8);
		s_allPrimitiveNames.put("CHAR", i8);
		s_allPrimitiveNames.put("INT8", i8);
		s_allPrimitiveNames.put("int8_t", i8);
		s_allPrimitiveNames.put("__int8", i8);

		s_allPrimitiveNames.put("short", i16);
		s_allPrimitiveNames.put("SHORT", i16);
		s_allPrimitiveNames.put("INT16", i16);
		s_allPrimitiveNames.put("int16_t", i16);
		s_allPrimitiveNames.put("__int16", i16);

		s_allPrimitiveNames.put("int", i32);
		s_allPrimitiveNames.put("long", i32);
		s_allPrimitiveNames.put("INT", i32);
		s_allPrimitiveNames.put("LONG", i32);
		s_allPrimitiveNames.put("INT32", i32);
		s_allPrimitiveNames.put("int32_t", i32);
		s_allPrimitiveNames.put("__int32", i32);

		s_allPrimitiveNames.put("__int64", i64);
		s_allPrimitiveNames.put("long long", i64);
		s_allPrimitiveNames.put("INT64", i64);
		s_allPrimitiveNames.put("int64_t", i64);
		s_allPrimitiveNames.put("__int64", i64);

		s_allPrimitiveNames.put("unsigned char", ui8);
		s_allPrimitiveNames.put("byte", ui8);
		s_allPrimitiveNames.put("BYTE", ui8);
		s_allPrimitiveNames.put("UINT8", ui8);
		s_allPrimitiveNames.put("uint8_t", ui8);
		s_allPrimitiveNames.put("unsigned __int8", ui8);

		s_allPrimitiveNames.put("unsigned short", ui16);
		s_allPrimitiveNames.put("word", ui16);
		s_allPrimitiveNames.put("WORD", ui16);
		s_allPrimitiveNames.put("UINT16", ui16);
		s_allPrimitiveNames.put("uint16_t", i16);
		s_allPrimitiveNames.put("unsigned __int16", ui16);

		s_allPrimitiveNames.put("unsigned int", ui32);
		s_allPrimitiveNames.put("unsigned long", ui32);
		s_allPrimitiveNames.put("dword", ui32);
		s_allPrimitiveNames.put("DWORD", ui32);
		s_allPrimitiveNames.put("ULONG", ui32);
		s_allPrimitiveNames.put("UINT", ui32);
		s_allPrimitiveNames.put("UINT32", ui32);
		s_allPrimitiveNames.put("uint32_t", ui32);
		s_allPrimitiveNames.put("unsigned __int32", ui32);

		s_allPrimitiveNames.put("qword", ui64);
		s_allPrimitiveNames.put("QWORD", ui64);
		s_allPrimitiveNames.put("UINT64", ui64);
		s_allPrimitiveNames.put("uint64_t", ui64);
		s_allPrimitiveNames.put("unsigned __int64", ui64);

		s_allPrimitiveNames.put("float", f32);
		s_allPrimitiveNames.put("FLOAT", f32);
		s_allPrimitiveNames.put("FLOAT32", f32);
		s_allPrimitiveNames.put("double", f64);
		s_allPrimitiveNames.put("DOUBLE", f64);
		s_allPrimitiveNames.put("FLOAT64", f64);

		s_allPrimitiveNames.put("String", str);
		s_allPrimitiveNames.put("StringTiny", strT);
		s_allPrimitiveNames.put("WideString", strW);
		s_allPrimitiveNames.put("WideStringTiny", strWT);
		s_allPrimitiveNames.put("Buffer", buff);
		s_allPrimitiveNames.put("BufferTiny", buffT);
	}

	public static ArrayList<LanguageAndRole> getLanguageAndRoles()
	{
		return s_langAndRoles;
	}

	public static LanguageAndRole findLanguageAndRole(String name)
	{
		for (LanguageAndRole lnr : s_langAndRoles)
		{
			if (name.equals(lnr.toString()))
				return lnr;
		}

		throw new RuntimeException("Unknown LanguageAndRole: " + name);
	}

	static LanguageAndRole findLanguageAndRole(int lang, int role)
	{
		for (LanguageAndRole lnr : s_langAndRoles)
		{
			if (lnr.getLanguage() == lang && lnr.getRole() == role)
				return lnr;
		}

		throw new RuntimeException(String.format("Unknown languag=%d and role=%d", lang, role));
	}

	public static String[] getPrimitiveTypeNames()
	{
		return s_allPrimitiveNames.keySet().toArray(new String[0]);
	}

	public static HashMap<PrimitiveType, String> getRecommendedNumericPrimitives()
	{
		return s_recommendedPrimitiveNames;
	}

	public static HashMap<String, PrimitiveType> getAllPrimitives()
	{
		return s_allPrimitiveNames;
	}

	public static PrimitiveType getIfPrimitive(String name)
	{
		return s_allPrimitiveNames.get(name);
	}

	public static PrimitiveType findPrimitive(int category, int sizeBytes)
	{
		for (PrimitiveType pt : s_recommendedPrimitiveNames.keySet())
		{
			if (pt.getCategory() == category && pt.getSizeBytes() == sizeBytes)
				return pt;
		}

		throw new RuntimeException(String.format("Couldn't find primitive category=%d,bytes=%d", category, sizeBytes));
		//return null;
	}


	private static final String ELEM_PROTOCOL = "protocol";
	private static final String ELEM_CONFIG = "config";
	private static final String ELEM_EXPORTS = "exports";
	private static final String ELEM_CONSTANTS = "constants";
	private static final String ELEM_TYPES = "types";
	private static final String ELEM_STAGES = "stages";
	private static final String ELEM_PACKETS = "packets";

	private static final String ELEM_STAGE = "stage";
	private static final String ELEM_PACKET_GROUP = "group";
	private static final String ELEM_PACKET = "packet";
	private static final String ELEM_STRUCT = "struct";
	private static final String ELEM_BLINDCLASS = "bclass";
	private static final String ELEM_DEFINES = "defines";
	private static final String ELEM_ENUM = "enum";
	private static final String ELEM_COMMENT = "comment";
	private static final String ELEM_DESCRIPTION = "description";
	private static final String ELEM_FIELD = "field";
	private static final String ELEM_ENTRY = "entry";

	private static final String ATTR_VERSION = "version";
	private static final String ATTR_GROUP_STARTID = "start_id";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_FLOW = "flow";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_STAGE = "stage";
	private static final String ATTR_INDEX = "index";
	private static final String ATTR_ABBR = "abbr";
	private static final String ATTR_DIRECTCASTING = "directcast";
	private static final String ATTR_GENERATEBUILDER = "genbuilder";
	private static final String ATTR_REPEAT = "repeat";
	private static final String ATTR_ENABLED = "enabled";
	private static final String ATTR_LANGUAGE = "language";
	private static final String ATTR_ROLE = "role";
	private static final String ATTR_NEWLINE = "newline";
	private static final String ATTR_ENCODING = "encoding";
	private static final String ATTR_OUTPUTDIR = "output_dir";

	private static final String FLOW_C2S = "c2s";
	private static final String FLOW_S2C = "s2c";
	private static final String FLOW_INTER = "inter";

	static final String VALUE_YES = "yes";

	private ArrayList<ExportEntry> m_exports = new ArrayList<>();
	private PxConfig m_config = new PxConfig();
	private ArrayList<PacketStage> m_packetStages = new ArrayList<>();
	private ArrayList<PacketGroup> m_packetGroups = new ArrayList<>();
	private ArrayList<UserType> m_userTypes = new ArrayList<>();
	private ArrayList<Constant> m_constants = new ArrayList<>();

	private String m_dirtyHash;
	private String m_filename;
	private String m_basedir;	// directory of m_filename

	private ErrorLogger m_errlog;
	
	// empty new
	public Protocol()
	{
		m_basedir = Util.getCurrentDirectory();

		// default packet stages
		m_packetStages.add(new PacketStage(0, "NoAuth", "STAGE_NOAUTH"));
		m_packetStages.add(new PacketStage(1, "Authed", "STAGE_AUTHED"));

		m_config.setDefaults();

		m_dirtyHash = getDocHash();
	}

	public PxConfig getConfig()
	{
		return m_config;
	}

	public ArrayList<PacketStage> getPacketStages()
	{
		return m_packetStages;
	}

	public void setPacketStages(ArrayList<PacketStage> stages)
	{
		m_packetStages = stages;
	}

	public ArrayList<PacketGroup> getPacketGroups()
	{
		return m_packetGroups;
	}

	public ArrayList<UserType> getUserTypes()
	{
		return m_userTypes;
	}

	public ArrayList<Constant> getConstants()
	{
		return m_constants;
	}

	public ArrayList<ExportEntry> getExportEntries()
	{
		return m_exports;
	}

	public void addExportEntry()
	{
		ExportEntry ee = new ExportEntry();
		ee.enabled = true;
		ee.langAndRole = s_langAndRoles.get(0);
		ee.encoding = "UTF-8";
		ee.nlchar = "LF";
		ee.outputDir = ".";
		m_exports.add(ee);
	}

	public boolean isPacketGroupSamePos(PacketGroup grp, int pos)
	{
		if (!m_packetGroups.contains(grp))
			return false;

		int index = m_packetGroups.indexOf(grp);
		return (pos == index || pos == (index +1));
	}

	public boolean isPacketNameConflict(String name, Packet pkt)
	{
		for (PacketGroup grp : m_packetGroups)
		{
			for (Packet cur : grp.getPackets())
			{
				if (cur != pkt && name.equals(cur.getName()))
					return true;
			}
		}

		return false;
	}

	public boolean isTypeNameConflict(String name, UserType ut)
	{
		for(UserType cur : m_userTypes)
		{
			if(cur != ut && name.equals(cur.getName()))
				return true;
		}

		return false;
	}

	public boolean isConstNameConflict(String name, Constant cnst)
	{
		for(Constant cur : m_constants)
		{
			if(cur != cnst && name.equals(cur.getName()))
				return true;
		}

		return false;
	}

	public boolean isDirty()
	{
		String curHash = getDocHash();
		return !curHash.equals(m_dirtyHash);
	}

	private String getDocHash()
	{
		try
		{
			DocHashMaker dhm = new DocHashMaker();

			m_config.updateDocHash(dhm);

			for (Constant ct : m_constants)
			{
				ct.updateDocHash(dhm);

				for (ConstantField fld : ct.getFields())
				{
					fld.updateDocHash(dhm);
					dhm.update(fld.getValue());
				}
			}

			for (UserType ut : m_userTypes)
			{
				ut.updateDocHash(dhm);

				if (!ut.isImported())
				{
					if (ut instanceof Struct)
					{
						Struct st = (Struct)ut;

						for (StructField fld : st.getFields())
						{
							fld.updateDocHash(dhm);
							dhm.update(fld.getType());

							if (fld.getRepeatInfo().hasRepeat())
								dhm.update(fld.getRepeatInfo().toString());
						}
					}
					else if (ut instanceof BlindClass)
					{
						// no more member
					}
				}
			}

			for (PacketStage ps : m_packetStages)
			{
				dhm.update(String.valueOf(ps.getIndex()));
				dhm.update(ps.getAbbr());
				dhm.update(ps.getName());
			}

			for (PacketGroup grp : m_packetGroups)
			{
				for (Packet pk : grp.getPackets())
				{
					pk.updateDocHash(dhm);

					dhm.update(pk.getFlow());

					if (pk.isAllStage() == false && pk.getStages().size() > 0)
					{
						for (PacketStage stg : pk.getStages())
						{
							dhm.update(stg.getIndex());
						}
					}

					dhm.update(pk.isDirectCasting());
					dhm.update(pk.isGenerateBuilder());

					for (PacketField fld : pk.getFields())
					{
						fld.updateDocHash(dhm);
						dhm.update(fld.getType());
						if (fld.getRepeatInfo().hasRepeat())
							dhm.update(fld.getRepeatInfo().toString());
					}
				}
			}

			return dhm.getDigest();
		}
		catch(Exception ex)
		{
			//return "ERROR";
			throw new RuntimeException(ex);
		}
	}



	// load protocol from xml file
	public Protocol(String filename) throws Exception
	{
		setFilename(filename);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		factory.newDocumentBuilder();
		Document dom = factory.newDocumentBuilder().parse(filename);

		if (!dom.getDocumentElement().getTagName().equals(ELEM_PROTOCOL))
		{
			throw new Exception("Invalid protocol xml file.");
		}

		m_errlog = new ErrorLogger();

		//dom.getDocumentElement().normalize();
		NodeList children = dom.getDocumentElement().getChildNodes();
		for(int n=0; n<children.getLength(); n++)
		{
			Node node = children.item(n);
			if(node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String localName = node.getNodeName();

			if(ELEM_CONFIG.equals(localName))
			{
				m_config.load(node);
			}
			else if(ELEM_TYPES.equals(localName))
			{
				loadUserTypes(node);
			}
			else if(ELEM_EXPORTS.equals(localName))
			{
				loadExportEntries(node);
			}
			else if(ELEM_CONSTANTS.equals(localName))
			{
				loadConstants(node);
			}
			else if(ELEM_STAGES.equals(localName))
			{
				loadStages(node);
			}
			else if(ELEM_PACKETS.equals(localName))
			{
				loadPackets(node);
			}
			else
			{
				m_errlog.writeln("Unknown node: " + localName);
			}
		}

		String errs = m_errlog.isLogged() ? m_errlog.getLoggedString() : null;
		m_errlog = null;

		if (errs != null)
			throw new Exception(errs);

		m_dirtyHash = getDocHash();
	}

	private void loadExportEntries(Node node)
	{
		NodeList children = node.getChildNodes();
		for (int n=0; n<children.getLength(); n++)
		{
			Node cn = children.item(n);
			if(cn.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (cn.getNodeName().equals(ELEM_ENTRY))
			{
				ExportEntry ee = new ExportEntry();
				ee.enabled = XmlDomMaker.getAttributeBoolean(cn, ATTR_ENABLED);
				String lang = XmlDomMaker.getAttributeString(cn, ATTR_LANGUAGE, null);
				String role = XmlDomMaker.getAttributeString(cn, ATTR_ROLE, null);
				ee.langAndRole = findLanguageAndRole(PxConfig.getLanguageCode(lang), PxConfig.getRoleCode(role));
				ee.nlchar = Util.filterNewlineString(XmlDomMaker.getAttributeString(cn, ATTR_NEWLINE, null));
				ee.encoding = XmlDomMaker.getAttributeString(cn, ATTR_ENCODING, null);
				ee.outputDir = XmlDomMaker.getAttributeString(cn, ATTR_OUTPUTDIR, null);
				m_exports.add(ee);
			}
		}
	}

	private void readItemCommons(ItemCommons target, Node node)
	{
		target.setName(XmlDomMaker.getAttributeString(node, ATTR_NAME, null));
		target.setComment(XmlDomMaker.getChildElementText(node, ELEM_COMMENT));
		target.setDescription(XmlDomMaker.getChildElementText(node, ELEM_DESCRIPTION));
	}

	private void loadConstants(Node node)
	{
		NodeList children = node.getChildNodes();
		for(int n=0; n<children.getLength(); n++)
		{
			Node cn = children.item(n);
			if(cn.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String localName = cn.getNodeName();

			Constant ret;

			if (ELEM_DEFINES.equals(localName))
			{
				ret = makeConstant(cn, false);
			}
			else if (ELEM_ENUM.equals(localName))
			{
				ret = makeConstant(cn, true);
			}
			else continue;

			if (ret != null)
			{
				m_constants.add(ret);
			}
		}
	}

	private Constant makeConstant(Node node, boolean isEnum)
	{
		Constant ret = new Constant(isEnum);

		readItemCommons(ret, node);
		if (Util.isNullOrEmpty(ret.getName()))
		{
			ret.setName("(noname)");
		}

		NodeList children = node.getChildNodes();
		for (int n=0; n<children.getLength(); n++)
		{
			Node cn = children.item(n);
			if(cn.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String localName = cn.getNodeName();

			if (ELEM_FIELD.equals(localName))
			{
				ConstantField fld = new ConstantField();

				readItemCommons(fld, cn);

				if (Util.isNullOrEmpty(fld.getName()))
				{
					m_errlog.writeln("A <field> under <{0} name=\"{1}\"> has no 'name' attribute.", localName, ret.getName());
					return null;
				}

				Node attr = cn.getAttributes().getNamedItem(ATTR_VALUE);
				if (attr == null || attr.getNodeValue().isEmpty())
				{
					fld.setValue(null);
				}
				else
				{
					if(Util.isStringNumber(attr.getNodeValue()))
					{
						fld.setValue(attr.getNodeValue());
					}
					else
					{
						m_errlog.writeln("A value of <field name=\"{0}\" value=\"{1}\"> should be a number.", fld.getName(), attr.getNodeValue());
						return null;
					}
				}

				ret.addField(fld);
			}
		}

		return ret;
	}

	private void loadUserTypes(Node node)
	{
		HashSet<String> uniqueNames = new HashSet<>();
		NodeList children = node.getChildNodes();
		for(int n=0; n<children.getLength(); n++)
		{
			Node cn = children.item(n);
			if(cn.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String localName = cn.getNodeName();
			UserType ret;

			if(ELEM_STRUCT.equals(localName))
			{
				ret = makeStruct(cn);
			}
			else if(ELEM_BLINDCLASS.equals(localName))
			{
				ret = makeBlindClass(cn);
			}
			else continue;

			if(ret != null)
			{
				if (uniqueNames.contains(ret.getName()))
				{
					m_errlog.writeln("Duplicated type name: " + ret.getName());
				}
				else
				{
					uniqueNames.add(ret.getName());
					m_userTypes.add(ret);
				}
			}
		}

		// custom type reference check
		for (UserType ut : m_userTypes)
		{
			if (ut instanceof BlindClass)
				continue;

			Struct st = (Struct)ut;
			for (StructField fld : st.getFields())
			{
				if (fld.getPrimitiveType() == null)
				{
					fld.setCustomType(getCustomType(fld.getType()));
					if (fld.getCustomType() == null)
					{
						m_errlog.writeln("Unknown type '{0}' in <field name=\"{1}\"> of <struct name=\"{2}\">",
							fld.getType(), fld.getName(), st.getName());
					}
					else if (fld.getCustomType() instanceof BlindClass)
					{
						m_errlog.writeln("Type '{0}' in <field name=\"{1}\"> of <struct name=\"{2}\"> is Blind Class.",
							fld.getType(), fld.getName(), st.getName());
					}
					else
					{
						fld.getCustomType().incRef();
					}
				}
			}

			// FIXME: cross-reference & backward-reference will not be processed properly.
			st.calcBytes();
		}
	}

	private BlindClass makeBlindClass(Node node)
	{
		BlindClass ret = new BlindClass();

		readItemCommons(ret, node);

		if (Util.isNullOrEmpty(ret.getName()))
		{
			m_errlog.writeln("A <{0}> element has no 'name' attribute.", ELEM_BLINDCLASS);
			return null;
		}

		return ret;
	}

	private Struct makeStruct(Node node)
	{
		Struct ret = new Struct();

		readItemCommons(ret, node);

		if (Util.isNullOrEmpty(ret.getName()))
		{
			m_errlog.writeln("A <{0}> element has no 'name' attribute.", ELEM_STRUCT);
			return null;
		}

		NodeList children = node.getChildNodes();
		for(int n=0; n<children.getLength(); n++)
		{
			Node cn = children.item(n);
			if(cn.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String localName = cn.getNodeName();

			if (ELEM_FIELD.equals(localName))
			{
				StructField fld = new StructField();

				readItemCommons(fld, cn);
				fld.setType(XmlDomMaker.getAttributeString(cn, ATTR_TYPE, null));

				if (Util.isNullOrEmpty(fld.getName()))
				{
					m_errlog.writeln("A <field> in <struct name=\"{0}\"> has no 'name' attribute.", ret.getName());
					return null;
				}

				if (Util.isNullOrEmpty(fld.getType()))
				{
					m_errlog.writeln("A <field name=\"{0}\"> in struct has no 'type' attribute.", fld.getName());
					return null;
				}

				fld.setPrimitiveType(getIfPrimitive(fld.getType()));

				String repeat_spec = XmlDomMaker.getAttributeString(cn, ATTR_REPEAT, null);
				try
				{
					fld.setRepeatInfo(new RepeatInfo(repeat_spec));
				}
				catch(Exception ex)
				{
					m_errlog.writeln("Invalid repeat value '{0}' in <field name=\"{1}\" type=\"{2}\">", repeat_spec, fld.getName(), fld.getType());
					return null;
				}

				ret.addField(fld);
			}
		}

		return ret;
	}

	private UserType getCustomType(String name)
	{
		for (UserType ut : m_userTypes)
		{
			if (ut.getName().equals(name))
				return ut;
		}

		return null;
	}

	private void loadStages(Node node)
	{
		NodeList children = node.getChildNodes();
		for (int n=0; n<children.getLength(); n++)
		{
			Node cn = children.item(n);
			if(cn.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String localName = cn.getNodeName();

			if (ELEM_STAGE.equals(localName))
			{
				String sIndex = XmlDomMaker.getAttributeString(cn, ATTR_INDEX, null);
				String abbr = XmlDomMaker.getAttributeString(cn, ATTR_ABBR, null);
				String name = XmlDomMaker.getAttributeString(cn, ATTR_NAME, null);
				int index;
				int[] outval = new int[1];

				if (sIndex == null || abbr == null || name == null)
				{
					m_errlog.writeln("Invalid stage element.");
				}
				else if(Util.tryParseInt(sIndex, outval))
				{
					index = outval[0];

					if(index != m_packetStages.size())
					{
						m_errlog.writeln("Invalid stage index number: {0} (abbr={1})", sIndex, abbr);
					}
					else
					{
						m_packetStages.add(new PacketStage(index, abbr, name));
					}
				}
				else
				{
					m_errlog.writeln("Stage index ParseInt failed: {0} (abbr={1})", sIndex, abbr);
				}
			}
		}
	}

	private void loadPackets(Node node)
	{
		NodeList children = node.getChildNodes();
		for (int n=0; n<children.getLength(); n++)
		{
			Node cn = children.item(n);
			if(cn.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (cn.getNodeName().equals(ELEM_PACKET_GROUP))
			{
				m_packetGroups.add(makePacketGroup(cn));
			}
		}
	}

	private PacketGroup makePacketGroup(Node node)
	{
		PacketGroup ret = new PacketGroup();

		readItemCommons(ret, node);

		ret.setStartId(XmlDomMaker.getAttributeString(node, ATTR_GROUP_STARTID, null));

		if (Util.isNullOrEmpty(ret.getName()))
		{
			ret.setName("(Unnamed Group)");
		}

		NodeList children = node.getChildNodes();
		for (int n=0; n<children.getLength(); n++)
		{
			Node cn = children.item(n);
			if (cn.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (cn.getNodeName().equals(ELEM_PACKET))
			{
				ret.addPacket(makePacket(cn));
			}
		}

		return ret;
	}

	private Packet makePacket(Node node)
	{
		Packet ret = new Packet();

		readItemCommons(ret, node);

		if (Util.isNullOrEmpty(ret.getName()))
		{
			m_errlog.writeln("A <packet> element has no 'name' attribute.");
			return null;
		}

		String flow = XmlDomMaker.getAttributeString(node, ATTR_FLOW, null);
		if(flow == null)
		{
			m_errlog.writeln("A <packet name=\"{0}\"> has no 'flow' attribute.", ret.getName());
			return null;
		}

		if(FLOW_C2S.equals(flow))
		{
			ret.setFlow(Packet.FLOW_C2S);
		}
		else if(FLOW_S2C.equals(flow))
		{
			ret.setFlow(Packet.FLOW_S2C);
		}
		else if(FLOW_INTER.equals(flow))
		{
			ret.setFlow(Packet.FLOW_INTER);
		}
		else
		{
			m_errlog.writeln("Invalid flow '{0}' in <packet name=\"{1}\">", flow, ret.getName());
			return null;
		}

		ret.setDirectCasting(XmlDomMaker.getAttributeBoolean(node, ATTR_DIRECTCASTING));
		ret.setGenerateBuilder(XmlDomMaker.getAttributeBoolean(node, ATTR_GENERATEBUILDER));

		String stages = XmlDomMaker.getAttributeString(node, ATTR_STAGE, null);
		if (stages == null)
		{
			ret.setAllStage(true);
		}
		else
		{
			ret.setAllStage(false);

			String [] stgs = stages.split(" |,");
			for(String stgnum : stgs)
			{
				if(stgnum.isEmpty())
					continue;

				int index;
				int[] outval = new int[1];

				if(Util.tryParseInt(stgnum, outval))
				{
					index = outval[0];
				}
				else
				{
					// parse failed
					m_errlog.writeln("Invalid stage spec: \"{0}\"", stages);
					return null;
				}

				if(index >= m_packetStages.size())
				{
					m_errlog.writeln("Invalid stage number {0} in <packet name=\"{1}\">", index, ret.getName());
					return null;
				}

				if(ret.getStages().indexOf(m_packetStages.get(index)) >= 0)
				{
					m_errlog.writeln("Stage number duplicated: {0}", stages);
					return null;
				}

				ret.getStages().add(m_packetStages.get(index));
			}
		}

		NodeList children = node.getChildNodes();
		for (int n = 0; n < children.getLength(); n++)
		{
			Node cn = children.item(n);
			if(cn.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (cn.getNodeName().equals(ELEM_FIELD))
			{
				PacketField fld = new PacketField();

				readItemCommons(fld, cn);

				if (Util.isNullOrEmpty(fld.getName()))
				{
					m_errlog.writeln("A <field> in <packet name=\"{0}\"> has no 'name' attribute.", ret.getName());
					return null;
				}

				//fld.Type = XmlDomMaker.getAttributeString(cn, ATTR_TYPE, "(unknown)");
				fld.setType(XmlDomMaker.getAttributeString(cn, ATTR_TYPE, null));
				if (Util.isNullOrEmpty(fld.getType()))
				{
					m_errlog.writeln("A <field name=\"{0}\"> in packet {1} has no 'type' attribute.", fld.getName(), ret.getName());
					return null;
				}

				fld.setCustomType(null);
				fld.setPrimitiveType(getIfPrimitive(fld.getType()));
				if (fld.getPrimitiveType() == null)
				{
					fld.setCustomType(getCustomType(fld.getType()));
					if (fld.getCustomType() == null)
					{
						m_errlog.writeln("Unknown type '{0}' in <field name=\"{1}\">.", fld.getType(), fld.getName());
						return null;
					}

					fld.getCustomType().incRef();
				}

				String repeat_spec = XmlDomMaker.getAttributeString(cn, ATTR_REPEAT, null);
				try
				{
					fld.setRepeatInfo(new RepeatInfo(repeat_spec));

					if (fld.getRepeatInfo().getType() == RepeatInfo.TYPE_BY_REFERENCE)
					{
						// check if the repeat String is found in previous fields
						boolean found = false;
						for (PacketField old : ret.getFields())
						{
							if (old.getName().equals(fld.getRepeatInfo().getReference()))
							{
								found = true;
								break;
							}
						}

						if (!found)
							throw new Exception("Invalid repeat field reference: " + fld.getRepeatInfo().getReference());
					}
				}
				catch (Exception ex)
				{
					m_errlog.writeln("Invalid repeat value '{0}' in <field name=\"{1}\" type=\"{2}\">", repeat_spec, fld.getName(), fld.getType());
					return null;
				}

				ret.addField(fld);
			}
		}

		return ret;
	}

	//////////////////////////////////////////////////////////////////////////

	public void setFilename(String filename)
	{
		m_filename = filename;

		String dir = Util.getDirectoryName(filename);
		if(Util.isNullOrEmpty(dir))
			m_basedir = Util.getCurrentDirectory();
		else
			m_basedir = dir;
	}

	public String getFilename()
	{
		return m_filename;
	}

	public String getBaseDir()
	{
		return m_basedir;
	}

	public void save() throws Exception
	{
		String bakfile = null;

		if (Util.isFileExists(m_filename))
		{
			// make backup
			bakfile = m_filename + ".bak";
			Util.deleteFile(bakfile);
			File oldfile = new File(m_filename);
			oldfile.renameTo(new File(bakfile));
		}

		try
		{
			if(saveXML())
			{
				// succeeded
				m_dirtyHash = getDocHash();
				if(bakfile != null)
					Util.deleteFile(bakfile);
			}
		}
		catch(Exception ex)
		{
			if (bakfile != null)
			{
				// restore baked file
				Util.deleteFile(m_filename);
				Util.renameFile(bakfile, m_filename);
			}

			throw ex;
		}
	}

	private boolean saveXML() throws Exception
	{
		XmlDomMaker xml = new XmlDomMaker();

		xml.writeProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"protocol_report.xsl\"");
		xml.writeStartDocument();
		xml.writeStartElement(ELEM_PROTOCOL);
		xml.writeAttribute(ATTR_VERSION, "1.0");

		// config
		xml.writeStartElement(ELEM_CONFIG);
		m_config.save(xml);
		xml.writeEndElement();

		// export entries
		xml.writeStartElement(ELEM_EXPORTS);
		for (ExportEntry ee : m_exports)
		{
			xml.writeStartElement(ELEM_ENTRY);
				xml.writeAttribute(ATTR_ENABLED, ee.enabled ? VALUE_YES : null);
				xml.writeAttribute(ATTR_LANGUAGE, PxConfig.getLanguageName(ee.langAndRole.getLanguage()));
				xml.writeAttribute(ATTR_ROLE, PxConfig.getRoleName(ee.langAndRole.getRole()));
				xml.writeAttribute(ATTR_NEWLINE, ee.nlchar);
				xml.writeAttribute(ATTR_ENCODING, ee.encoding);
				xml.writeAttribute(ATTR_OUTPUTDIR, ee.outputDir);
			xml.writeEndElement();
		}
		xml.writeEndElement();
		
		// constants
		xml.writeStartElement(ELEM_CONSTANTS);
		for (Constant ct : m_constants)
		{
			startWriteItem(xml, ct.isEnum() ? ELEM_ENUM : ELEM_DEFINES, ct);
			for (ConstantField fld : ct.getFields())
			{
				startWriteItem(xml, ELEM_FIELD, fld);
				xml.writeAttribute(ATTR_VALUE, fld.getValue());
				xml.writeEndElement();
			}
			xml.writeEndElement();
		}
		xml.writeEndElement();

		// user-types
		xml.writeStartElement(ELEM_TYPES);
		for (UserType ut : m_userTypes)
		{
			if (!ut.isImported())
			{
				if (ut instanceof Struct)
					writeStruct(xml, (Struct) ut);
				else if (ut instanceof BlindClass)
					writeBlindClass(xml, (BlindClass) ut);
			}
		}
		xml.writeEndElement();

		// packet dispatch stages
		xml.writeStartElement(ELEM_STAGES);
		for (PacketStage ps : m_packetStages)
		{
			xml.writeStartElement(ELEM_STAGE);
			xml.writeAttribute(ATTR_INDEX, String.valueOf(ps.getIndex()));
			xml.writeAttribute(ATTR_ABBR, ps.getAbbr());
			xml.writeAttribute(ATTR_NAME, ps.getName());
			xml.writeEndElement();
		}
		xml.writeEndElement();

		// packets
		xml.writeStartElement(ELEM_PACKETS);
		for (PacketGroup grp : m_packetGroups)
		{
			startWriteItem(xml, ELEM_PACKET_GROUP, grp);
			xml.writeAttribute(ATTR_GROUP_STARTID, grp.getStartId());

			for (Packet pkt : grp.getPackets())
			{
				writePacket(xml, pkt);
			}

			xml.writeEndElement();
		}
		xml.writeEndElement();


		xml.writeEndElement();
		xml.writeEndDocument();

		xml.saveToFile(m_filename);
		return true;
	}

	private void startWriteItem(XmlDomMaker xml, String elem, ItemCommons item) throws XMLStreamException
	{
		xml.writeStartElement(elem);
		xml.writeAttribute(ATTR_NAME, item.getName());
		xml.writeSimpleElement(ELEM_COMMENT, item.getComment());
		xml.writeSimpleElement(ELEM_DESCRIPTION, item.getDescription());
	}

	private void writeStruct(XmlDomMaker xml, Struct st) throws Exception
	{
		startWriteItem(xml, ELEM_STRUCT, st);

		for (StructField fld : st.getFields())
		{
			startWriteItem(xml, ELEM_FIELD, fld);
			xml.writeAttribute(ATTR_TYPE, fld.getType());

			if (fld.getRepeatInfo().hasRepeat())
			{
				xml.writeAttribute(ATTR_REPEAT, fld.getRepeatInfo().toString());
			}

			xml.writeEndElement();
		}

		xml.writeEndElement();
	}

	private void writeBlindClass(XmlDomMaker xml, BlindClass bc) throws Exception
	{
		startWriteItem(xml, ELEM_BLINDCLASS, bc);
		xml.writeEndElement();
	}

	private void writePacket(XmlDomMaker xml, Packet pk) throws XMLStreamException
	{
		startWriteItem(xml, ELEM_PACKET, pk);

		String flow;
		switch (pk.getFlow())
		{
		case Packet.FLOW_C2S:
			flow = FLOW_C2S;
			break;
		case Packet.FLOW_S2C:
			flow = FLOW_S2C;
			break;
		case Packet.FLOW_INTER:
			flow = FLOW_INTER;
			break;
		default:
			flow = null;
			break;
		}

		xml.writeAttribute(ATTR_FLOW, flow);

		if (pk.isAllStage() == false && pk.getStages().size() > 0)
		{
			ArrayList<String> nums = new ArrayList<String>();

			for(PacketStage stg : pk.getStages())
			{
				nums.add(String.valueOf(stg.getIndex()));
			}

			xml.writeAttribute(ATTR_STAGE, Util.stringJoin(",", nums.toArray()));
		}

		if (pk.isDirectCasting())
			xml.writeAttribute(ATTR_DIRECTCASTING, VALUE_YES);
		if (pk.isGenerateBuilder())
			xml.writeAttribute(ATTR_GENERATEBUILDER, VALUE_YES);

		for (PacketField fld : pk.getFields())
		{
			startWriteItem(xml, ELEM_FIELD, fld);
			xml.writeAttribute(ATTR_TYPE, fld.getType());

			if (fld.getRepeatInfo().hasRepeat())
				xml.writeAttribute(ATTR_REPEAT, fld.getRepeatInfo().toString());

			xml.writeEndElement();
		}

		xml.writeEndElement();
	}


	public void onTypeNameChanged(UserType ut)
	{
		for (PacketGroup grp : m_packetGroups)
		{
			for (Packet pk : grp.getPackets())
			{
				for (PacketField fld : pk.getFields())
				{
					if (fld.getCustomType() == ut)
					{
						fld.setType(ut.getName());
					}
				}
			}
		}
	}

	private static Pattern re_AbsPath = Pattern.compile("^[a-zA-Z]+:");
	private static String DirSepChars = "/|\\\\";

	public String getAbsPath(String relpath)
	{
		if(Util.isNullOrEmpty(relpath))
			return m_basedir;

		if(re_AbsPath.matcher(relpath).matches())
		{
			return relpath;
		}

		String cur = m_basedir;
		String[] dirs = relpath.split(DirSepChars);
		for (String dir : dirs)
		{
			if(dir.equals("."))
			{
				// ignore
			}
			else if(dir.equals(".."))
			{
				cur = Util.getDirectoryName(cur);
			}
			else
			{
				cur = Util.pathCombine(cur, dir);
			}
		}

		return cur;
	}

	public String getRelPath(String abspath)
	{
		if(Util.isNullOrEmpty(abspath))
			return ".";

		String[] dirsBase = m_basedir.split(DirSepChars);
		String[] dirsAbs = abspath.split(DirSepChars);

		if(!dirsBase[0].toLowerCase().equals(dirsAbs[0].toLowerCase()))
		{
			// different directory
			return abspath;
		}

		String ret;
		for (int i = 1; i < dirsBase.length; i++)
		{
			if(i < dirsAbs.length
				&& dirsBase[i].toLowerCase().equals(dirsAbs[i].toLowerCase()))
				continue;

			ret = "..";
			int cnt = dirsBase.length -i;
			while(cnt-- > 1) ret = Util.pathCombine(ret, "..");
			for(int k=i; k<dirsAbs.length; k++)
				ret = Util.pathCombine(ret, dirsAbs[k]);
			return ret;
		}

		ret = ".";
		for(int k=dirsBase.length; k<dirsAbs.length; k++)
			ret = Util.pathCombine(ret, dirsAbs[k]);

		return ret;
	}

}
