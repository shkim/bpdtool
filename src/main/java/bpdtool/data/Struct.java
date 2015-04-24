package bpdtool.data;

import bpdtool.codegen.ErrorLogger;
import java.util.*;

public class Struct extends UserType
{
	private ArrayList<StructField> m_fields;

	@Override
	public String getDataTypeName()
	{
		return "Struct";
	}

	public Struct()
	{
		m_fields = new ArrayList<StructField>();
	}

	public static Struct createNew()
	{
		Struct r = new Struct();
		r.setName("(New Struct)");
		return r;
	}

	public static Struct makeCopy(Struct src, Protocol doc)
	{
		Struct r = new Struct();
		r.copy(src, doc);

		for (StructField fld : src.m_fields)
		{
			r.m_fields.add(fld.clone());
		}

		return r;
	}

	public final ArrayList<StructField> getFields()
	{
		return m_fields;
	}

	public void addField(StructField sf)
	{
		m_fields.add(sf);
	}

	public void calcBytes()
	{
		int cb, bytes = 0;
		for (StructField fld : m_fields)
		{
			if (fld.isCustomType())
			{
				cb = fld.getCustomType().getSizeBytes();
				if (cb == 0)
				{
					bytes = 0;
					break;
				}

				bytes += cb;
			}
			else
			{
				cb = fld.getPrimitiveType().getSizeBytes();
				if (cb == 0)
				{
					bytes = 0;
					break;
				}

				bytes += cb;
			}
		}

		setSizeBytes(bytes);
	}

	private boolean m_hasRepeat;

	public void prepareExport(ErrorLogger err)
	{
		m_hasRepeat = false;

		for (StructField fld : m_fields)
		{
			//if(fld.nRepeat > 1 && fld.TypeCustom != null)
			if (fld.getRepeatInfo().getType() != RepeatInfo.TYPE_ONCE && fld.isCustomType())
			{
				m_hasRepeat = true;
				return;
			}
		}
	}

	public int getMaxRepeatDepth()
	{
		if (!m_hasRepeat)
		{
			return 0;
		}

		int retmax = 1;

		for (StructField fld : m_fields)
		{
			if (fld.getCustomType() instanceof Struct)
			{
				int cur = ((Struct) fld.getCustomType()).getMaxRepeatDepth() + 1;
				if (cur > retmax)
				{
					retmax = cur;
				}
			}
		}

		return retmax;
	}
}
