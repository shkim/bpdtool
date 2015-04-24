
package$$PackageName$$
{
import flash.net.Socket;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;
import flash.utils.ByteArray;
import flash.utils.Endian;
import flash.errors.IOError;
import $$PacketPkgName$$.*;

public class $$ClassName$$
{
	private var m_sock:Socket;
	private var m_listener:$$ListenerName$$;
	private var m_receiverTable:Object;
	private var m_curPacketID:int;
	private var m_curPacketLen:int;
	private var m_buff:ByteArray;
$$StressVars$$
$$CommonConstants$$
$$PacketIDs$$
	public function $$ClassName$$(lsnr:$$ListenerName$$$$StressEnvArg$$)
	{
		m_listener = lsnr;
		
		m_sock = new Socket();
		m_sock.endian = Endian.$$Endian$$;
		m_sock.addEventListener(Event.CONNECT, lsnr.$$LsnrMethodPrefix$$_OnConnect);
		m_sock.addEventListener(Event.CLOSE, lsnr.$$LsnrMethodPrefix$$_OnClose);
		m_sock.addEventListener(IOErrorEvent.IO_ERROR, lsnr.$$LsnrMethodPrefix$$_OnError);
		m_sock.addEventListener(SecurityErrorEvent.SECURITY_ERROR, lsnr.$$LsnrMethodPrefix$$_OnError);
		m_sock.addEventListener(ProgressEvent.SOCKET_DATA, OnReceive);

		m_receiverTable = new Object();
$$HandlerFill$$
$$Name2IDs$$
		m_curPacketID = -1;
		m_buff = new ByteArray();
		m_buff.endian = Endian.$$Endian$$;
	}
$$StressFuncs$$
	private function OnReceive(event:ProgressEvent):void
	{
		while(m_sock.bytesAvailable > $$PacketHeaderLen$$)
		{
			if(m_curPacketID < 0)
			{
				m_curPacketID = m_sock.read$$PacketIdType$$();
				if(m_curPacketID > S2C_LastPacketID)
				{
					m_listener.$$LsnrMethodPrefix$$_OnInvalidPacket(m_curPacketID);
					Kick();
					return;
				}

				m_curPacketLen = m_sock.readShort() - $$PacketHeaderLen$$;
				//trace("packet id="+m_curPacketID+", len="+m_curPacketLen);
			}

			if(m_sock.bytesAvailable >= m_curPacketLen)
			{
				m_sock.readBytes(m_buff, 0, m_curPacketLen);

				var recvfn:Function = m_receiverTable[m_curPacketID];
				if(recvfn != null)
				{
					m_buff.position = 0;
					if(!recvfn(m_buff) || !m_sock.connected)
						break;
				}
				else
				{
					trace("Packet #" + m_curPacketID + " receiver is null.");
					m_listener.$$LsnrMethodPrefix$$_OnInvalidPacket(m_curPacketID);
				}

				m_curPacketID = -1;
			}
		}
	}

	public function Connect(addr:String, port:int):void
	{
		m_sock.connect(addr, port);
	}

	public function Kick():void
	{
		if(m_sock.connected)
		{
			m_listener.$$LsnrMethodPrefix$$_OnClose(new Event("Kicked"));
			m_sock.close();
		}
	}

	private function SendPacket(pkid:int, content:ByteArray, len:int):void
	{
		try
		{
			m_sock.write$$PacketIdType$$(pkid);
			m_sock.writeShort($$PacketHeaderLen$$ + len);
			m_sock.writeBytes(content, 0, len);
			m_sock.flush();
		}
		catch(e:IOError)
		{
			m_listener.$$LsnrMethodPrefix$$_OnError(new IOErrorEvent(e.message));
		}
	}

	private function writeWideString(_nsw:ByteArray, str:String, isTiny:Boolean):void
	{
		if(str == null)
		{
			if(isTiny)
				_nsw.writeByte(0);
			else
				_nsw.writeShort(0);
			return;
		}

		var len:int = str.length;

		if(isTiny)
			_nsw.writeByte(len);
		else
			_nsw.writeShort(len);

		for(var i:int = 0; i < len; i++)
		{
			var ch:uint = str.charCodeAt(i);

			if(ch < 0xFF)
			{
				_nsw.writeByte(ch);
				_nsw.writeByte(0);
			}
			else
			{
				_nsw.writeByte(ch & 0x00FF);
				_nsw.writeByte(ch >> 8);
			}
		}

		_nsw.writeShort(0);
	}

	private function writeString(_nsw:ByteArray, str:String, isTiny:Boolean):void
	{
		if(str == null)
		{
			if(isTiny)
				_nsw.writeByte(0);
			else
				_nsw.writeShort(0);
			return;
		}

		if(!isTiny)
		{
			_nsw.writeUTF(str);
			_nsw.writeByte(0);
			return;
		}

		var p1:uint = _nsw.position;
		_nsw.writeByte(0);
		_nsw.writeUTFBytes(str);
		var p2:uint = _nsw.position;
		_nsw.position = p1;
		_nsw.writeByte(p2 - p1 - 1);
		_nsw.position = p2;
		_nsw.writeByte(0);
	}

	private function readWideString(_nsr:ByteArray, isTiny:Boolean):String
	{
		var len:uint;
		var str:String;

		if(isTiny)
			len = _nsr.readUnsignedByte();
		else
			len = _nsr.readUnsignedShort();

		if(len == 0)
		{
			str = null;
		}
		else
		{
			str = "";
			while(len-- > 0)
			{
				var ch:uint = _nsr.readUnsignedShort();
				str += String.fromCharCode(ch);
			}
			_nsr.readShort();
		}

		return str;
	}

	private function readString(_nsr:ByteArray, isTiny:Boolean):String
	{
		var len:uint;
		var str:String;

		if(isTiny)
			len = _nsr.readUnsignedByte();
		else
			len = _nsr.readUnsignedShort();

		if(len == 0)
		{
			str = null;
		}
		else
		{
			str = _nsr.readUTFBytes(len);
			_nsr.readByte();
		}

		return str;
	}

