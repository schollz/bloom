BloomWildly {
	var server;
	var syns;
	var buses;
	var bloomSample;
	var bloomRecorders;
	var numRecorders;
	var timer;
	var delta;
	var ticksBetweenChords;
	var tickBetweenChords;
	var chordNum;
	var chords;
	var noteRoot;
	var tick;
	var scale;
	var patternDeath;

	*new {
		arg argServer;
		^super.new.init(argServer);
	}

	fnEmit {
		arg recorder, pattern, v, age;
		var note;
		note = scale[v[0].mod(16)]+noteRoot;
		("[BloomWildly] emit"+v+"age"+age+"pattern"+pattern+"note"+note).postln;
		Synth.head(server,"bell",[\freq,note.midicps,\amp,(age.linlin(0,patternDeath,0,-12).dbamp)]);
		v = v.add(age);
		NetAddr("127.0.0.1", 10111).sendMsg("/emit",*v);
		// TODO
		// emit a note based on current sample with
		// amplitidue defined by the age of the pattern
		// TODO create a parameter for the pattern age limit
		if (age>patternDeath,{
			bloomRecorders[recorder].remove(pattern);
		});
	}

	init {
		arg argServer;

		server = argServer;

		// initialize globals
		numRecorders = 2;
		patternDeath = 60;
		delta = 0.1;
		ticksBetweenChords = 12 / delta; // 4 seconds
		tickBetweenChords = 1;
		chordNum = 0;
		chords = [
			[0,0,4,7],
			[-3,0,4,9],
			[7.neg,7.neg,3.neg,0],
			[-3,0,4,9],
		];
		noteRoot = 60;
		tick = 0;
		scale = Scale.major.degrees++(12+Scale.major.degrees)++(24+Scale.major.degrees);
		bloomSample = BloomSample(server);
		bloomRecorders = Array.newClear(numRecorders);
		numRecorders.do({ arg i;
			bloomRecorders[i] = BloomRecord(server, { arg pattern, v, age;
				this.fnEmit(i, pattern, v, age);
			});
		});

		// define synths
		SynthDef("bass", { arg freq = 440, amp = 0.5, gate = 1;
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

		SynthDef.new("bell",	{
			arg freq=440, rate=0.6, pan=0.0, amp=1.0, dur=1.0, lfor1=0.08, lfor2=0.05, nl=0.3, filt=5000;
			var sig, sub, lfo1, lfo2, env, noise;

			lfo1  = SinOsc.kr(lfor1, 0.5, 1, 0);
			lfo2  = SinOsc.kr(lfor2, 0, 1, 0);
			sig   = SinOscFB.ar(freq, lfo1, 1, 0);
			env = EnvGen.ar(Env.perc(0.005,rrand(2,4)),doneAction:2);
			noise = PinkNoise.ar(nl, 0);
			sig   = (sig +  noise) * env;
			sig   = MoogFF.ar(sig, 5000, 0, 0, 1, 0);
			sig   = Pan2.ar(sig, pan, amp);
			Out.ar(0, sig * 0.2);
		}).send(server);

		SynthDef("pad",{ arg freq=440, amp = 0.5, gate = 1, modBus = 0;
			var snd;
			snd = SinOscFB.ar(freq,In.kr(modBus,1).range(0,0.5));
			snd = snd * EnvGen.ar(Env.adsr(8,1,0.5,4),gate,doneAction:2);
			DetectSilence.ar(snd,doneAction:2);
			snd = LPF.ar(snd,400);
			snd = Pan2.ar(snd,SinOsc.kr(1/Rand(2,5),mul:0.5));
			Out.ar(0, snd * amp * 12.neg.dbamp);
		}).send(server);

		SynthDef("final",{
			var snd;
			snd = In.ar(0,2);

			snd = SelectX.ar(LFNoise2.kr(1/5).range(0.3,0.7),[snd,
				Fverb.ar(snd[0],snd[1],200,
					input_lowpass_cutoff: LFNoise2.kr(1/3).range(5000,10000),
					tail_density: LFNoise2.kr(1/3).range(70,90),
					decay: LFNoise2.kr(1/3).range(70,90),
				)
			]);

			snd = snd * EnvGen.ar(Env.adsr(3,1,1,1));
			snd = HPF.ar(snd,60);
			snd = LPF.ar(snd,12000);
			ReplaceOut.ar(0,snd * Lag.kr(\db.kr(0),30).dbamp);
		}).send(server);

		// initialize dictionaries
		syns = Dictionary.new();
		buses = Dictionary.new();

		// initialize buses
		// modulation buses
		4.do({ arg i;
			buses.put("mod"++i,Bus.control(server,1));
		});

		// sync server
		server.sync;

		// intialize final effects
		syns.put("final",Synth.tail(server,"final",[]));

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
					4.do({ arg i ;
						("[BloomWildly] playing chord"+chord).postln;
						if (i==0,{
							if (syns.at("bass").notNil,{
								syns.at("bass").set(\gate,0);
							});
							syns.put("bass",Synth.after(syns.at("mod"++i),"pad",[
								modBus: buses.at("mod"++i),
								freq: (chord[i]+noteRoot-24).midicps,
							]));
							NodeWatcher.register(syns.at("bass"));
						},{
							// stop old pad
							if (syns.at("pad"++i).notNil,{
								syns.at("pad"++i).set(\gate,0);
							});
							syns.put("pad"++i,Synth.after(syns.at("mod"++i),"pad",[
								modBus: buses.at("mod"++i),
								freq: (chord[i]+noteRoot).midicps,
								amp: 6.neg.dbamp,
							]));
							NodeWatcher.register(syns.at("pad"++i));
						});
					});

				}
			});
		})}.fork;

		"[BloomWildly] ready".postln;
	}

	record {
		arg i,v;
		var note = scale[v.mod(16)]+noteRoot;
		Synth.head(server,"bell",[\freq,note.midicps,\amp,0.4]);
		bloomRecorders[i].record(v, { arg pattern, v, age;
			this.fnEmit(i, pattern, v, age);
		});
	}

	remove {
		arg i, v;
		bloomRecorders[i].remove(v);
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
		numRecorders.do({arg i;
			bloomRecorders[i].free;
		});
	}
}