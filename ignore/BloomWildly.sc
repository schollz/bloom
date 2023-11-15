BloomWildly {
	var server;
	var syns;
	var buses;
	var bloomSample;
	var bloomRecorder;
	var timer;
	var delta;
	var ticksBetweenChords;
	var tickBetweenChords;
	var chordNum;
	var chords;
	var noteRoot;
	var tick;

	*new {
		arg argServer;
		^super.new.init(argServer);
	}

	init {
		arg argServer;

		server = argServer;

		// initialize globals
		bloomSample = BloomSample(server);
		bloomRecorder = BloomRecord(server);
		delta = 0.1;
		ticksBetweenChords = 4 / delta; // 4 seconds
		tickBetweenChords = 1;
		chordNum = 0;
		chords = [
			[0,4,7],
			[0,4,9],
			[7.neg,3.neg,0],
			[0,4,9],
		];
		noteRoot = 60;
		tick = 0;

		// define synths
		SynthDef(\bass, { arg freq = 440, amp = 0.5, gate = 1;
			var snd, env, oscfreq, output;
			var lfo;
			oscfreq = {freq * LFNoise2.kr(Rand(0.0001,0.5)).range(0.98, 1.02)}!10;
			lfo = { SinOsc.kr({ 1/Rand(2,52) }!10) };
			env = Env.adsr(0.2, 1, 0.9,0.1).kr(doneAction:2, gate: gate);
			output = LFSaw.ar(oscfreq, mul: lfo.value.range(0,1));
			output = Fold.ar(output,-0.5,0.5);
			output = RLPF.ar(output, (env*freq*0.7) + (freq * lfo.value.range(0.1,2)), lfo.value.range(0.2,1));
			output = Splay.ar(output, lfo.value.range(0,1));
			output = output * env * amp;
			Out.ar(0, output * 18.neg.dbamp);
		}).send(server);

		SynthDef("sine",{ arg bus=0,freq=10;
			Out.kr(bus,SinOsc.kr(freq));
		}).send(server);

		SynthDef("lfnoise2",{ arg bus=0,freq=10;
			Out.kr(bus,LFNoise2.kr(freq));
		}).send(server);

		SynthDef("pad",{ arg freq=440, amp = 0.5, gate = 1, modBus = 0;
			var snd;
			snd = SinOscFB.ar(freq,In.kr(modBus,1).range(0,0.5));
			snd = snd * EnvGen.ar(Env.adsr(8,1,0.5,4),gate,doneAction:2);
			DetectSilence.ar(snd,doneAction:2);
			Out.ar(0, snd * amp * 12.neg.dbamp);
		}).send(server);

		SynthDef("final",{
			var snd;
			snd = In.ar(0,2);

			snd = SelectX.ar(LFNoise2.kr(1/5).range(0.1,0.55),[snd,
				Fverb.ar(snd[0],snd[1],50,
					tail_density: LFNoise2.kr(1/3).range(50,90),
					decay: LFNoise2.kr(1/3).range(50,90),
				)
			]);

			snd = snd * EnvGen.ar(Env.adsr(3,1,1,1));
			snd = HPF.ar(snd,100);
			snd = LPF.ar(snd,12000);
			ReplaceOut.ar(0,snd * Lag.kr(\db.kr(0),30).dbamp);
		}).send(server);

		// initialize dictionaries
		syns = Dictionary.new();
		buses = Dictionary.new();

		// initialize buses
		// modulation buses
		3.do({ arg i;
			buses.put("mod"++i,Bus.control(server,1));
		});

		// sync server
		server.sync;

		// intialize final effects
		syns.put("final",Synth.tail(server,"final",[]));

		// intialize modulation
		3.do({ arg i ;
			// TODO: add option to change type of modulation?
			syns.put("mod"++i,Synth.head(server,"sine",[
				bus: buses.at("mod"++i),
				freq: 1/(3+(rrand(0,300000)/100000)),
			]));
		});

		// starts the pattern recorderplayer
		bloomRecorder.run({
			arg pattern, v, age;
			("[BloomWildly] emit"+v+"age"+age+"pattern"+pattern).postln;
			// TODO
			// emit a note based on current sample with
			// amplitidue defined by the age of the pattern
			// TODO create a parameter for the pattern age limit
			// if (age>50,{
			// 	bloomRecorder.remove(pattern);
			// });
		});

		// starts the pad
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
					tickBetweenChords = ticksBetweenChords;
					// next chord
					chordNum = chordNum + 1;
					chord = chords[chordNum.mod(chords.size)];
					3.do({ arg i ;
						("[BloomWildly] playing chord"+chord).postln;
						// stop old pad
						// if (syns.at("pad"++i).notNil,{
						// 	syns.at("pad"++i).set(\gate,0);
						// });
						// syns.put("pad"++i,Synth.after(syns.at("mod"++i),"pad",[
						// 	modBus: buses.at("mod"++i),
						// 	freq: (chord[i]+noteRoot).midicps,
						// ]));
						// NodeWatcher.register(syns.add("pad"++i));
					});

				}
			});
		})}.fork;

		"[BloomWildly] ready".postln;
	}

	record {
		arg v;
		bloomRecorder.record(v);
	}

	remove {
		arg v;
		bloomRecorder.remove(v);
	}

	free {
		syns.keysValuesDo({ arg name, val;
			val.free;
		});
		buses.keysValuesDo({ arg name, val;
			val.free;
		});
		timer.stop;
		bloomSample.free;
		bloomRecorder.free;
	}
}