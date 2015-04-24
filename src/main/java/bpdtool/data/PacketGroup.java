package bpdtool.data;

import java.util.ArrayList;

public class PacketGroup extends ItemCommons
{
	private ArrayList<Packet> m_packets;
	private String m_startId;

	public String getDataTypeName()
	{
		return "PacketGroup";
	}

	public PacketGroup()
	{
		m_packets = new ArrayList<Packet>();
	}

	public static PacketGroup createNew()
	{
		PacketGroup r = new PacketGroup();
		r.setName("(New PacketGroup)");
		return r;
	}

	public ArrayList<Packet> getPackets()
	{
		return m_packets;
	}

	public void addPacket(Packet pkt)
	{
		m_packets.add(pkt);
	}

	public void addPacket(int pos, Packet pkt)
	{
		m_packets.add(pos, pkt);
	}

	public void setStartId(String num)
	{
		m_startId = num;
	}

	public String getStartId()
	{
		return m_startId;
	}

	public boolean isMyPacket(Packet pkt)
	{
		return m_packets.contains(pkt);
	}

	public boolean isSamePos(Packet pkt, int pos)
	{
		int index = m_packets.indexOf(pkt);
		if (index < 0)
			return false;

		return (pos == index || pos == (index +1));
	}

	public void movePacketPos(Packet pkt, int toPos)
	{
		int oldPos = m_packets.indexOf(pkt);
		if (oldPos < 0)
			throw new RuntimeException(String.format("Packet %s not found in Group %s", pkt.getName(), getName()));

		m_packets.remove(oldPos);
		if (oldPos < toPos)
			toPos--;
		m_packets.add(toPos, pkt);
	}

	public void removePacket(Packet pkt)
	{
		if (!m_packets.contains(pkt))
			throw new RuntimeException(String.format("Packet %s not found in Group %s", pkt.getName(), getName()));

		m_packets.remove(pkt);
	}
}
