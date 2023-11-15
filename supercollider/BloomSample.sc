BloomSample {
	var server;
	var syns;
	var <allNumbers;
	var <allDynamics;
	var <allRoundRobins;

	*new {
		arg argServer;
		^super.new.init(argServer);
	}

	init {
		arg argServer;

		server = argServer;
		allNumbers=Dictionary.new();
		allDynamics=Dictionary.new();
		allRoundRobins=Dictionary.new();
		syns = Dictionary.new();
	}

	loadFolder {
		arg folder;
		var noteDynamics=Dictionary.new();
		var noteRoundRobins=Dictionary.new();
		var noteNumbers=Array.new(128);

		PathName.new(folder).entries.do({ arg v;
			var fileSplit=v.fileName.split($.);
			var note,dyn,dyns,rr,rel;
			if (fileSplit.last=="wav",{
				if (fileSplit.size==6,{
					note=fileSplit[0].asInteger;
					dyn=fileSplit[1].asInteger;
					dyns=fileSplit[2].asInteger;
					rr=fileSplit[3].asInteger;
					rel=fileSplit[4].asInteger;
					if (rel==0,{
						if (noteDynamics.at(note).isNil,{
							noteDynamics.put(note,dyns);
							noteNumbers.add(note);
						});
						if (noteRoundRobins.at(note.asString++"."++dyn.asString).isNil,{
							noteRoundRobins.put(note.asString++"."++dyn.asString,rr);
						},{
							if (rr>noteRoundRobins.at(note.asString++"."++dyn.asString),{
								noteRoundRobins.put(note.asString++"."++dyn.asString,rr);
							});
						});
					});
				});
			});
		});

		noteNumbers=noteNumbers.sort;

		allDynamics.put(folder,noteDynamics);
		allNumbers.put(folder,noteNumbers);
		allRoundRobins.put(folder,noteRoundRobins);

		SynthDef("playx2",{
			arg out=0,pan=0,amp=1.0,
			buf1,buf2,buf1mix=1,
			t_trig=1,rate=1,
			attack=0.01,decay=0.1,sustain=1.0,release=6,gate=1,
			startPos=0;
			var snd,snd2;
			var frames1=BufFrames.ir(buf1);
			var frames2=BufFrames.ir(buf2);
			rate=rate*BufRateScale.ir(buf1);
			snd=PlayBuf.ar(2,buf1,rate,t_trig,startPos:startPos*frames1,doneAction:Select.kr(frames1>frames2,[0,2]));
			snd2=PlayBuf.ar(2,buf2,rate,t_trig,startPos:startPos*frames2,doneAction:Select.kr(frames2>frames1,[0,2]));
			snd=(buf1mix*snd)+((1-buf1mix)*snd2);//SelectX.ar(buf1mix,[snd2,snd]);
			snd=snd*EnvGen.ar(Env.adsr(attack,decay,sustain,release),gate,doneAction:2);
			DetectSilence.ar(snd,0.001,doneAction:2);
			snd=Balance2.ar(snd[0],snd[1],pan,amp);
			Out.ar(out,snd);
		}).send(server);

		SynthDef("playx1",{
			arg out=0,pan=0,amp=1.0,
			buf1,buf2,buf1mix=1,
			t_trig=1,rate=1,
			attack=0.01,decay=0.1,sustain=1.0,release=6,gate=1,
			startPos=0;
			var snd,snd2;
			var frames1=BufFrames.ir(buf1);
			var frames2=BufFrames.ir(buf2);
			rate=rate*BufRateScale.ir(buf1);
			snd=PlayBuf.ar(1,buf1,rate,t_trig,startPos:startPos*frames1,doneAction:Select.kr(frames1>frames2,[0,2]));
			snd2=PlayBuf.ar(1,buf2,rate,t_trig,startPos:startPos*frames2,doneAction:Select.kr(frames2>frames1,[0,2]));
			snd=SelectX.ar(buf1mix,[snd2,snd]);
			snd=snd*EnvGen.ar(Env.adsr(attack,decay,sustain,release),gate,doneAction:2);
			DetectSilence.ar(snd,0.001,doneAction:2);
			snd=Pan2.ar(snd,pan,amp);
			Out.ar(out,snd);
		}).send(server);
	}

	doPlay {
		arg id,note,amp,buf1,buf2,buf1mix,rate,attack=0;
		var notename=1000000.rand;
		var node;
		// [notename,note,amp,buf1,buf2,buf1mix,rate].postln;
		if (syns.at(id).isNil,{
			syns.put(id,Dictionary.new());
		});
		if (syns.at(id).at(note).notNil,{
			if (syns.at(id).at(note).isRunning,{
				syns.at(id).at(note).set(\gate,0);
			});
		});
		node=Synth.head(server,"playx"++buf1.numChannels,[
			\out,0,
			\amp,amp,
			\buf1,buf1,
			\buf2,buf2,
			\buf1mix,buf1mix,
			\rate,rate,
			\attack,attack,
		]).onFree({
			buf1.free;
			buf2.free;
		});
		syns.at(id).put(note,node);
		NodeWatcher.register(node,true);
	}

	playSample {
		arg id, folder, note, velocity, attack=0;
		var noteDynamics = allDynamics.at(folder);
		var noteRoundRobins = allRoundRobins.at(folder);
		var noteNumbers = allNumbers.at(folder);

		var noteOriginal=note;
		var noteLoaded=note;
		var noteClosest=noteNumbers[noteNumbers.indexIn(note)];
		var rate=1.0;
		var rateLoaded=1.0;
		var buf1mix=1.0;
		var amp=1.0;
		var file1,file2,fileLoaded;
		var velIndex=0;
		var velIndices;
		var vels;
		var dyns;
		var noteNumbersLoadedDict=Dictionary.new();

		// first determine the rate to get the right note
		while ({note<noteClosest},{
			note=note+12;
			rate=rate*0.5;
		});

		while ({note-noteClosest>11},{
			note=note-12;
			rate=rate*2;
		});
		rate=rate*Scale.chromatic.ratios[note-noteClosest];

		// determine the number of dynamics
		dyns=noteDynamics.at(noteClosest);
		if (dyns>1,{
			velIndices=Array.fill(dyns,{ arg i;
				i*128/(dyns-1)
			});
			velIndex=velIndices.indexOfGreaterThan(velocity)-1;
		});


		// determine file 1 and 2 interpolation
		file1=noteClosest.asInteger.asString++".";
		file2=noteClosest.asInteger.asString++".";
		if (dyns<2,{
			// simple playback using amp
			amp=velocity/127.0;
			file1=file1++"1.1.";
			file2=file2++"1.1.";
			// add round robin
			file1=file1++(noteRoundRobins.at(noteClosest.asString++".1").rand+1).asString++".0.wav";
			file2=file2++(noteRoundRobins.at(noteClosest.asString++".1").rand+1).asString++".0.wav";
		},{
			var rr1,rr2;
			amp=velocity/127.0/2+0.25;
			// gather the velocity indices that are available
			// TODO: make this specific to a single note?
			vels=[velIndices[velIndex],velIndices[velIndex+1]];
			buf1mix=(1-((velocity-vels[0])/(vels[1]-vels[0])));
			// add dynamic
			file1=file1++(velIndex+1).asInteger.asString++".";
			file2=file2++(velIndex+2).asInteger.asString++".";
			// add dynamic max
			file1=file1++dyns.asString++".";
			file2=file2++dyns.asString++".";
			// add round robin
			rr1=noteRoundRobins.at(noteClosest.asString++"."++(velIndex+1).asString);
			if (rr1.isNil,{
				rr1=1;
			});
			file1=file1++(rr1.rand+1).asString++".0.wav";
			rr2=noteRoundRobins.at(noteClosest.asString++"."++(velIndex+2).asString);
			if (rr2.isNil,{
				rr2=1;
			});
			file2=file2++(rr2.rand+1).asString++".0.wav";
		});

		// [file1,file2,amp,rate].postln;

		Buffer.read(server,PathName(folder+/+file1).fullPath,action:{ arg b1;
			Buffer.read(server,PathName(folder+/+file2).fullPath,action:{ arg b2;
				this.doPlay(id,noteOriginal,amp,b1,b2,buf1mix,rate,attack);
			});
		});

	}

	noteOff {
		arg id, note;
		if (syns.at(id).notNil,{
			if (syns.at(id).at(note).notNil,{
				if (syns.at(id).at(note).isRunning,{
					syns.at(id).at(note).set(\gate,0);
				});
			});
		});
	}

	noteOn {
		arg id, folder, note, velocity,attack=0;


		if (allNumbers.at(folder).isNil,{
			this.loadFolder(folder);
		});

		this.playSample(id, folder, note, velocity, attack);
	}

	free {
		syns.keysValuesDo({ arg name, val;
			val.free;
		});
	}
}