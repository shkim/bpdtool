package bpdtool.codegen;

import java.io.*;
import bpdtool.Util;

class NewlineNormalizer
{
	private PrintStream m_ps;
	private String m_newline;
	private int m_lastCRLF = 0;
	private int m_deferCnt = 0;

	public NewlineNormalizer(PrintStream ps, String newline)
	{
		m_ps = ps;
		m_newline = newline;
	}

	public void push(String str)
	{
		int len = str.length();
		for(int i=0; i<len; i++)
		{
			char ch = str.charAt(i);

			if(ch == 0x0D)
			{
				if(m_lastCRLF == 0x0D || m_deferCnt >= 2)
				{
					newline();
				}

				m_lastCRLF = 0x0D;
				m_deferCnt++;
			}
			else if(ch == 0x0A)
			{
				if(m_lastCRLF == 0x0A || m_deferCnt >= 2)
				{
					newline();
				}

				m_lastCRLF = 0x0A;
				m_deferCnt++;
			}
			else
			{
				flush();
				m_ps.print(ch);
			}
		}
	}

	public void flush()
	{
		if(m_lastCRLF != 0)
		{
			newline();
		}
	}

	private void newline()
	{
		m_ps.print(m_newline);
		m_lastCRLF = 0;
		m_deferCnt = 0;
	}
}

public class StreamWriter implements ITextWriter
{
	private PrintStream m_ps;
	private NewlineNormalizer m_nn;

	// only used for gui preview
	private String m_filename;
	private ByteArrayOutputStream m_baos;

	public StreamWriter(File file, String enc, String newline) throws Exception
	{
		FileOutputStream fos = new FileOutputStream(file);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		m_ps = new PrintStream(bos, true, enc);
		m_nn = new NewlineNormalizer(m_ps, newline);
	}

	public StreamWriter(String filename) throws Exception
	{
		m_filename = filename;
		m_baos = new ByteArrayOutputStream();
		m_ps = new PrintStream(m_baos, true, "UTF-8");
		m_nn = new NewlineNormalizer(m_ps, "\n");
	}

	public void addBOM(String enc)
	{
		// TODO: unicode LE,BE support

		if("UTF-8".equals(enc))
		{
			m_ps.write(0xEF);
			m_ps.write(0xBB);
			m_ps.write(0xBF);
		}
		else if("UTF-16".equals(enc))
		{
			m_ps.write(0xFF);
			m_ps.write(0xFE);
		}
	}

	@Override
	public void write(String format, Object ... args)
	{
		m_nn.push(Util.stringFormat(format, args));
	}

	@Override
	public void writeln(String format, Object ... args)
	{
		m_nn.push(Util.stringFormat(format, args) + "\n");
	}

	public void close()
	{
		m_nn.flush();
		m_ps.close();
	}

	public String getFilename()
	{
		return m_filename;
	}

	public String getResult() throws Exception
	{
		return m_baos.toString("UTF-8");
	}
}
