package bpdtool.data;

public class PrimitiveType implements Comparable
{
	public static final int BOOLEAN = 1;
	public static final int SIGNED_INTEGER = 2;
	public static final int UNSIGNED_INTEGER = 3;
	public static final int FLOAT = 4;

	public static final int STRING = 10;
	public static final int STRING_TINY = 11;
	public static final int WIDESTRING = 12;
	public static final int WIDESTRING_TINY = 13;

	public static final int BUFFER = 20;
	public static final int BUFFER_TINY = 21;


	private int m_category;
	private int m_sizeBytes;
	private String m_description;

	public PrimitiveType(int cate, int cb, String description)
	{
		m_category = cate;
		m_sizeBytes = cb;
		m_description = description;
	}

	public int getCategory()
	{
		return m_category;
	}

	public int getSizeBytes()
	{
		return m_sizeBytes;
	}

	public boolean isVariableLengthTiny()
	{
		return (m_category == STRING_TINY || m_category == WIDESTRING_TINY || m_category == BUFFER_TINY);
	}

	public String getDescription()
	{
		return m_description;
	}

	@Override
	public int hashCode()
	{
		return (m_category <<4) | m_sizeBytes;
	}

	@Override
	public boolean equals(Object b)
	{
		if (b instanceof PrimitiveType)
		{
			return (hashCode() == b.hashCode());
		}

		return false;
	}

	@Override
	public int compareTo(Object b)
	{
		return (hashCode() - b.hashCode());
	}
}
