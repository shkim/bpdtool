package bpdtool.data;

public class BlindClass extends UserType
{
	@Override
	public String getDataTypeName()
	{
		return "BlindClass";
	}

	public static BlindClass createNew()
	{
		BlindClass r = new BlindClass();
		r.setName("(New Blind Class)");
		return r;
	}

	public static BlindClass makeCopy(BlindClass src, Protocol doc)
	{
		BlindClass r = new BlindClass();
		r.copy(src, doc);

		return r;
	}
}
