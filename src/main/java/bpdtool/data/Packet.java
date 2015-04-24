package bpdtool.data;

import bpdtool.codegen.ITextWriter;

import java.util.ArrayList;
import java.util.Set;

public class Packet extends ItemCommons
{
	public static final int FLOW_C2S = 1;
	public static final int FLOW_S2C = 2;
	public static final int FLOW_INTER = 3;
/*
	public class PacketFlowTypes
	{
		public static final int 
		C2S = 1,
		S2C = 2,
		INTER = 3;
	}
*/

	private int m_flow;
	private ArrayList<PacketField> m_fields;
	private boolean m_isDirectCasting;
	private boolean m_isGenerateBuilder;
	//private boolean m_isEnableFiber;
	private boolean m_isAllStage;
	private ArrayList<PacketStage> m_stages;

	private int m_nRepeatDepth;		// TODO: delete it (currently not using it..)
	private int m_nDirectCastPacketLen;
	private boolean m_hasAutoRepeat;
	private boolean m_hasVarLenRepeat;
	private int m_nMaxPacketBufferSize;	// size for local packet composition buffer

	private int m_exportC2SID;
	private int m_exportS2CID;

	@Override
	public String getDataTypeName()
	{
		return "Packet";
	}

	public Packet()
	{
		m_fields = new ArrayList<PacketField>();
		m_stages = new ArrayList<PacketStage>();
		m_isAllStage = true;
	}

	public static Packet createNew()
	{
		Packet r = new Packet();
		r.setName("(New Packet)");
		r.m_flow = FLOW_C2S;
		return r;
	}

	public static Packet makeCopy(Packet src, Protocol doc)
	{
		Packet r = new Packet();
		r.copy(src, doc);

		r.m_flow = src.m_flow;
		r.m_isDirectCasting = src.m_isDirectCasting;
		r.m_isGenerateBuilder = src.m_isGenerateBuilder;
		for (PacketField fld : src.m_fields)
		{
			r.m_fields.add(fld.clonePacketField());
		}

		r.m_isAllStage = src.m_isAllStage;
		r.m_stages.addAll(src.m_stages);

		return r;
	}

	public int getFlow()
	{
		return m_flow;
	}

	public void setFlow(int flow)
	{
		m_flow = flow;
	}

	public boolean isDirectCasting()
	{
		return m_isDirectCasting;
	}

	public void setDirectCasting(boolean dc)
	{
		m_isDirectCasting = dc;
	}

	public boolean isGenerateBuilder()
	{
		return m_isGenerateBuilder;
	}

	public void setGenerateBuilder(boolean gb)
	{
		m_isGenerateBuilder = gb;
	}

	public boolean isAllStage()
	{
		return m_isAllStage;
	}

	public void setAllStage(boolean b)
	{
		m_isAllStage = b;
	}

	public ArrayList<PacketStage> getStages()
	{
		return m_stages;
	}

	public final ArrayList<PacketField> getFields()
	{
		return m_fields;
	}

	public void addField(PacketField pf)
	{
		m_fields.add(pf);
	}

	public int getDirectCastPacketLen()
	{
		return m_nDirectCastPacketLen;
	}

	public boolean hasAutoRepeat()
	{
		return m_hasAutoRepeat;
	}

	public boolean hasVarLenRepeat()
	{
		return m_hasVarLenRepeat;
	}

	public int getMaxPacketBufferSize()
	{
		return m_nMaxPacketBufferSize;
	}

	public void prepareExport(ITextWriter logger, Set<Integer> expLangs)
	{
		m_nRepeatDepth = 0;
		m_hasAutoRepeat = false;
		m_hasVarLenRepeat = false;
		m_nMaxPacketBufferSize = 4;

		m_exportC2SID = -1;
		m_exportS2CID = -1;

		boolean bRootRepeat = false;
		int nStringFieldCnt = 0;
		for (int i = 0; i < m_fields.size(); i++)
		{
			PacketField fld = m_fields.get(i);

			int curFieldBytes = 4;
			if (fld.getPrimitiveType() != null)
			{
				if (fld.getPrimitiveType().getSizeBytes() > 0)
				{
					curFieldBytes = fld.getPrimitiveType().getSizeBytes();
				}
				else
				{
					nStringFieldCnt++;
				}
			}
			else if (fld.getCustomType() != null)
			{
				curFieldBytes = (fld.getCustomType().getSizeBytes() > 0) ? fld.getCustomType().getSizeBytes() : 256;	// FIXME 256
			}
			else
			{
				throw new RuntimeException("Invalid field type at " + i);
			}

			if (expLangs.contains(Integer.valueOf(Protocol.LANGUAGE_ACTIONSCRIPT)))
			{
				if (fld.getPrimitiveType() != null)
				{
					if (fld.getPrimitiveType().getSizeBytes() > 4)
					{
						logger.writeln("{0}.{1}: 64bit integer is not supported in ActionScript", this.getName(), fld.getName());
					}
					else if (fld.getPrimitiveType().getCategory() >= PrimitiveType.WIDESTRING
						&& fld.getPrimitiveType().getCategory() < PrimitiveType.BUFFER)
					{
						logger.writeln("{0}.{1}: WIDESTRING is not accessible in ActionScript. Use String with UTF-8 encoding.", this.getName(), fld.getName());
					}
				}
			}

			if (fld.getRepeatInfo().isVariableRepeat())
			{
				bRootRepeat = true;
				m_hasVarLenRepeat = true;

				if (fld.getRepeatInfo().getType() == RepeatInfo.TYPE_AUTO_VAR)
				{
					m_hasAutoRepeat = true;
				}
				else
				{
					// check if valid repeater field
					boolean valid = false;
					for (int k = 0; k < i; k++)
					{
						if (m_fields.get(k).getName().equals(fld.getRepeatInfo().getReference()))
						{
							valid = true;
							break;
						}

					}

					if (!valid)
					{
						logger.writeln("Invalid repeater reference: " + fld.getRepeatInfo().getReference());
						return;
					}
				}

				assert (fld.getRepeatInfo().getLimit() >= 2);
				m_nMaxPacketBufferSize += curFieldBytes * fld.getRepeatInfo().getLimit();
			}
			else if (fld.getRepeatInfo().hasRepeat())
			{
				assert (fld.getRepeatInfo().getType() == RepeatInfo.TYPE_FIXED);
				m_nMaxPacketBufferSize += curFieldBytes * fld.getRepeatInfo().getCount();

				if (fld.isCustomType())
				{
					bRootRepeat = true;
				}
			}
			else
			{
				assert (fld.getRepeatInfo().isOnce());
				m_nMaxPacketBufferSize += curFieldBytes;
			}

			if (fld.isCustomType() && fld.getCustomType() instanceof Struct)
			{
				int _cur = ((Struct) fld.getCustomType()).getMaxRepeatDepth();
				if (_cur > m_nRepeatDepth)
				{
					m_nRepeatDepth = _cur;
				}
			}
		}

		if (m_nRepeatDepth == 0 && bRootRepeat)
		{
			m_nRepeatDepth = 1;
		}

		if(m_isGenerateBuilder && m_fields.isEmpty())
		{
			logger.writeln("Can't generate the builder method for empty-field packet ({0}.", getName());
			return;
		}

		if (m_isDirectCasting)
		{
			m_nDirectCastPacketLen = 0;
			for (PacketField fld : m_fields)
			{
				if (fld.getRepeatInfo().isVariableRepeat())
				{
					logger.writeln("Variable-length repeat ({0}) is not allowed for Direct-Casting packet ({1}).", fld.getName(), getName());
					return;
				}

				if (fld.getPrimitiveType() != null)
				{
					if (fld.getPrimitiveType().getSizeBytes() > 0)
					{
						if (fld.getRepeatInfo().isOnce())
						{
							m_nDirectCastPacketLen += fld.getPrimitiveType().getSizeBytes();
						}
						else
						{
							m_nDirectCastPacketLen += fld.getPrimitiveType().getSizeBytes() * fld.getRepeatInfo().getCount();
						}

						continue;
					}
					else
					{
						logger.writeln("Variable-length type ({0}) is not allowed for Direct-Casting packet ({1}).", fld.getName(), getName());
						return;
					}
				}
				else if (fld.getCustomType() instanceof BlindClass)
				{
					logger.writeln("BlindClass field ({0}) is not allowed for Direct-Casting packet({1}).", fld.getName(), getName());
					return;
				}
				else
				{
					Struct st = (Struct) fld.getCustomType();
					if (st.getSizeBytes() > 0)
					{
						if (fld.getRepeatInfo().isOnce())
						{
							m_nDirectCastPacketLen += st.getSizeBytes();
						}
						else
						{
							m_nDirectCastPacketLen += st.getSizeBytes() * fld.getRepeatInfo().getCount();
						}

						continue;
					}
					else
					{
						logger.writeln("Variable-length struct type ({0}) is not allowed for Direct-Casting packet ({1}).", fld.getName(), getName());
						return;
					}
				}
			}
		}

		if (nStringFieldCnt > 0)
		{
			if (nStringFieldCnt <= 2)
			{
				m_nMaxPacketBufferSize += 1024;
			}
			else
			{
				m_nMaxPacketBufferSize += 2048;
			}
		}

		m_nMaxPacketBufferSize = ((m_nMaxPacketBufferSize + 31) / 32) * 32;
	}

	public void setExportS2CID(int pkid)
	{
		m_exportS2CID = pkid;
	}

	public void setExportC2SID(int pkid)
	{
		m_exportC2SID = pkid;
	}

	public int getExportS2CID()
	{
		if (m_exportS2CID < 0)
			throw new RuntimeException("S->C Packet ID for " + getName() + " not assigned.");

		return m_exportS2CID;
	}

	public int getExportC2SID()
	{
		if (m_exportC2SID < 0)
			throw new RuntimeException("C->S Packet ID for " + getName() + " not assigned.");

		return m_exportC2SID;
	}
}
