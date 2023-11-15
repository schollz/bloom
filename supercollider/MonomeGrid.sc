/*
v.1.6
for information about monome devices:
monome.org

written by:
raja das, ezra buchla, dan derks

*/

MonomeGrid {

	classvar seroscnet, discovery,
	prefixHandler,
	add, addCallback, addCallbackComplete,
	remove, removeCallback, removeCallbackComplete,
	rows, columns,
	portlst, prefixes, connectedDevices,
	quadDirty, ledQuads, redrawTimers;

	var prefixID, rot, fpsVal, dvcID, keyFunc, oscout; // instance variables

	*initClass {

		addCallback = nil;
		removeCallback = nil;
		portlst = List.new(0);
		connectedDevices = List.new(0);
		addCallbackComplete = List.new(0);
		removeCallbackComplete = List.new(0);
		rows = List.new(0);
		columns = List.new(0);
		prefixes = List.new(0);
		seroscnet = NetAddr.new("localhost", 12002);
		seroscnet.sendMsg("/serialosc/list", "127.0.0.1", NetAddr.localAddr.port);
		seroscnet.sendMsg("/serialosc/notify", "127.0.0.1", NetAddr.localAddr.port);

		quadDirty = Dictionary.new;
		ledQuads = Dictionary.new;
		redrawTimers = Dictionary.new;

		this.buildOSCResponders;

		ServerQuit.add({
			seroscnet.disconnect;
			add.free;
			discovery.free;
			remove.free;
			redrawTimers.do({arg dvc;
				redrawTimers[dvc].stop;
			});
		},\default);

	}

	*new { arg rotation, prefix, fps;
		var rotTranslate = [0,90,180,270];

		rotation = case
		{rotation == nil} {0}
		{rotation <= 3} {rotTranslate[rotation]}
		{rotation > 3} {rotation};

		prefix = case
		{prefix.isNil} {"/monome"}
		{prefix.notNil} {prefix.asString};

		fps = case
		{fps.isNil} {60}
		{fps.notNil} {fps.asFloat};

		^ super.new.init(prefix, rotation, fps);
	}

	*buildOSCResponders {

		add = OSCdef.newMatching(\monomeadd,
			{|msg, time, addr, recvPort|

				var portIDX, sizeDiscover, rw, cl, portID, serial, model;

				serial = msg[1];
				model = msg[2];
				portID = msg[3];

				NetAddr("localhost", msg[3].value).sendMsg("/sys/info", "localhost",NetAddr.localAddr.port);
				sizeDiscover = OSCdef.newMatching(\sizeDiscovery,
					{|msg, time, addr, recvPort|
						cl = msg[1];
						rw = msg[2];
						if( (cl * rw) > 0, { /* grid support only: */
							if( portlst.includes(portID) == false, {
								columns.add(msg[1]);
								rows.add(msg[2]);
								portlst.add(portID);
								prefixes.add("/monome");
								connectedDevices.add(serial);
								addCallbackComplete.add(false);
								removeCallbackComplete.add(false);
							});
							portIDX = portlst.detectIndex({arg item, i; item == portID});
							if( addCallbackComplete[portIDX] == false,{
								("MonomeGrid device added to port: "++portID).postln;
								("MonomeGrid serial: "++serial).postln;
								("MonomeGrid model: "++model).postln;
								addCallback.value(serial,portID,prefixes[portIDX]);
								addCallbackComplete[portIDX] = true;
								removeCallbackComplete[portIDX] = false;
							});
						},{
							"no monome arc support yet".postln;
						};
						seroscnet.sendMsg("/serialosc/notify", "127.0.0.1", NetAddr.localAddr.port);
						sizeDiscover.free;
						);
					}, '/sys/size', NetAddr("localhost", msg[3].value)
				);

		}, '/serialosc/add', seroscnet);

		remove = OSCdef.newMatching(\monomeremove,
			{|msg, time, addr, recvPort|
				var portIDX;

				portIDX = portlst.detectIndex({arg item, i; item == msg[3]});
				if( portIDX.notNil, {
					if( removeCallbackComplete[portIDX] == false, {
						removeCallback.value(msg[2],msg[1],msg[3],prefixes[portIDX]);
						("MonomeGrid device removed from port: "++msg[3]).postln;
						("MonomeGrid serial: "++msg[1]).postln;
						("MonomeGrid model: "++msg[2]).postln;
						addCallbackComplete[portIDX] = false;
						removeCallbackComplete[portIDX] = true;
					});
				});

				seroscnet.sendMsg("/serialosc/notify", "127.0.0.1", NetAddr.localAddr.port);

		}, '/serialosc/remove', seroscnet);

		discovery = OSCdef.newMatching(\monomediscover,
			{|msg, time, addr, recvPort|

				var portIDX, sizeDiscover, rw, cl, portID, serial, model;

				serial = msg[1];
				model = msg[2];
				portID = msg[3];

				NetAddr("localhost", msg[3].value).sendMsg("/sys/info", "localhost",NetAddr.localAddr.port);
				sizeDiscover = OSCdef.newMatching(\sizeDiscovery,
					{|msg, time, addr, recvPort|
						cl = msg[1];
						rw = msg[2];
						if( (cl * rw) > 0, { /* grid support only: */
							if( portlst.includes(portID) == false, {
								portlst.add(portID);
								connectedDevices.add(serial);
								prefixes.add("/monome");
								addCallbackComplete.add(false);
								removeCallbackComplete.add(false);
								("MonomeGrid device connected to port: "++portID).postln;
								("MonomeGrid serial: "++serial).postln;
								("MonomeGrid model: "++model).postln;
								portIDX = portlst.detectIndex({arg item, i; item == portID});
								columns.add(cl);
								rows.add(rw);
								addCallback.value(serial,portID,prefixes[portIDX]);
							},{
								// ("grid already registered!!!").postln;
							});
						},{
							"no monome arc support yet".postln;
						};
						seroscnet.sendMsg("/serialosc/notify", "127.0.0.1", NetAddr.localAddr.port);
						sizeDiscover.free;
						);
					}, '/sys/size', NetAddr("localhost", msg[3].value)
				);

		}, '/serialosc/device', seroscnet);

	}

	*refreshConnections {
		portlst.clear; connectedDevices.clear; prefixes.clear; rows.clear; columns.clear;
		seroscnet.sendMsg("/serialosc/list", "127.0.0.1", NetAddr.localAddr.port);
	}

	*getConnectedDevices {
		^connectedDevices;
	}

	*getPortList {
		^portlst;
	}

	*getPrefixes {
		^prefixes;
	}

	*setAddCallback { arg func;
		addCallback = nil;
		addCallback = func;
	}

	*setRemoveCallback { arg func;
		removeCallback = nil;
		removeCallback = func;
	}

	init { arg prefix_, rot_, fps_;
		prefixID = prefix_;
		rot = rot_;
		fpsVal = fps_;
	}

	connectToPort { arg port;
		if( portlst.includes(port),{
			var idx = portlst.detectIndex({arg item, i; item == port});
			this.connect(idx);
		},{
			("no monome grid connected to specified port").warn;
		});
	}

	connectToSerial { arg serial;
		if( connectedDevices.includes(serial.asSymbol),{
			var idx = connectedDevices.detectIndex({arg item, i; item == serial});
			this.connect(idx);
		},{
			("no monome grid connected with specified serial").warn;
		});
	}

	connect { arg devicenum;
		if( devicenum == nil, {devicenum = 0});
		if( (portlst[devicenum].value).notNil, {

			var prefixDiscover;

			MonomeGrid.buildOSCResponders;

			dvcID = devicenum;
			oscout = NetAddr.new("localhost", portlst[devicenum].value);
			Post << "MonomeGrid: using device on port #" << portlst[devicenum].value << Char.nl;

			oscout.sendMsg(prefixID++"/grid/led/all", 0);

			prefixDiscover.free;
			prefixDiscover = OSCdef.newMatching(\monomeprefix,
				{|msg, time, addr, recvPort|
					prefixes[devicenum] = prefixID;
			}, '/sys/prefix', oscout);

			oscout.sendMsg("/sys/port", NetAddr.localAddr.port);
			oscout.sendMsg("/sys/prefix", prefixID);
			oscout.sendMsg("/sys/rotation", rot);
			oscout.sendMsg("/sys/info");

			// collect individual LED messages into a 'map':
			quadDirty[dvcID] = Array.fill(8,{0});
			ledQuads[dvcID] = Array.fill(8,{Array.fill(64,{0})});

			redrawTimers[dvcID].stop;

			redrawTimers[dvcID] = Routine({
				var interval = 1/fpsVal,
				offsets = [
					[0,0],[8,0],[0,8],[8,8]
				],
				max = case
				{(rows[dvcID] == 8) && (columns[dvcID] == 8)}{0}
				{(rows[dvcID] == 8) && (columns[dvcID] == 16)}{1}
				{(rows[dvcID] == 16) && (columns[dvcID] == 16)}{3};

				loop {
					if( (portlst[devicenum].value).notNil,{
						for (0, max, {
							arg i;
							if(quadDirty[dvcID][i] != 0,
								{
									oscout.sendMsg(
										prefixID++"/grid/led/level/map",
										offsets[i][0],
										offsets[i][1],
										*ledQuads[dvcID][i]
									);
									quadDirty[dvcID][i] = 0;
								}
							);
						});
					});

					interval.yield;
				}

			});

			redrawTimers[dvcID].play();
			addCallbackComplete[dvcID] = false;
			addCallback.value(connectedDevices[dvcID], portlst[dvcID], prefixes[dvcID]);
			seroscnet.sendMsg("/serialosc/notify", "127.0.0.1", NetAddr.localAddr.port);
		},{
			("no monome grid detected at device slot " ++ devicenum).warn;
		});
	}

	usePort { arg portnum;
		dvcID = portlst.indexOf(portnum);
		oscout = NetAddr.new("localhost", portnum);
		Post << "MonomeGrid: using device # " << dvcID << Char.nl;

		oscout.sendMsg("/sys/port", NetAddr.localAddr.port);
		oscout.sendMsg("/sys/prefix", prefixID);
		oscout.sendMsg("/sys/rotation", rot);
	}

	port {
		if( dvcID.notNil, {
			^portlst[dvcID];
		},{
			^nil;
		});
	}

	rows {
		if( dvcID.notNil, {
			^rows[dvcID];
		},{
			^nil;
		});
	}

	cols {
		if( dvcID.notNil, {
			^columns[dvcID];
		},{
			^nil;
		});
	}

	serial {
		if( dvcID.notNil, {
			^connectedDevices[dvcID];
		},{
			^nil;
		});
	}

	prefix {
		if( dvcID.notNil, {
			^prefixes[dvcID];
		},{
			^nil;
		});
	}

	rotation {
		if( dvcID.notNil, {
			^rot
		},{
			^nil;
		});
	}

	fps {
		if( dvcID.notNil, {
			^fpsVal
		},{
			^nil;
		});
	}

	dvcnum {
		^dvcID;
	}


	key { arg func;
		keyFunc = OSCdef.newMatching(
			("keyFunc_" ++ dvcID).asSymbol,
			{ arg message, time, addr, recvPort;
				var x = message[1], y = message[2], z = message[3];
				if( dvcID.notNil,{
					if( this.port.value() == addr.port, {
						func.value(x,y,z);
					});
				});
			},
			prefixID++"/grid/key"
		);
	}

	led { arg x,y,val;
		var offset;
		case
		// 64: quad 0 (top left)
		{(x < 8) && (y < 8)} {
			offset = (8*y)+x;
			ledQuads[dvcID][0][offset] = val;
			quadDirty[dvcID][0] = 1;
		}
		// 128: quad 1 (top right)
		{(x > 7) && (x < 16) && (y < 8)} {
			offset = (8*y)+(x-8);
			ledQuads[dvcID][1][offset] = val;
			quadDirty[dvcID][1] = 1;
		}
		// 256: quad 2 (bottom left)
		{(x < 8) && (y > 7) && (y < 16)} {
			offset = (8*(y-8))+x;
			ledQuads[dvcID][2][offset] = val;
			quadDirty[dvcID][2] = 1;
		}
		// 256: quad 3 (bottom right)
		{(x > 7) && (x < 16) && (y > 7) && (y < 16)} {
			offset = (8*(y-8))+(x-8);
			ledQuads[dvcID][3][offset] = val;
			quadDirty[dvcID][3] = 1;
		}
	}

	all { arg val;
		oscout.sendMsg(prefixID++"/grid/led/level/all", val);
	}

	// See here: http://monome.org/docs/tech:osc
	// if you need further explanation of the LED methods below
	ledset	{ arg x, y, state;
		if ((state == 0) or: (state == 1)) {
			oscout.sendMsg(prefixID++"/grid/led/set", x, y, state);
		} {
			"invalid argument (state must be 0 or 1).".warn;
		};
	}

	intensity	{ arg val;
		oscout.sendMsg(prefixID++"/grid/led/intensity", val);
	}

	tilt_enable { arg device, state;
		oscout.sendMsg(prefixID++"/tilt/set", device, state);
	}

	cleanup {
		this.all(0);
		keyFunc.free;
		oscout.disconnect;
	}

}