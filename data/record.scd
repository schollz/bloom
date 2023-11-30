(
s.waitForBoot({
	f = File("~/Documents/bloom/data/labdanum.txt".standardizePath,"w");
	SynthDef("pitch",{
		var freq,hasFreq;
		var snd = Mix.new(SoundIn.ar([0,1]));
		# freq, hasFreq = Pitch.kr(snd, ampThreshold: 0.02, median: 7);
		SendReply.kr(Impulse.kr(10),"/note",freq.cpsmidi);
		Out.ar(0,snd);
	}).play;

	OSCdef("note", {
		|msg, time, addr, recvPort|
		var note = msg[3];
		note.postln;
		f.write(note.asString);
		f.write("\n");
	}, '/note');
})
)

f.close;

[50, 55, 62, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84, 86, 88, 89, 91, 93, 95, 96, 98].choose


(
var server = s;
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
)

Synth.new("pad",[\freq,50.midicps]);
Synth.new("bell",[\freq,[55,55,55,55,55, 62, 62, 62, 62, 62, 62, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84, 86, 88, 89, 91, 93, 95, 96, 98].midicps.choose,\amp,0.2]);

