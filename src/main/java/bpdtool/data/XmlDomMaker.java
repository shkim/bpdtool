package bpdtool.data;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import bpdtool.Util;
import org.w3c.dom.*;

import java.util.Stack;
import java.io.FileOutputStream;

public class XmlDomMaker
{
	private Document m_dom;
	private Stack<Element> m_stack;
	private Element m_curElement;

	public XmlDomMaker() throws Exception
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		m_dom = db.newDocument();
	}
	
	public void writeStartDocument()
	{
		m_stack = new Stack<Element>();
		m_curElement = null;
	}

	public void writeEndDocument()
	{
		assert(m_stack.isEmpty());
	}

	public void writeProcessingInstruction(String instr, String value)
	{
		ProcessingInstruction pi = m_dom.createProcessingInstruction(instr, value);
		//dom.insertBefore(pi, dom.getDocumentElement());
	}

	public void writeStartElement(String elemName)
	{
		Element elem = m_dom.createElement(elemName);
		m_stack.push(elem);

		if(m_curElement == null)
			m_dom.appendChild(elem);
		else
			m_curElement.appendChild(elem);

		m_curElement = elem;
	}

	public void writeEndElement()
	{
		Element lastElement = m_curElement;

		assert(m_stack.size() > 0);
		m_stack.pop();
		m_curElement = m_stack.isEmpty() ? null : m_stack.lastElement();

		int numAttrs = lastElement.getAttributes().getLength();
		int numChildTags = lastElement.getChildNodes().getLength();

		if (numAttrs == 0 && numChildTags == 0 && Util.isNullOrEmpty(lastElement.getTextContent()))
		{
			// remove empty element
			if (m_curElement != null)
				m_curElement.removeChild(lastElement);
		}
	}
	
	public void writeAttribute(String name, String value)
	{
		if (!Util.isNullOrEmpty(value))
			m_curElement.setAttribute(name, value);
	}

	public void writeCharacters(String text)
	{
		if (!Util.isNullOrEmpty(text))
			m_curElement.setTextContent(text);
	}

	public void writeSimpleElement(String tag, String text)
	{
		if (!Util.isNullOrEmpty(text))
		{
			writeStartElement(tag);
			writeCharacters(text);
			writeEndElement();
		}
	}

	public void saveToFile(String filename) throws Exception
	{
		Transformer tr = TransformerFactory.newInstance().newTransformer();
		tr.setOutputProperty(OutputKeys.METHOD,"xml");
		tr.setOutputProperty(OutputKeys.INDENT, "yes");
		tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		m_dom.setXmlStandalone(true);

		FileOutputStream fos = new FileOutputStream(filename);
		tr.transform(new DOMSource(m_dom), new StreamResult(fos));
		fos.close();
	}


	// read helpers -->

	public static String getAttributeString(Node node, String attr, String defl)
	{
		Node ret = node.getAttributes().getNamedItem(attr);
		return (ret == null) ? defl : ret.getNodeValue().trim();
	}

	public static int getAttributeInt(Node node, String attr)
	{
		return Integer.parseInt(getAttributeString(node, attr, "0"));
	}

	public static boolean getAttributeBoolean(Node node, String attr)
	{
		Node ret = node.getAttributes().getNamedItem(attr);
		if (ret == null)
			return false;

		return Util.isStringTrue(ret.getNodeValue().trim());
	}

	public static Node getChildElementNode(Node node, String elem)
	{
		NodeList nodes = node.getChildNodes();
		for(int n=0; n<nodes.getLength(); n++)
		{
			Node child = nodes.item(n);
			if (child.getNodeName().equals(elem))
				return child;
		}

		return null;
	}

	public static String getChildElementText(Node node, String elem)
	{
		NodeList nodes = node.getChildNodes();
		for(int n=0; n<nodes.getLength(); n++)
		{
			Node child = nodes.item(n);
			if (child.getNodeName().equals(elem))
				return child.getTextContent().trim();
		}

		return null;
	}
}
