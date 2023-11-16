// before runningmake sure to
// start grid osc utility by running
//
// serialoscd
//
// or similar.
// see TODO for more information

(
Server.default = Server.local;

~m = MonomeGrid.new(); // if we don't want to change rotation, we can leave the argument out
s.waitForBoot({
	var draw, timer, cols = 16, rows = 8;
	var syn;

	~bloom = BloomWildly(Server.default);

	MonomeGrid.setAddCallback({
		arg serial, port, prefix;
		("grid was added: " ++ serial ++ " " ++ port ++ " " ++ prefix).postln;
		if( serial == MonomeGrid.getConnectedDevices[0], {
			cols = ~m.cols;
			rows = ~m.rows;
		});
	});

	~m.connect(0); // if we don't want to connect to a second device, we can leave the argument out
	~step = Array.fill(cols * rows, {0});
	~press = Array.fill(cols * rows, {0});

	// 'cols' + 'rows' return as 1-indexed,
	// but we need 0-indexed for most of our functions!
	~lastCol = cols-1;
	~lastRow = rows-1;

	~m.key({ arg x,y,z;
		var pos = x + (y * 16);
		var idx = 15-x;
		var recorder = 0;
		if (y>3,{
			recorder = 1;
		});
		[x,y].postln;
		~press[pos]=z;
		if (z>0,{
			~step[pos] = z*15;
			~bloom.record(recorder,[idx,x,y]);
			draw.value();
		},{
		});
	});

	draw = {
		for(0,~lastCol, {arg x;
			for(0,~lastRow, {arg y;
				var pos=y*16+x;
				if (~step[pos]>0,{
					if (~press[pos]<1,{
						~step[pos]=~step[pos]-1;
					});
				});
				~m.led(x,y,~step[pos]);
			});
		})
	};

	timer = Routine({
		var interval = 0.25;
		loop {
			draw.value;
			interval.yield;
		}
	});
	timer.play();

	OSCFunc({  arg msg, time, addr, recvPort;
		var x = msg[2];
		var y = msg[3];
		var pos = x + (y * 16);
		var age = msg[4];
		age.postln;
		// [msg, time, addr, recvPort].postln;
		// [x,y,pos].postln;
		~step[pos]=age.linlin(0,120,15,1).round;
		draw.value();
	},"/emit",nil,10111);

});

)
