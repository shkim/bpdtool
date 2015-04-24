package bpdtool.data;

public class ConstantField extends ItemCommons
{
	private String m_value;
	private boolean m_isValueSet;

	@Override
	public String getDataTypeName()
	{
		throw new UnsupportedOperationException();
	}

	public String getValue()
	{
		return m_value;
	}

	public void setValue(String v)
	{
		m_value = v;
		m_isValueSet = (v != null);
	}

	public boolean isValueSet()
	{
		return m_isValueSet;
	}

	public static ConstantField createNew()
	{
		ConstantField r = new ConstantField();
		r.setName("NewField");
		r.setValue("1");
		return r;
	}

	public ConstantField clone()
	{
		ConstantField r = new ConstantField();

		r.setName(getName());
		r.setComment(getComment());
		r.setDescription(getDescription());
		r.setValue(getValue());

		return r;
	}
}
