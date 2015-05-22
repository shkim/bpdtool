"use strict";
var net = require('net');
var util = require('util');

$$Constants$$

$$PacketIDs$$

$$Receivers$$
$$PacketDispatchTableSetup$$

function processReceive(self, recvData)
{
	var buff;
	var buffLen = recvData.length;
	if (self._curRecvPtr > 0)
	{
		recvData.copy(self._recvBuffer, self._curRecvPtr);
		buff = self._recvBuffer;
		buffLen += self._curRecvPtr;
	}
	else
	{
		buff = recvData;
	}
	
	var remainLen;
	var basePtr = 0;
	for(;;)
	{
		var pktId = buff.read$$PacketIdType$$(basePtr);
		if (pktId > $$Recv_LastPacketID$$)
		{
			console.error("packet id range over: %d", pktId);
			self.kick();
			return;
		}
		
		remainLen = buffLen - basePtr;
		if (remainLen <= 3)
			break;
		
		var pktLen = buff.readUInt16$$LEBE$$(basePtr + $$PacketLengthOffset$$);
		if (pktLen <= remainLen)
		{
			var deserialFn = $$PacketDispatchTable$$[pktId];
			if (!deserialFn)
			{
				console.error("Invalid packet id: %d", pktId);
				self.kick();
				return;
			}
			
			deserialFn(self, buff, basePtr + $$PacketHeaderLen$$);
			basePtr += pktLen;
			
			if (basePtr == buffLen)
			{
				// consumed all received buffer
				self._curRecvPtr = 0;
				return;
			}
		}
		else if (pktLen > 2048)
		{
			console.error("Unsupported too long packet length: %d (id=%d)", pktLen, pktId);
			self.kick();
			return;
		}
		else
		{
			break;
		}
	}
	
	buff.copy(self._recvBuffer, 0, basePtr, buffLen);
	self._curRecvPtr = remainLen;
}

var isCheckedHandlers = false;

###IF<client>###
function Instance()
{
	this._socket = new net.Socket();
###ENDIF<client>###
###IF<server>###
function Instance(sock)
{
	this._socket = sock;

$$CheckHandlersExist$$
$$ResetPacketDispatchTable$$
###ENDIF<server>###

	this._recvBuffer = new Buffer(8192);
	this._curRecvPtr = 0;

	var self = this;
		
	this._socket.on('data', function(recvData) {
		processReceive(self, recvData);
	});

	this._socket.on('close', function() {
		if (util.isFunction(self._onDisconnect))
			self._onDisconnect();
	});	
}

function checkHandlerExists(fn, pktName)
{
	if (!util.isFunction(fn))
	{
		throw new Error("bpdtool Error: Handler function for Packet '"+ pktName +"' not found.");
	}
}

###IF<client>###
Instance.prototype.connect = function(port, host, cbOnConnect)
{
$$CheckHandlersExist$$

	this._socket.connect(port, host, function() {
		// socket connected
		if (util.isFunction(cbOnConnect))
			cbOnConnect();
	});	
}
###ENDIF<client>###
###IF<useStage>###
Instance.prototype.setPacketDispatchStage = function(stage)
{
	if (stage >= $$MaxPacketStage$$)
		throw new Error("bpdtool Error: Packet dispatch stage number out of range: " + stage);

	$$PacketDispatchTable$$ = $$PacketDispatchTableStages$$[stage];
}
###ENDIF<useStage>###

Instance.prototype.kick = function()
{
	// user wants disconnect
	this._socket.destroy();
}

Instance.prototype.setOnDisconnect = function(cb)
{
	this._onDisconnect = cb;
}

exports.setPacketHandlers = function(pktCBs)
{
	for (var k in pktCBs)
	{
		Instance.prototype['$$HandlerPrefix$$'+k] = pktCBs[k];
	}
}

function sendPacket(self, pktId, buf, len)
{
	buf.write$$PacketIdType$$(pktId, 0);
	buf.writeInt16$$LEBE$$(len, $$PacketLengthOffset$$);

	if (buf.length == len)
		self._socket.write(buf);
	else
		self._socket.write(buf.slice(0,len));
}

$$Senders$$
exports.Instance = Instance;
