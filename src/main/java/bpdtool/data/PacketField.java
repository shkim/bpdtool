package bpdtool.data;

public class PacketField extends StructField
{
//	String Repeat;

	public static PacketField createNewPacketField()
	{
		PacketField r = new PacketField();
		r.initNew();
//		r.Repeat = "1";
		return r;
	}

	public PacketField clonePacketField()
	{
		PacketField r = new PacketField();
		copyTo(r);
//		r.Repeat = this.Repeat;
		return r;
	}
}
