package bpdtool.data;

public class PacketStage
{
	private int m_index;
	private String m_abbr;
	private String m_name;
	public PacketStage _src;

	public PacketStage(int i)
	{
		m_index = i;
	}

	public PacketStage(int i, String a, String n)
	{
		m_index = i;
		m_abbr = a;
		m_name = n;
	}

	public PacketStage(PacketStage src)
	{
		m_index = src.m_index;
		m_abbr = src.m_abbr;
		m_name = src.m_name;
		_src = src;
	}

	public int getIndex()
	{
		return m_index;
	}

	public void setIndex(int idx)
	{
		m_index = idx;
	}

	public String getAbbr()
	{
		return m_abbr;
	}

	public String getName()
	{
		return m_name;
	}

	public void setAbbr(String abbr)
	{
		m_abbr = abbr;
	}

	public void setName(String name)
	{
		m_name = name;
	}
}
