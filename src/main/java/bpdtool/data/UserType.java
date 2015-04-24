package bpdtool.data;

public abstract class UserType extends ItemCommons
{
	public boolean m_isImported;
	public int m_nRefCnt;
	public int m_nBytes;	// sum of fields

	public boolean isImported()
	{
		return m_isImported;
	}

	public void setImported(boolean b)
	{
		m_isImported = b;
	}

	public int getRefCnt()
	{
		return m_nRefCnt;
	}

	public void incRef()
	{
		++m_nRefCnt;
	}

	public void decRef()
	{
		--m_nRefCnt;
	}

	public int getSizeBytes()
	{
		return m_nBytes;
	}

	public void setSizeBytes(int s)
	{
		m_nBytes = s;
	}

	public UserType()
	{
		m_isImported = false;
		m_nRefCnt = 0;
		m_nBytes = -1;
	}
}
