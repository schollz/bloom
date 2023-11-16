(
s.waitForBoot({
	x = BloomWildly(Server.default);
	OSCFunc({  arg msg, time, addr, recvPort;
		[msg, time, addr, recvPort].postln;
	},"/emit",nil,10111);
	Routine {
		1.wait;
		x.record(0,[0,7,13]);
		3.wait;
	}.play;
})
)

thisProcess.openUDPPort(7771);
n = NetAddr.new("127.0.0.1", 10111);    // create the NetAddr
// create the OSCFunc
o = OSCFunc.new({ arg msg, time, addr, recvPort; [msg, time, addr, recvPort].postln; }, '/goodbye', nil,7771);

n.sendMsg("/emit",[1,2],);
thisProcess.openPorts;
OSCFunc.trace(false);
o.free;    // remove the OSCFunc when you are done.
Date.getDate.rawSeconds



10.mod(3)
x = Dictionary.new();
x.put(0,0);
x.put(1,0);
x.put(4,0);
x.remove(0)
delete(x,0)
x.put(0,nil);
x
x.keys().asArray.sort
List()

x = [1,2,3]
x.removeAt(1)
x
x.indexOf(1)
(
s.waitForBoot({
	x = BloomRecord(Server.default);
	x.run;
	Routine {
		0.1.wait;
		x.record(0);
		0.2.wait;
		x.record(1);
		0.3.wait;
		x.record(2);
		0.4.wait;
		x.record(3);
		0.5.wait;
		x.record(4);
		"done".postln;
	}.play;
});
)

3+(rrand(0,300000)/100000)
x.clear;
(
Routine {
	0.5.wait;
	x.record(5);
	0.4.wait;
	x.record(6);
	0.6.wait;
	x.record(7);
	0.6.wait;
	x.record(8);
	0.7.wait;
	x.record(9);
	"done".postln;
}.play;
)

x.patterns

x.patterns[0].arrTime

(
s.waitForBoot({
	y= 0;
	x = { inf.do({ arg i; y = y + 1; 0.05.wait;}) }.fork;
	Routine {
		p=BloomPattern();
		0.1.wait;
		p.record(y,0);
		0.2.wait;
		p.record(y,1);
		0.3.wait;
		p.record(y,2);
		0.4.wait;
		p.record(y,3);
		0.5.wait;
		p.record(y,4);
		p.finish();
		"done".postln;
	}.play;
});
)

(
Routine {
	p.play(y);
	inf.do({
		p.play(y,{ arg v,timePlayed; [v,timePlayed].postln;},{ arg v; "finished".postln;});
		0.05.wait;
	});
}.play;
)


p.play(y);
p.arrValue

x=BloomPattern()

x.counter

x
x.free;
x=Array.new();
x.add(3)
x=x.add(4)
x

x.do({ arg v, i;
	[i,v].postln;
	x[i] = v-x[0];


	time
})

a=1
a++
true