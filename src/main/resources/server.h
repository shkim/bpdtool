// YOU MAY USE THIS FILE AS THE SKELETON CODE OF THE SERVER-SIDE SOCKET CLASS.

#include "netstream.h"
#include "_$$CommonHeader$$.h"

$$NamespaceBegin$$
// Rename _$$ClassName$$.h to $$ClassName$$.h, then add to your project & edit.
class $$ClassName$$ : public ServerCore::BaseClientListener
{
public:
	static ITcpClientListener* Creator(void*) { return new $$ClassName$$(); }
	virtual void OnFinalDestruct() { delete this; }

#include "$$IncludeInlineFile$$"

	virtual void OnRelease();
	virtual void OnDisconnect();

	virtual bool OnConnect(ITcpSocket* pSocket, int nErrorCode)
	{
		m_pSocket = pSocket;
		$$VarDispatchTable$$ = $$VarDispatchTableBegin$$;
		return true;
	}

	virtual unsigned int OnReceive(char* pBuffer, unsigned int nLength)
	{
		NetStreamReader$$SwapEndian$$ _nsr(pBuffer);

	_nextPacket:

		unsigned int nPacketID = $$PacketIdGet$$;
		if(nPacketID > $$MaxPacketID$$)
		{
	_invPacket:
			Log(LOG_WARNING, "Invalid packet id: %d\nClosing client %s\n", nPacketID, m_pSocket->GetRemoteAddr());
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
				Log(LOG_WARNING, "Invalid packet length:%d(pkid=%d)\nClosing client %s\n",
					nPacketLen, nPacketID, m_pSocket->GetRemoteAddr());

				m_pSocket->Kick();
				return 0;
			}
		}

		return (int) _nsr.GetOffsetFrom(pBuffer);
	}
};

$$NamespaceEnd$$