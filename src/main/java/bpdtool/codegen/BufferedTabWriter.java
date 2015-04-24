package bpdtool.codegen;

import bpdtool.Util;

public class BufferedTabWriter implements ITextWriter
{
	public static final int TABSTOP = 4;

	private StringBuilder m_sb;

	public BufferedTabWriter()
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

	public void begin()
	{
		m_sb.setLength(0);
	}

	public void end(ITextWriter w, int nBeginTabs)
	{
		int nMax = 0;

		String[] lines = m_sb.toString().split("\n|\r");

		for (String line : lines)
		{
			if(line.isEmpty())
				continue;

			int iTab = line.indexOf('\t');
			if (iTab > 0)
			{
				int cur = (iTab + TABSTOP) / TABSTOP;
				cur *= TABSTOP;

				if (nMax < cur)
					nMax = cur;
			}
		}

		String begintabs = "";
		while (nBeginTabs-- > 0)
			begintabs += "\t";

		for (String line : lines)
		{
			w.write(begintabs);

			int iTab = line.indexOf('\t');
			if (iTab < 0)
			{
				w.writeln(line);
			}
			else
			{
				w.write(line.substring(0, iTab));

				int req = (nMax - iTab + TABSTOP - 1) / TABSTOP;
				while (req-- > 0)
					w.write("\t");

				w.writeln(line.substring(iTab));
			}
		}
	}
}