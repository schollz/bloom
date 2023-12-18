// 1. before running make sure to
// copy all the lib/Bloom* files into `~/.local/share/SuperCollider/Extensions/`
//
// 2. start grid osc utility by running
//
// serialoscd
//

(
Server.default = Server.local;

~m = MonomeGrid.new(); // if we don't want to change rotation, we can leave the argument out
s.waitForBoot({
	var draw, timer, cols = 16, rows = 8;
	var syn;

	~bloom = BloomWildly(Server.default,thisProcess.nowExecutingPath.dirname++"/../samples/kalimba_small");
	~bloom.setBlend(0.05);

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
		var pos = x + (y * cols);
		var idx = (cols-1)-x;
		var recorder = 0;
		if (y>3,{
			recorder = 1;
		});
		[x,y].postln;
		~press[pos]=z;
		if (z>0,{
			~step[pos] = z*15;
			~bloom.record(recorder,[x/cols,y/rows]);
			draw.value();
		},{
		});
	});

	draw = {
		for(0,~lastCol, {arg x;
			for(0,~lastRow, {arg y;
				var pos=y*cols+x;
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
		var x = msg[1]*cols;
		var y = msg[2]*rows;
		var pos = x + (y * cols);
		var age = msg[3];
		age.postln;
		// [msg, time, addr, recvPort].postln;
		// [x,y,pos].postln;
		~step[pos]=age.linlin(0,120,15,1).round;
		draw.value();
	},"/emit",nil,10111);

});

)


~bloom.setBlend(1)