BloomWildly {
	var server;
	var syns;
	var buses;
	var bloomSample;
	var bloomSampleFolder;
	var bloomRecorders;
	var numRecorders;
	var timer;
	var timerDrone;
	var delta;
	var ticksBetweenChords;
	var tickBetweenChords;
	var tickBetweenChordsDrone;
	var chordNum;
	var chords;
	var noteRoot;
	var tick;
	var scale;
	var patternDeath;
	var scales;
	var droneVolume;
	var oscs;

	*new {
		arg argServer,argbloomSampleFolder;
		^super.new.init(argServer,argbloomSampleFolder);
	}

	fnEmit {
		arg recorder, pattern, v, age, patternI, patternN;
		var note;
		var idx = v[0]*90;
		var release = 1;
		var noiserelease;
		idx = idx + (v[1]*10);
		idx = idx.linlin(0,100,0,scale.size-2).round.asInteger;
		note = scale[idx.mod(scale.size-2)+2];
		note = note + noteRoot - 12;
		if (patternI+1==patternN,{
			release = 4;
		});
		noiserelease = release;
		release = release * age.linlin(0,patternDeath,1,0.5);
		("[BloomWildly] emit"+v+"age"+age+"pattern"+pattern+patternI+patternN+note).postln;
		bloomSample.noteOn(0,
			bloomSampleFolder,
			note,
			12.neg.dbamp,
			age.linlin(0,patternDeath,127,1),
			0,
			NetAddr("127.0.0.1", 10111).sendMsg("/note_on_norns",note,age.linlin(0,patternDeath,127,1).round.asInteger);
			buses.at("acoustic").index,{
				Synth.head(server,"bell",[
					\out, buses.at("synthetic"),
					\freq,(note).midicps,
					\amp,(age.linlin(0,patternDeath,-12,-36).dbamp),
					\release,release,
					\noiserelease,noiserelease
				]).onFree({
					NetAddr("127.0.0.1", 10111).sendMsg("/note_off_norns",note);
				});

		});
		v = v.add(age);
		NetAddr("127.0.0.1", 10111).sendMsg("/emit",*v);
		if (age>patternDeath,{
			bloomRecorders[recorder].remove(pattern);
		});
	}

	init {
		arg argServer,argbloomSampleFolder;
		var starting = 10000;
		server = argServer;
		bloomSampleFolder=argbloomSampleFolder;
		droneVolume = 3.neg.dbamp;

		// initialize globals
		numRecorders = 8;
		patternDeath = 60;
		delta = 0.1;
		ticksBetweenChords = 12 / delta; // 4 seconds
		tickBetweenChords = 1;
		tickBetweenChordsDrone = 1;
		chordNum = 0;
		chords = [
			[0,0,4,7],
			[-3,0,4,9],
			[7.neg,7.neg,3.neg,0],
			[-3,0,4,9],
		];
		noteRoot = 0;
		tick = 0;
		bloomSample = BloomSample(server);
		bloomRecorders = Array.newClear(numRecorders);
		numRecorders.do({ arg i;
			bloomRecorders[i] = BloomRecord(server, { arg pattern, v, age, patternI, patternN;
				this.fnEmit(i, pattern, v, age, patternI, patternN);
			});
		});

		// define synths
		SynthDef("bass", { arg freq = 440, amp = 0.5, gate = 1, lfoIn=0;
			var snd, env, oscfreq, output;
			var lfo;
			oscfreq = {freq * LFNoise2.kr(Rand(0.0001,0.5)).range(0.98, 1.02)}!10;
			lfo = { SinOsc.kr({ 1/Rand(2,52) }!10) };
			env = Env.adsr(8, 1, 0.9,4).kr(doneAction:2, gate: gate);
			output = LFSaw.ar(oscfreq, mul: lfo.value.range(0,1));
			output = Fold.ar(output,-0.5,0.5);
			output = RLPF.ar(output, (env*freq*0.7) + (freq * lfo.value.range(0.1,3)), lfo.value.range(0.2,1));
			output = Splay.ar(output, lfo.value.range(0,1));
			output = output * env * Lag.kr(amp,2);
			Out.ar(0, output * 24.neg.dbamp);
		}).send(server);

		SynthDef("sine",{ arg bus=0,freq=10;
			Out.kr(bus,SinOsc.kr(freq));
		}).send(server);

		SynthDef("lfnoise2",{ arg bus=0,freq=10;
			Out.kr(bus,LFNoise2.kr(freq));
		}).send(server);


		SynthDef("blender",{ arg busA=0,busB=2,blend=0.2,shimmer=1;
			var snd, snd2;
			var sndA = In.ar(busA,2);
			var sndB = In.ar(busB,2);
			snd = SelectX.ar(VarLag.kr(blend,0.3,warp:\sine),[sndA,sndB]);
			snd2=snd;
			snd2 = DelayN.ar(snd, 0.03, 0.03);
			snd2 = snd2 + PitchShift.ar(snd, 0.13, 2,0,1,1*shimmer/3);
			snd2 = snd2 + PitchShift.ar(snd, 0.1, 4,0,1,0.5*shimmer/3);
			snd2 = snd2 + PitchShift.ar(snd, 0.1, 8,0,1,0.125*shimmer/2);
			snd = SelectX.ar(0.8,[snd2,snd]);
			Out.ar(0,snd);
		}).send(server);

		SynthDef.new("bell",	{
			arg out=0, freq=440, rate=0.6, pan=0.0, amp=1.0, dur=1.0, lfor1=0.08, lfor2=0.05, nl=0.5, filt=5000, release=1, noiserelease=1;
			var sig, sub, lfo1, lfo2, env, noiseenv, noise;

			lfo1  = SinOsc.kr(lfor1, 0.5, 1, 0);
			lfo2  = SinOsc.kr(lfor2, 0, 1, 0);
			sig   = SinOscFB.ar(freq, lfo1, 1, 0);
			env = EnvGen.ar(Env.perc(0.005,rrand(2,4)*release),doneAction:2);
			noiseenv = EnvGen.ar(Env.perc(0.005,rrand(2,4)*noiserelease),doneAction:2);
			noise = PinkNoise.ar(nl, 0) * noiseenv;
			sig   = (sig * env) +  noise;
			sig   = MoogFF.ar(sig, 5000, 0, 0, 1, 0);
			sig   = Pan2.ar(sig, pan, amp);
			Out.ar(out, sig * 6.neg.dbamp);
		}).send(server);

		SynthDef("pad",{ arg freq=440, amp = 0.5, gate = 1, modBus = 0;
			var snd;
			snd = SinOscFB.ar(freq,In.kr(modBus,1).range(0,0.5));
			snd = snd * EnvGen.ar(Env.adsr(8,1,0.5,4),gate,doneAction:2);
			DetectSilence.ar(snd,doneAction:2);
			snd = LPF.ar(snd,400);
			snd = Pan2.ar(snd,SinOsc.kr(1/Rand(2,5),mul:0.5));
			Out.ar(0, snd * Lag.kr(amp,1) * 20.neg.dbamp);
		}).send(server);

		SynthDef("final",{
			var snd,snd2;
			snd = In.ar(0,2);

			snd = SelectX.ar(LFNoise2.kr(1/5).range(0.2,0.7),[snd,
				Fverb.ar(snd[0],snd[1],200,
					input_lowpass_cutoff: LFNoise2.kr(1/3).range(5000,10000),
					tail_density: LFNoise2.kr(1/3).range(70,90),
					decay: LFNoise2.kr(1/3).range(70,90),
				)
			]);


			snd = snd * EnvGen.ar(Env.adsr(3,1,1,1));
			snd = HPF.ar(snd,30);
			snd = LPF.ar(snd,12000);
			ReplaceOut.ar(0,snd * Lag.kr(\db.kr(0),3).dbamp);
		}).send(server);

		// initialize scales
		scales = Dictionary.new();
		// first two notes of the scale are drone notes
		scales.put("ambrette",[50, 54, 55, 62, 67, 69, 70, 72, 74, 76, 77, 79, 81, 82, 84, 86, 88, 89, 91, 93, 94, 96, 98]);
		scales.put("benzoin",[43, 47, 48, 55, 60, 62, 64, 66, 67, 69, 71, 72, 74, 76, 78, 79, 81, 83, 84, 86, 88, 90, 91, 93, 95]);
		scales.put("bergamot",[47, 45, 50, 57, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84, 86, 88, 89, 91, 93, 95, 96]);
		scales.put("labdanum",[52, 54, 55, 57, 64, 69, 71, 72, 74, 76, 78, 79, 81, 83, 84, 86, 88, 90, 91, 93, 95, 96, 98, 100, 102]);
		scales.put("neroli",[50, 54, 55, 62, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84, 86, 88, 89, 91, 93, 95, 96, 98]);
		scales.put("orris",[48, 50, 53, 60, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84, 86, 88, 89, 91, 93, 95, 96, 98]);
		scales.put("tolu",[54, 53, 58, 65, 70, 72, 74, 76, 77, 79, 81, 82, 84, 86, 88, 89, 91, 93, 94, 96, 98, 100]);
		scales.put("vetiver",[43, 46, 47, 48, 55, 60, 62, 64, 65, 67, 69, 70, 72, 74, 76, 77, 79, 81, 82, 84, 86, 88, 89, 91, 93, 94, 96]);
		scales.put("ylang",[45, 47, 46, 50, 57, 62, 64, 66, 67, 69, 71, 72, 74, 78, 79, 81, 83, 84, 86, 90, 91, 93, 95, 96]);

		scale = scales.at("orris");
		scale.postln;


		// initialize dictionaries
		syns = Dictionary.new();
		buses = Dictionary.new();

		// initialize buses
		// modulation buses
		8.do({ arg i;
			buses.put("mod"++i,Bus.control(server,1));
		});

		buses.put("synthetic",Bus.audio(server,2));
		buses.put("acoustic",Bus.audio(server,2));

		// sync server
		server.sync;

		// intialize final effects
		syns.put("final",Synth.tail(server,"final",[]));

		syns.put("blender",Synth.before(syns.at("final"),"blender",[
			busA: buses.at("synthetic"),
			busB: buses.at("acoustic"),
			blend: 0.5,
		]));

		// intialize modulation
		4.do({ arg i ;
			// TODO: add option to change type of modulation?
			syns.put("mod"++i,Synth.head(server,"sine",[
				bus: buses.at("mod"++i),
				freq: 1/(3+(rrand(0,300000)/100000)),
			]));
		});

		// starts the pattern recorderplayer
		numRecorders.do({ arg i;
			bloomRecorders[i].run();
		});

		Routine{
			1.wait;
			server.sync;
			syns.put("bass",Synth.after(syns.at("mod0"),"bass",[
				freq: (60-12).midicps,
				amp: droneVolume.dbamp,
			]));
			NodeWatcher.register(syns.at("bass"));
		}.play;

		// starts the drone
		if (timerDrone.notNil,{
			timerDrone.stop;
		});
		timerDrone = { inf.do({
			delta.wait;
			tick = tick + 1;
			if (tickBetweenChordsDrone>0,{
				tickBetweenChordsDrone = tickBetweenChordsDrone - 1;
				if (tickBetweenChordsDrone==0) {
					if (10.rand<starting,{
						var note = [scale[0],scale[1]].choose + noteRoot;
						tickBetweenChordsDrone = ticksBetweenChords;
						("[BloomWildly] playing bass").postln;
						syns.at("bass").set(\freq,(note-12).midicps);
					});
					if (10.rand<starting, {
						// stop old pad
						("[BloomWildly] playing drone").postln;
						if (syns.at("drone").notNil,{
							syns.at("drone").set(\gate,0);
						});
						syns.put("drone",Synth.after(syns.at("mod0"),"pad",[
							modBus: buses.at("mod0"),
							freq: (scale.choose.mod(12)+noteRoot+48).midicps,
							amp: droneVolume.dbamp,
						]));
						NodeWatcher.register(syns.at("drone"));
					});
					if (starting>8,{
						starting = 8;
					})
				}
			});
		})}.fork;


		/*		// starts the pad
		if (timer.notNil,{
		timer.stop;
		});
		timer = { inf.do({
		delta.wait;
		tick = tick + 1;
		if (tickBetweenChords>0,{
		tickBetweenChords = tickBetweenChords - 1;
		if (tickBetweenChords==0) {
		var chord;
		var drone = [scale[0],scale[1]].choose;
		tickBetweenChords = ticksBetweenChords;
		// next chord
		chordNum = chordNum + 1;
		chord = chords[chordNum.mod(chords.size)];
		4.do({ arg i ;
		("[BloomWildly] playing chord"+chord).postln;
		if (i==0,{
		if (syns.at("bass").notNil,{
		syns.at("bass").set(\gate,0);
		});
		syns.put("bass",Synth.after(syns.at("mod"++i),"pad",[
		modBus: buses.at("mod"++i),
		freq: (chord[i]+drone+scale[0]-24).midicps,
		]));
		NodeWatcher.register(syns.at("bass"));
		},{
		// stop old pad
		if (syns.at("pad"++i).notNil,{
		syns.at("pad"++i).set(\gate,0);
		});
		syns.put("pad"++i,Synth.after(syns.at("mod"++i),"pad",[
		modBus: buses.at("mod"++i),
		freq: (chord[i]+scale[0]+noteRoot).midicps,
		amp: 6.neg.dbamp,
		]));
		NodeWatcher.register(syns.at("pad"++i));
		});
		});
		}
		});
		})}.fork;*/

		"[BloomWildly] ready".postln;
	}

	setDroneVolume {
		arg v;
		droneVolume = v;
		if (syns.at("drone").notNil,{
			syns.at("drone").set(\amp,droneVolume.dbamp);
		});
		if (syns.at("bass").notNil,{
			syns.at("bass").set(\amp,droneVolume.dbamp);
		});
	}

	setSecondsBetweenRecordings {
		arg v;
		numRecorders.do({arg i;
			bloomRecorders[i].setSecondsBetweenRecordings(v);
		});
	}

	setSecondsBetweenPatterns {
		arg v;
		numRecorders.do({arg i;
			bloomRecorders[i].setSecondsBetweenPatterns(v);
		});
	}

	setPatternDuration {
		arg v;
		patternDeath = v;
	}

	removeAll {
		numRecorders.do({arg i;
			bloomRecorders[i].removeAll();
		});
	}

	setScale {
		arg v;
		if (scales.at(v).notNil,{
			("[BloomWildly] setting scale to "+v).postln;
			scale = scales.at(v);
		});
	}

	setBlend {
		arg v;
		syns.at("blender").set(\blend,v);
	}

	setShimmer {
		arg v;
		syns.at("blender").set(\shimmer,2*v);
	}

	setRoot {
		arg v;
		noteRoot = v;
	}

	record {
		arg i,v;
		if (i<numRecorders,{
			bloomRecorders[i].record(v, { arg pattern, v, age, patternI, patternN;
				this.fnEmit(i, pattern, v, age, patternI, patternN);
			});
		});
	}

	remove {
		arg i, v;
		bloomRecorders[i].remove(v);
	}

	free {
		"[bloom] free".postln;
		syns.keysValuesDo({ arg name, val;
			val.free;
		});
		buses.keysValuesDo({ arg name, val;
			val.free;
		});
		timer.stop;
		timerDrone.stop;
		bloomSample.free;
		numRecorders.do({arg i;
			bloomRecorders[i].free;
		});
	}
}