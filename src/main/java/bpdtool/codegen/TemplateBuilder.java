package bpdtool.codegen;

public class TemplateBuilder
{
	private StringBuilder m_buffer;

	public TemplateBuilder(String templ)
	{
		reset(templ);
	}

	public void reset(String templ)
	{
		m_buffer = new StringBuilder(templ);
	}

	public void substitute(String marker, String str)
	{
		while(true)
		{
			int start = m_buffer.indexOf(marker);
			if(start < 0)
				break;

			m_buffer = m_buffer.replace(start, start + marker.length(), str);
		}
	}

	public String getResult()
	{
		return m_buffer.toString();
	}
}
