package bpdtool.data;


import bpdtool.Util;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Set;

class PxNode
{
	private String m_tagName;
	private String m_text;
	private HashMap<String,String> m_attrs = new HashMap<>();
	private HashMap<String,PxNode> m_children = new HashMap<>();

	private static PxNode s_nullNode = new PxNode();

	private PxNode() {}

	public PxNode(Node xmlNode)
	{
		if(xmlNode.getNodeType() != Node.ELEMENT_NODE)
			throw new RuntimeException("PxNode: not an element node.");

		m_tagName = xmlNode.getNodeName();
		m_text = xmlNode.getTextContent().trim();

		NamedNodeMap attrs = xmlNode.getAttributes();
		for (int a=0; a<attrs.getLength(); a++)
		{
			Attr atr = (Attr) attrs.item(a);
			m_attrs.put(atr.getName(), atr.getValue());
		}

		NodeList cnodes = xmlNode.getChildNodes();
		for(int c=0; c<cnodes.getLength(); c++)
		{
			Node cld = cnodes.item(c);
			if(cld.getNodeType() == Node.ELEMENT_NODE)
			{
				m_children.put(cld.getNodeName(), new PxNode(cld));
			}
		}
	}

	public String getName()
	{
		return m_tagName;
	}

	public String getText()
	{
		return m_text;
	}

	public Set<String> getAttributeKeys()
	{
		return m_attrs.keySet();
	}

	public String getAttribute(String key)
	{
		return m_attrs.get(key);
	}

	public PxNode getChild(String elementName)
	{
		PxNode child = m_children.get(elementName);
		if (child == null)
			return s_nullNode;

		return child;
	}
}

public class PxConfig
{
	private static final String ELEM_TITLE = "title";
	private static final String ELEM_AUTHOR = "author";
	private static final String ELEM_MEMO = "memo";

	private static final String ELEM_PREFIX = "prefix";
	private static final String ATTR_C2SID = "c2s_id";
	private static final String ATTR_S2CID = "s2c_id";
	private static final String ATTR_PACKETSTRUCT = "packet_struct";
	private static final String ATTR_SENDERMETHOD = "sender_method";
	private static final String ATTR_BUILDERMETHOD = "builder_method";
	private static final String ATTR_HANDLERMETHOD = "handler_method";

	private static final String ELEM_DELEGATETYPES = "delegate_types";
	private static final String ATTR_ENABLEALTERNATIVETYPES = "enable_alttype";
	private static final String ATTR_BOOL8 = "b8";
	private static final String ATTR_INT8 = "i8";
	private static final String ATTR_INT16 = "i16";
	private static final String ATTR_INT32 = "i32";
	private static final String ATTR_INT64 = "i64";
	private static final String ATTR_UINT8 = "u8";
	private static final String ATTR_UINT16 = "u16";
	private static final String ATTR_UINT32 = "u32";
	private static final String ATTR_UINT64 = "u64";
	private static final String ATTR_FLOAT32 = "f32";
	private static final String ATTR_FLOAT64 = "f64";

	private static final String ELEM_CLIENT = "client";
	private static final String ELEM_SERVER = "server";
	private static final String ELEM_CPP = "cpp";
	private static final String ELEM_CSHARP = "csharp";
	private static final String ELEM_AS3 = "actionscript";
	private static final String ELEM_JAVASCRIPT = "javascript";
	private static final String ELEM_JAVA = "java";

	private static final String ATTR_CLASSNAME = "class_name";
	private static final String ELEM_ADDHEADER = "header";
	private static final String ELEM_NAMESPACE = "namespace";
	private static final String ELEM_FILEEXT = "file_ext";
	private static final String ATTR_SKIPCOMMON = "skip_common_code";
	private static final String ATTR_SWAPENDIAN = "swap_endian";
	private static final String ATTR_GENERATESAMPLE = "generate_sample_code";

	private static final String ATTR_COMMONFILENAME = "common_file_name";
	private static final String ATTR_CAPITALIZE = "capitalize";
	private static final String ATTR_NODIRECTCAST = "no_direct_cast";
	private static final String ATTR_USESTDVECTOR = "use_std_vector";
	private static final String ATTR_MERGEALL = "merge_all";
	private static final String ELEM_PACKAGENAME = "package_name";
	private static final String ATTR_STRESSTEST = "for_stress_test";

	private static final String ATTR_USE16BITPKTID = "use_16bit_id";
	private static final String ATTR_USEHEXID = "use_hex_id";
	private static final String ATTR_NOEXPORTCOMMENT = "no_comment_export";
	private static final String ATTR_NOUSEPACKETSTAGE = "no_packet_stage";
	private static final String ATTR_ADDUNICODEBOM = "add_unicode_bom";

	private static final String ATTR_USE = "use";

	public static class _Prefix
	{
		public String C2SPacketID;
		public String S2CPacketID;
		public String PacketStruct;
		public String SenderMethod;
		public String BuilderMethod;
		public String HandlerMethod;
	}

	public static class _CppCS
	{
		public String ClassName;
		public String Namespace;
		public boolean UseNamespace;
		public String FileExt;
		public boolean OverrideFileExt;
		public String CustomHeader;
		public boolean SkipGenerateCommon;
		public boolean SwapEndian;
		public boolean GenerateSampleImpl;
	}

	public static class _Cpp
	{
		public _CppCS Server;
		public _CppCS Client;

		public String CommonFileName;
		public boolean CapitalizeMethodName;
		public boolean DisableDirectCasting;
		public boolean UseStdVector;
	}

	public static class _As3Client
	{
		public String ClassName;
		public String PackageName;
		public boolean UsePackageName;
		public boolean MergeAll;
		public boolean ForStressTester;
	}

	public static class _As3
	{
		public _As3Client Client;
	}

	public static class _JsCS
	{
		public String ClassName;
	}

	public static class _Js
	{
		public _JsCS Server;
		public _JsCS Client;
	}

	public _Prefix Prefix;
	public _Cpp Cpp;
	public _As3 As3;
	public _Js Js;

	public boolean Use16BitPacketID;
	public boolean UseHexID;
	public boolean NoExportComment;
	public boolean NoUsePacketDispatchStage;
	public boolean AddUnicodeBOM;
	public boolean EnableAlternativeCppTypes;

	public String Title;
	public String Author;
	public String Memo;

	public HashMap<PrimitiveType, String> DelegatePrimitiveNames;

	public PxConfig()
	{
		DelegatePrimitiveNames = new HashMap<>();

		Prefix = new _Prefix();

		Cpp = new _Cpp();
		Cpp.Server = new _CppCS();
		Cpp.Client = new _CppCS();

		As3 = new _As3();
		As3.Client = new _As3Client();

		Js = new _Js();
		Js.Server = new _JsCS();
		Js.Client = new _JsCS();
	}

	public void setDefaults()
	{
		Prefix.C2SPacketID = "IDC2S_";
		Prefix.S2CPacketID = "IDS2C_";
		Prefix.PacketStruct = "Pkt";
		Prefix.SenderMethod = "send_";
		Prefix.BuilderMethod = "build_";
		Prefix.HandlerMethod = "on_";

		Cpp.Server.ClassName = "TheUser";
		Cpp.Client.ClassName = "TheServer";
		Cpp.CommonFileName = "TheProtocol";

		Cpp.Server.CustomHeader = "#include \"stdafx.h\"\n#include \"netstream.h\"\n#include \"TheUser.h\"";
		Cpp.Client.CustomHeader = "#include \"stdafx.h\"\n#include \"netstream.h\"\n#include \"TheServer.h\"";

		setDefaultDelegatePrimitives();
	}

	public void setDefaultDelegatePrimitives()
	{
		DelegatePrimitiveNames.clear();
		DelegatePrimitiveNames.putAll(Protocol.getRecommendedNumericPrimitives());
	}

	public void updateDocHash(DocHashMaker dhm)
	{
		dhm.update(Title);
		dhm.update(Author);
		dhm.update(Memo);

		dhm.update(Use16BitPacketID);
		dhm.update(UseHexID);
		dhm.update(NoExportComment);
		dhm.update(NoUsePacketDispatchStage);
		dhm.update(AddUnicodeBOM);

		dhm.update(EnableAlternativeCppTypes);
		for (String deleType : DelegatePrimitiveNames.values())
		{
			dhm.update(deleType);
		}

		dhm.update(Prefix.C2SPacketID);
		dhm.update(Prefix.S2CPacketID);
		dhm.update(Prefix.PacketStruct);
		dhm.update(Prefix.SenderMethod);
		dhm.update(Prefix.BuilderMethod);
		dhm.update(Prefix.HandlerMethod);

		updateDocHash(dhm, Cpp.Server);
		updateDocHash(dhm, Cpp.Client);
		dhm.update(Cpp.CommonFileName);
		dhm.update(Cpp.DisableDirectCasting);
		dhm.update(Cpp.UseStdVector);
		dhm.update(Cpp.CapitalizeMethodName);

		dhm.update(As3.Client.ClassName);
		dhm.update(As3.Client.PackageName);
		dhm.update(As3.Client.UsePackageName);
		dhm.update(As3.Client.MergeAll);
		dhm.update(As3.Client.ForStressTester);

		dhm.update(Js.Server.ClassName);
		dhm.update(Js.Client.ClassName);
	}

	private void updateDocHash(DocHashMaker dhm, _CppCS cs)
	{
		dhm.update(cs.ClassName);
		dhm.update(cs.CustomHeader);
		dhm.update(cs.Namespace);
		dhm.update(cs.UseNamespace);
		dhm.update(cs.FileExt);
		dhm.update(cs.OverrideFileExt);
		dhm.update(cs.SkipGenerateCommon);
		dhm.update(cs.SwapEndian);
		dhm.update(cs.GenerateSampleImpl);
	}


	public void save(XmlDomMaker xml)
	{
		xml.writeSimpleElement(ELEM_TITLE, Title);
		xml.writeSimpleElement(ELEM_AUTHOR, Author);
		xml.writeSimpleElement(ELEM_MEMO, Memo);

		xml.writeStartElement(ELEM_PREFIX);
		{
			xml.writeAttribute(ATTR_C2SID, Prefix.C2SPacketID);
			xml.writeAttribute(ATTR_S2CID, Prefix.S2CPacketID);
			xml.writeAttribute(ATTR_PACKETSTRUCT, Prefix.PacketStruct);
			xml.writeAttribute(ATTR_SENDERMETHOD, Prefix.SenderMethod);
			xml.writeAttribute(ATTR_BUILDERMETHOD, Prefix.BuilderMethod);
			xml.writeAttribute(ATTR_HANDLERMETHOD, Prefix.HandlerMethod);
		}
		xml.writeEndElement();

		xml.writeStartElement(ELEM_DELEGATETYPES);
		{
			xml.writeAttribute(ATTR_ENABLEALTERNATIVETYPES, EnableAlternativeCppTypes ? Protocol.VALUE_YES : null);

			for (PrimitiveType pt : DelegatePrimitiveNames.keySet())
			{
				String typename = DelegatePrimitiveNames.get(pt);
				if (Util.isNullOrEmpty(typename))
					continue;

				String attrKey = null;
				switch(pt.getCategory())
				{
				case PrimitiveType.BOOLEAN:
					attrKey = ATTR_BOOL8;
					break;
				case PrimitiveType.SIGNED_INTEGER:
					switch(pt.getSizeBytes())
					{
					case 1:
						attrKey = ATTR_INT8;
						break;
					case 2:
						attrKey = ATTR_INT16;
						break;
					case 4:
						attrKey = ATTR_INT32;
						break;
					case 8:
						attrKey = ATTR_INT64;
						break;
					}
					break;
				case PrimitiveType.UNSIGNED_INTEGER:
					switch(pt.getSizeBytes())
					{
					case 1:
						attrKey = ATTR_UINT8;
						break;
					case 2:
						attrKey = ATTR_UINT16;
						break;
					case 4:
						attrKey = ATTR_UINT32;
						break;
					case 8:
						attrKey = ATTR_UINT64;
						break;
					}
					break;
				case PrimitiveType.FLOAT:
					switch(pt.getSizeBytes())
					{
					case 4:
						attrKey = ATTR_FLOAT32;
						break;
					case 8:
						attrKey = ATTR_FLOAT64;
						break;
					}
					break;
				}

				if (attrKey == null)
				{
					throw new RuntimeException("Invalid delegate primitve type: " + pt.getDescription());
				}

				xml.writeAttribute(attrKey, typename);
			}
		}
		xml.writeEndElement();

		xml.writeStartElement(ELEM_CPP);
		{
			xml.writeAttribute(ATTR_COMMONFILENAME, Cpp.CommonFileName);
			xml.writeAttribute(ATTR_CAPITALIZE, Cpp.CapitalizeMethodName ? Protocol.VALUE_YES : null);
			xml.writeAttribute(ATTR_NODIRECTCAST, Cpp.DisableDirectCasting ? Protocol.VALUE_YES : null);
			xml.writeAttribute(ATTR_USESTDVECTOR, Cpp.UseStdVector ? Protocol.VALUE_YES : null);

			saveCppCS(xml, ELEM_SERVER, Cpp.Server);
			saveCppCS(xml, ELEM_CLIENT, Cpp.Client);
		}
		xml.writeEndElement();

		xml.writeStartElement(ELEM_AS3);
		{
			// client only
			xml.writeStartElement(ELEM_CLIENT);
			{
				xml.writeAttribute(ATTR_CLASSNAME, As3.Client.ClassName);

				xml.writeStartElement(ELEM_PACKAGENAME);
				xml.writeAttribute(ATTR_USE, As3.Client.UsePackageName ? Protocol.VALUE_YES : null);
				xml.writeCharacters(As3.Client.PackageName);
				xml.writeEndElement();

				xml.writeAttribute(ATTR_MERGEALL, As3.Client.MergeAll ? Protocol.VALUE_YES : null);
				xml.writeAttribute(ATTR_STRESSTEST, As3.Client.ForStressTester ? Protocol.VALUE_YES : null);
			}
			xml.writeEndElement();
		}
		xml.writeEndElement();

		xml.writeStartElement(ELEM_JAVASCRIPT);
		{
			xml.writeStartElement(ELEM_SERVER);
			{
				xml.writeAttribute(ATTR_CLASSNAME, Js.Server.ClassName);
			}
			xml.writeEndElement();

			xml.writeStartElement(ELEM_CLIENT);
			{
				xml.writeAttribute(ATTR_CLASSNAME, Js.Client.ClassName);
			}
			xml.writeEndElement();
		}
		xml.writeEndElement();

		xml.writeAttribute(ATTR_USE16BITPKTID, Use16BitPacketID ? Protocol.VALUE_YES : null);
		xml.writeAttribute(ATTR_USEHEXID, UseHexID ? Protocol.VALUE_YES : null);
		xml.writeAttribute(ATTR_NOEXPORTCOMMENT, NoExportComment ? Protocol.VALUE_YES : null);
		xml.writeAttribute(ATTR_NOUSEPACKETSTAGE, NoUsePacketDispatchStage ? Protocol.VALUE_YES : null);
		xml.writeAttribute(ATTR_ADDUNICODEBOM, AddUnicodeBOM ? Protocol.VALUE_YES : null);
	}

	private void saveCppCS(XmlDomMaker xml, String elementName, _CppCS cs)
	{
		xml.writeStartElement(elementName);
		{
			xml.writeAttribute(ATTR_CLASSNAME, cs.ClassName);
			xml.writeAttribute(ATTR_SKIPCOMMON, cs.SkipGenerateCommon ? Protocol.VALUE_YES : null);
			xml.writeAttribute(ATTR_SWAPENDIAN, cs.SwapEndian ? Protocol.VALUE_YES : null);
			xml.writeAttribute(ATTR_GENERATESAMPLE, cs.GenerateSampleImpl ? Protocol.VALUE_YES : null);

			xml.writeSimpleElement(ELEM_ADDHEADER, cs.CustomHeader);

			xml.writeStartElement(ELEM_NAMESPACE);
			xml.writeCharacters(cs.Namespace);
			xml.writeAttribute(ATTR_USE, cs.UseNamespace ? Protocol.VALUE_YES : null);
			xml.writeEndElement();

			xml.writeStartElement(ELEM_FILEEXT);
			xml.writeCharacters(cs.FileExt);
			xml.writeAttribute(ATTR_USE, cs.OverrideFileExt ? Protocol.VALUE_YES : null);
			xml.writeEndElement();
		}
		xml.writeEndElement();
	}


	public void load(Node node)
	{
		// convert xml-node to pxnode
		PxNode root = new PxNode(node);

		Title = root.getChild(ELEM_TITLE).getText();
		Author = root.getChild(ELEM_AUTHOR).getText();
		Memo = root.getChild(ELEM_MEMO).getText();

		Use16BitPacketID = Util.isStringTrue(root.getAttribute(ATTR_USE16BITPKTID));
		UseHexID = Util.isStringTrue(root.getAttribute(ATTR_USEHEXID));
		NoExportComment = Util.isStringTrue(root.getAttribute(ATTR_NOEXPORTCOMMENT));
		NoUsePacketDispatchStage = Util.isStringTrue(root.getAttribute(ATTR_NOUSEPACKETSTAGE));
		AddUnicodeBOM = Util.isStringTrue(root.getAttribute(ATTR_ADDUNICODEBOM));

		loadPrefix(root.getChild(ELEM_PREFIX));
		loadDelegateTypes(root.getChild(ELEM_DELEGATETYPES));
		loadCpp(root.getChild(ELEM_CPP));
		loadAs3(root.getChild(ELEM_AS3));
		loadJava(root.getChild(ELEM_JAVA));
		loadJavascript(root.getChild(ELEM_JAVASCRIPT));
	}

	private void loadPrefix(PxNode node)
	{
		Prefix.C2SPacketID = node.getAttribute(ATTR_C2SID);
		Prefix.S2CPacketID = node.getAttribute(ATTR_S2CID);
		Prefix.PacketStruct = node.getAttribute(ATTR_PACKETSTRUCT);
		Prefix.SenderMethod = node.getAttribute(ATTR_SENDERMETHOD);
		Prefix.BuilderMethod = node.getAttribute(ATTR_BUILDERMETHOD);
		Prefix.HandlerMethod = node.getAttribute(ATTR_HANDLERMETHOD);
	}

	private void loadDelegateTypes(PxNode node)
	{
		DelegatePrimitiveNames.clear();
		for (String attrKey : node.getAttributeKeys())
		{
			String val = node.getAttribute(attrKey);
			int category = 0;
			int sizeBytes = 0;
			switch (attrKey)
			{
			case ATTR_ENABLEALTERNATIVETYPES:
				EnableAlternativeCppTypes = Util.isStringTrue(val);
				break;

			case ATTR_BOOL8:
				category = PrimitiveType.BOOLEAN;
				sizeBytes = 1;
				break;
			case ATTR_INT8:
				category = PrimitiveType.SIGNED_INTEGER;
				sizeBytes = 1;
				break;
			case ATTR_INT16:
				category = PrimitiveType.SIGNED_INTEGER;
				sizeBytes = 2;
				break;
			case ATTR_INT32:
				category = PrimitiveType.SIGNED_INTEGER;
				sizeBytes = 4;
				break;
			case ATTR_INT64:
				category = PrimitiveType.SIGNED_INTEGER;
				sizeBytes = 8;
				break;

			case ATTR_UINT8:
				category = PrimitiveType.UNSIGNED_INTEGER;
				sizeBytes = 1;
				break;
			case ATTR_UINT16:
				category = PrimitiveType.UNSIGNED_INTEGER;
				sizeBytes = 2;
				break;
			case ATTR_UINT32:
				category = PrimitiveType.UNSIGNED_INTEGER;
				sizeBytes = 4;
				break;
			case ATTR_UINT64:
				category = PrimitiveType.UNSIGNED_INTEGER;
				sizeBytes = 8;
				break;

			case ATTR_FLOAT32:
				category = PrimitiveType.FLOAT;
				sizeBytes = 4;
				break;
			case ATTR_FLOAT64:
				category = PrimitiveType.FLOAT;
				sizeBytes = 8;
				break;
			}

			if (sizeBytes != 0)
			{
				PrimitiveType pt = Protocol.findPrimitive(category, sizeBytes);
				DelegatePrimitiveNames.put(pt, val);
			}
		}
	}

	private void loadCppCS(PxNode node, _CppCS target)
	{
		if (node.getName() == null)
			return;

		target.ClassName = node.getAttribute(ATTR_CLASSNAME);
		target.CustomHeader = node.getChild(ELEM_ADDHEADER).getText();

		PxNode elemNS = node.getChild(ELEM_NAMESPACE);
		target.Namespace = elemNS.getText();
		target.UseNamespace = Util.isStringTrue(elemNS.getAttribute(ATTR_USE));

		PxNode elemExt= node.getChild(ELEM_FILEEXT);
		target.FileExt = elemExt.getText();
		target.OverrideFileExt = Util.isStringTrue(elemExt.getAttribute(ATTR_USE));

		target.SkipGenerateCommon = Util.isStringTrue(node.getAttribute(ATTR_SKIPCOMMON));
		target.SwapEndian = Util.isStringTrue(node.getAttribute(ATTR_SWAPENDIAN));
		target.GenerateSampleImpl = Util.isStringTrue(node.getAttribute(ATTR_GENERATESAMPLE));
	}

	private void loadCpp(PxNode node)
	{
		loadCppCS(node.getChild(ELEM_SERVER), Cpp.Server);
		loadCppCS(node.getChild(ELEM_CLIENT), Cpp.Client);

		Cpp.CommonFileName = node.getAttribute(ATTR_COMMONFILENAME);
		Cpp.CapitalizeMethodName = Util.isStringTrue(node.getAttribute(ATTR_CAPITALIZE));
		Cpp.DisableDirectCasting = Util.isStringTrue(node.getAttribute(ATTR_NODIRECTCAST));
		Cpp.UseStdVector = Util.isStringTrue(node.getAttribute(ATTR_USESTDVECTOR));
	}

	private void loadAs3(PxNode node)
	{
		PxNode client = node.getChild(ELEM_CLIENT);
		if (client.getName() != null)
		{
			As3.Client.ClassName = client.getAttribute(ATTR_CLASSNAME);
			As3.Client.ForStressTester = Util.isStringTrue(client.getAttribute(ATTR_STRESSTEST));
			As3.Client.MergeAll = Util.isStringTrue(client.getAttribute(ATTR_MERGEALL));

			PxNode elemPkg = client.getChild(ELEM_PACKAGENAME);
			As3.Client.PackageName = elemPkg.getText();
			As3.Client.UsePackageName = Util.isStringTrue(elemPkg.getAttribute(ATTR_USE));
		}
	}

	private void loadJsCS(PxNode node, _JsCS target)
	{
		target.ClassName = node.getAttribute(ATTR_CLASSNAME);
	}

	private void loadJavascript(PxNode node)
	{
		loadJsCS(node.getChild(ELEM_CLIENT), Js.Client);
		loadJsCS(node.getChild(ELEM_SERVER), Js.Server);
	}

	private void loadJava(PxNode node)
	{
		// TODO
	}

	static String getLanguageName(int lang)
	{
		switch (lang)
		{
		case Protocol.LANGUAGE_CPP:
			return ELEM_CPP;
		case Protocol.LANGUAGE_CSHARP:
			return ELEM_CSHARP;
		case Protocol.LANGUAGE_JAVA:
			return ELEM_JAVA;
		case Protocol.LANGUAGE_JAVASCRIPT:
			return ELEM_JAVASCRIPT;
		case Protocol.LANGUAGE_ACTIONSCRIPT:
			return ELEM_AS3;
		default:
			throw new RuntimeException("Invalid language code: " + lang);
		}
	}

	static String getRoleName(int role)
	{
		switch (role)
		{
		case Protocol.ROLE_SERVER:
			return ELEM_SERVER;
		case Protocol.ROLE_CLIENT:
			return ELEM_CLIENT;
		default:
			throw new RuntimeException("Invalid role code: " + role);
		}
	}

	static int getLanguageCode(String name)
	{
		if (name.equalsIgnoreCase(ELEM_CPP))
			return Protocol.LANGUAGE_CPP;
		if (name.equalsIgnoreCase(ELEM_CSHARP))
			return Protocol.LANGUAGE_CSHARP;
		if (name.equalsIgnoreCase(ELEM_JAVA))
			return Protocol.LANGUAGE_JAVA;
		if (name.equalsIgnoreCase(ELEM_JAVASCRIPT))
			return Protocol.LANGUAGE_JAVASCRIPT;
		if (name.equalsIgnoreCase(ELEM_AS3))
			return Protocol.LANGUAGE_ACTIONSCRIPT;

		throw new RuntimeException("Unsupported language name: " + name);
	}

	static int getRoleCode(String name)
	{
		if (name.equalsIgnoreCase(ELEM_SERVER))
			return Protocol.ROLE_SERVER;
		if (name.equalsIgnoreCase(ELEM_CLIENT))
			return Protocol.ROLE_CLIENT;

		throw new RuntimeException("Invalid role name: " + name);
	}

}
