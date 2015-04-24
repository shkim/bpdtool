package bpdtool.data;

import java.util.Vector;

public class Constant extends ItemCommons
{
	private boolean m_isEnum;
	private Vector<ConstantField> m_fields;

	@Override
	public String getDataTypeName()
	{
		return m_isEnum ? "Enum" : "Defines";
	}

	public Constant(boolean en)
	{
		m_fields = new Vector<ConstantField>();
		m_isEnum = en;
	}

	public boolean isEnum()
	{
		return m_isEnum;
	}

	public void addField(ConstantField fld)
	{
		m_fields.add(fld);
	}

	public Vector<ConstantField> getFields()
	{
		return m_fields;
	}

	public static Constant createNew(boolean en)
	{
		Constant r = new Constant(en);
		r.setName(r.isEnum() ? "(New Enum)" : "(New Defines)");
		return r;
	}

	public static Constant makeCopy(Constant src, Protocol doc)
	{
		Constant r = new Constant(src.isEnum());
		r.copy(src, doc);

		for (ConstantField fld : src.m_fields)
		{
			r.m_fields.add(fld.clone());
		}

		return r;
	}
}
