package bpdtool.data;

public class StructField extends ItemCommons
{
	private String m_type;
//	public int nRepeat;	// -1 if PacketField.Repeat references another field
	private RepeatInfo m_repeat;
	private PrimitiveType m_typePrimitive = null;
	private UserType m_typeCustom = null;

	@Override
	public String getDataTypeName()
	{
		throw new UnsupportedOperationException();
	}

	public String getType()
	{
		return m_type;
	}

	public void setType(String t)
	{
		m_type = t;
	}

	public RepeatInfo getRepeatInfo()
	{
		return m_repeat;
	}

	public void setRepeatInfo(RepeatInfo ri)
	{
		m_repeat = ri;
	}

	public boolean isCustomType()
	{
		return (m_typeCustom != null);
	}

	public UserType getCustomType()
	{
		return m_typeCustom;
	}

	public void setCustomType(UserType ut)
	{
		m_typeCustom = ut;
	}

	public PrimitiveType getPrimitiveType()
	{
		return m_typePrimitive;
	}

	public void setPrimitiveType(PrimitiveType pt)
	{
		m_typePrimitive = pt;
	}

	protected void initNew()
	{
		setName("NewField");
		m_type = "int";
//		this.nRepeat = 1;
		m_repeat = new RepeatInfo();
		m_typePrimitive = Protocol.getIfPrimitive(m_type);
		m_typeCustom = null;
	}

	protected void copyTo(StructField r)
	{
		r.setName(getName());
		r.setComment(getComment());
		r.setDescription(getDescription());

		r.setType(m_type);
		r.m_repeat = new RepeatInfo(m_repeat);
		r.m_typePrimitive = m_typePrimitive;
		r.m_typeCustom = m_typeCustom;
		if (r.m_typeCustom != null)
		{
			r.m_typeCustom.incRef();
		}
	}

	public static StructField createNew()
	{
		StructField r = new StructField();
		r.initNew();
		return r;
	}

	public StructField clone()
	{
		StructField r = new StructField();
		copyTo(r);
		return r;
	}
}
