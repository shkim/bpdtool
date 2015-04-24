package bpdtool.data;

public abstract class ItemCommons
{
	private String m_name;
	private String m_comment;
	private String m_description;

	public abstract String getDataTypeName();

	public String getName()
	{
		return m_name;
	}

	public void setName(String name)
	{
		m_name = name;
	}

	public String getComment()
	{
		return m_comment;
	}

	public void setComment(String comment)
	{
		m_comment = comment;
	}

	public String getDescription()
	{
		return m_description;
	}

	public void setDescription(String desc)
	{
		m_description = desc;
	}

	protected void copy(ItemCommons src, Protocol doc)
	{
		String nextname = src.getName();

		while(true)
		{
			nextname = getNextName(nextname);

			if(src instanceof Packet)
			{
				if(doc.isPacketNameConflict(nextname, null))
					continue;
			}
			else if(src instanceof UserType)
			{
				if(doc.isTypeNameConflict(nextname, null))
					continue;
			}
			else if(src instanceof Constant)
			{
				if(doc.isConstNameConflict(nextname, null))
					continue;
			}

			break;
		}

		setName(nextname);
		setComment(src.getComment());
		setDescription(src.getDescription());
	}

	public static String getNextName(String name)
	{
		int dpos = name.length() -1;
		if(!Character.isDigit(name.charAt(dpos)))
			return name + "2";

		while(dpos > 0 && Character.isDigit(name.charAt(dpos -1)))
			dpos--;

		int num = Integer.parseInt(name.substring(dpos));
		return name.substring(0, dpos) + String.valueOf(num +1);
	}

	public void updateDocHash(DocHashMaker dhm)
	{
		dhm.update(m_name);
		dhm.update(m_comment);
		dhm.update(m_description);
	}
}
