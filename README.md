# bpdtool -- Binary Protocol Designer Tool
##### Download binary executable: [bpdtool.jar](http://shkim.github.io/dist/bpdtool.jar)

### Overview
If you would like to make your own client/server TCP/IP network program in C++,
you may want to send and receive a C struct object as a packet.

Since TCP/IP is the stream protocol, you will soon notice that 
the packet identifier and length are needed in addition to the core struct contents.

In some restricted environment, you can code like this:
```c++
// Shared protocol info file
struct PacketBase
{
  short id;
  short len;
};

struct Action1Packet : PacketBase
{
  int target;
  int param;
};
#define ID_ACTION1  100
```
```c++
// sender
void Socket::SendPacket(short id, PacketBase* p, int len)
{
  p->id = id;
  p->len = len;
  send(hSocket, p, len, 0); 
}

Action1Packet pkt;
pkt.target = 123;
pkt.param = 345;
socket.SendPacket(ID_ACTION1, &pkt, sizeof(pkt));
```
```c++
// receiver
int Server::OnReceive(char *buf, int len)
{
  if (len > sizeof(PacketBase))
  {
    if (len <= ((PacketBase*)buf)->len)
      HandlePacket((PacketBase*)buf);
    ...
    // handle left buf stream
    ...
  }
}

void Server::HandlePacket(PacketBase* pkt)
{
  switch (pkt->id)
  {
  case ID_ACTION1:
    OnAction1((Action1Packet*)buf));
    break;
    ...
  }
}
```

Above code actually works, if the following client/server conditions are met:

1. CPU Endianness is same.
2. C++ Compiler is same.
3. No variable length field or pointer field
 
Condition 1 and 2 are easy to achieve, but 3rd condition is painful: You can't send/receive a string data.

To deal with strings, you should build the packet stream with some special manners: 
ie, copy each fields to the network stream with checking if the field is variable length or not.

That means you can't just copy the struct memory and you should decompose the struct then compose them by hand written code.
Hand written code is not a problem. You may make a mistake in writing the corresponding composer/decompose code such as the field sequence error, datatype mismatching and so on. The problem is that these mistakes are not verified by compilers and can be hard to find.

So the packet composition/decompositon code should be generated automatically; that's the bpdtool's objective!

With bpdtool, you can design a packet like the C struct.
![image](http://shkim.github.io/img/screenshot/bpdtool_intro.png "Screenshot")

Then the tool will generate the packet composition / decomposition code in C++, ActionScript, JavaScript language.

More language support will be added soon.
