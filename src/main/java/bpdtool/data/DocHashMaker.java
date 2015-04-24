package bpdtool.data;

import java.security.MessageDigest;

public class DocHashMaker
{
	private MessageDigest m_md;

	public DocHashMaker() throws Exception
	{
		m_md = MessageDigest.getInstance("SHA-512");
		m_md.reset();
	}

	public void update(String str)
	{
		if(str != null)
			m_md.update(str.getBytes());
	}

	public void update(boolean b)
	{
		m_md.update(b ? (byte)1 : (byte)0);
	}

	public void update(int n)
	{
		m_md.update((byte)((n >> 24) & 0xff));
		m_md.update((byte)((n >> 16) & 0xff));
		m_md.update((byte)((n >> 8) & 0xff));
		m_md.update((byte)(n & 0xff));
	}

	public String getDigest()
	{
		byte[] mem = m_md.digest();

		StringBuffer hexString = new StringBuffer();

		for (int i = 0; i < mem.length; i++)
		{
			hexString.append(String.format("%02x", mem[i]));
		}

		return hexString.toString();
	}
}
