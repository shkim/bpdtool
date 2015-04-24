package bpdtool.data;

import bpdtool.Util;

public class RepeatInfo
{
	public static final int TYPE_ONCE = 1;
	public static final int TYPE_FIXED = 2;
	public static final int TYPE_AUTO_VAR = 3;
	public static final int TYPE_BY_REFERENCE = 5;

	private int m_type;
	private int m_count;
	private int m_limit;
	private String m_reference;

	public RepeatInfo()
	{
		setOnce();
	}

	public void setOnce()
	{
		m_type = TYPE_ONCE;
		m_count = 1;
		m_limit = 1;
		m_reference = null;
	}

	public RepeatInfo(RepeatInfo src)
	{
		set(src);
	}

	public void set(RepeatInfo src)
	{
		m_type = src.m_type;
		m_count = src.m_count;
		m_limit = src.m_limit;
		m_reference = src.m_reference;
	}

	// four cases:
	//	1. No Repeat: null
	//	2. Fixed:	"count_number"
	//	3. Auto:	"~limit_number"
	//	4. by reference:	"refer_name,~limit_number"
	public RepeatInfo(String spec) throws Exception
	{
		int[] outParam = new int[1];

		if (spec == null)
		{
			setOnce();
		}
		else if(spec.charAt(0) == '~')
		{
			// auto
			String num = spec.substring(1);
			m_limit = Integer.parseInt(num);
			if(m_limit <= 1)
				throw new Exception();

			m_count = -1;
			m_type = TYPE_AUTO_VAR;
			m_reference = null;
		}
		else if (Util.tryParseInt(spec, outParam))
		{
			m_count = outParam[0];
			if(m_count == 1)
			{
				// error case but...
				setOnce();
			}
			else if (m_count > 1)
			{
				m_type = TYPE_FIXED;
				m_limit = m_count;
				m_reference = null;
			}
			else
			{
				throw new Exception();
			}
		}
		else
		{
			int sep = spec.indexOf(",~");
			if(sep < 0)
				throw new Exception();

			m_reference = spec.substring(0, sep);
			m_limit = Integer.parseInt(spec.substring(sep +2));
			if(m_limit < 1 || m_reference.isEmpty())
				throw new Exception();

			m_count = -1;
			m_type = TYPE_BY_REFERENCE;
		}
	}

	public void set(int val)
	{
		m_type = TYPE_FIXED;
		m_count = val;
		m_limit = 0;
		m_reference = null;
	}

	public boolean isOnce()
	{
		if (m_type == TYPE_ONCE)
		{
			assert (m_count == 1 && m_limit == 1 && m_reference == null);
			return true;
		}

		return false;
	}

	public boolean hasRepeat()
	{
		return !isOnce();
	}

	public boolean isVariableRepeat()
	{
		if (m_type == TYPE_AUTO_VAR || m_type == TYPE_BY_REFERENCE)
		{
			return true;
		}

		return false;
	}

	public boolean isSame(RepeatInfo x)
	{
		return (m_type == x.m_type && m_count == x.m_count && m_limit == x.m_limit && m_reference == x.m_reference);
	}

	public int getType()
	{
		return m_type;
	}

	public void setType(int v)
	{
		m_type = v;
	}

	public int getLimit()
	{
		return m_limit;
	}

	public void setLimit(int n)
	{
		m_limit = n;
	}

	public int getCount()
	{
		return m_count;
	}

	public void setCount(int n)
	{
		m_count = n;
	}

	public String getReference()
	{
		return m_reference;
	}

	public void setReference(String s)
	{
		m_reference = s;
	}

	@Override
	public String toString()
	{
		switch(m_type)
		{
		case TYPE_ONCE:
			return "1";

		case TYPE_FIXED:
			assert(m_count > 1);
			return String.valueOf(m_count);

		case TYPE_AUTO_VAR:
			assert(m_limit > 1);
			return String.format("~%d", m_limit);

		case TYPE_BY_REFERENCE:
			assert(!m_reference.isEmpty());
			return String.format("%s,~%d", m_reference, m_limit);
		}

		assert(false);
		return "(error)";
	}

	public String toCodeString()
	{
		switch(m_type)
		{
		case TYPE_ONCE:
			assert(m_count > 1);
			return String.valueOf(m_count);

		case TYPE_BY_REFERENCE:
			assert(!m_reference.isEmpty());
			return m_reference;
		}

		assert(false);
		return "(error)";
	}

	public String toDecorativeString()
	{
		switch (m_type)
		{
		case TYPE_ONCE:
			return "1";

		case TYPE_FIXED:
			assert(m_count > 1);
			return String.valueOf(m_count);

		case TYPE_AUTO_VAR:
			assert(m_limit > 1);
			return String.format("(Auto),~%d", m_limit);

		case TYPE_BY_REFERENCE:
			assert(!m_reference.isEmpty());
			return String.format("By %s,~%d", m_reference, m_limit);
		}

		assert(false);
		return "(error)";
	}
}
