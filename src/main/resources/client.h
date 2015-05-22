
// YOU MAY USE THIS FILE AS THE SKELETON CODE OF THE CLIENT-SIDE SOCKET CLASS.

#include "netstream.h"

$$NamespaceBegin$$
// Rename _$$ClassName$$.h to $$ClassName$$.h, then add to your project & edit.
class $$ClassName$$ : public ITcpSocketListener
{
public:
	virtual void OnDisconnect();

#include "$$IncludeInlineFile$$"

	virtual void OnConnect(ITcpSocket* pSocket, int nErrorCode)
	{
		if(nErrorCode != 0)
		{
			// TODO: error handling
			return;
		}

		m_pSocket = pSocket;
		$$VarDispatchTable$$ = $$VarDispatchTableBegin$$;
		// TODO: jobs on connect
	}

	virtual unsigned int OnReceive(char* pBuffer, unsigned int nLength)
	{
		NetStreamReader$$SwapEndian$$ _nsr(pBuffer);

	_nextPacket:

		unsigned int nPacketID = $$PacketIdGet$$;
		if(nPacketID > $$MaxPacketID$$)
		{
	_invPacket:
			TRACE("Invalid packet id: %d\n", nPacketID);
			m_pSocket->Kick();
			return 0;
		}

		// To process a packet, the buffer should contain at least $$PacketHeaderSize$$ bytes.
		if(nLength > $$PacketHeaderSize$$)
		{
			unsigned int nPacketLen = _nsr._ReadWord($$PacketLengthPos$$);
			if(nPacketLen <= nLength)
			{
				if($$VarDispatchTable$$[nPacketID] == NULL)
					goto _invPacket;

				_nsr.MapPacket($$PacketHeaderSize$$, nPacketLen);
				if(false == (this->*$$VarDispatchTable$$[nPacketID])(_nsr))
				{
					// error
					m_pSocket->Kick();
					return 0;
				}

				nLength -= nPacketLen;

				if(nLength > $$PacketHeaderSize$$)
				{
					// process more packet
					goto _nextPacket;
				}
			}
			else if(nPacketLen > 2048)
			{
				TRACE("Invalid packet length:%d (pkid=%d)\n", nPacketLen, nPacketID);
				m_pSocket->Kick();
				return 0;
			}
		}

		return (int) _nsr.GetOffsetFrom(pBuffer);
	}
};

$$NamespaceEnd$$
