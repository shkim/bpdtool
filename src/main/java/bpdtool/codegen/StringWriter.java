package bpdtool.codegen;

import bpdtool.Util;

public class StringWriter implements ITextWriter
{
	protected StringBuilder m_sb;

	public StringWriter()
	{
		m_sb = new StringBuilder();
	}

	@Override
	public void write(String format, Object ... args)
	{
		m_sb.append(Util.stringFormat(format, args));
	}

	@Override
	public void writeln(String format, Object ... args)
	{
		write(format + "\n", args);
	}

	@Override
	public String toString()
	{
		return m_sb.toString();
	}

	public void reset()
	{
		m_sb.setLength(0);
	}
}
