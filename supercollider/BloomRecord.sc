BloomRecord {
	var server;
	var <tick;
	var <timer;
	var <patterns;
	var <patternRecording;
	var <patternCurrent;
	var ticksBetweenRecordings;
	var delta;
	var isPlaying;
	var ticksBetweenPatterns;
	var tickBetweenPatterns;

	*new {
		arg argServer;
		^super.new.init(argServer);
	}

	clear {
		patternCurrent = 0;
		patterns = Array.new();
		isPlaying = false;
		ticksBetweenPatterns = 1 / delta;
		tickBetweenPatterns = ticksBetweenPatterns;
		ticksBetweenRecordings = 0;
	}

	init {
		arg argServer;
		server = argServer;
		delta = 0.1;
		tick = 0;
		this.clear();
	}

	run {
		if (timer.notNil,{
			timer.stop;
		});
		timer = { inf.do({ 
			delta.wait; 
			tick = tick + 1; 
			if (ticksBetweenRecordings>0,{
				ticksBetweenRecordings = ticksBetweenRecordings - 1;
				if (ticksBetweenRecordings==0,{
					// stop recording
					("[BloomRecord] stop recording pattern"+patterns.size).postln;
					patternRecording.finish();
					patterns = patterns.add(patternRecording);
				});
			});

			if (isPlaying,{
				patterns[patternCurrent].emit(tick,
					{ arg v,timePlayed; [v,timePlayed].postln;},
					{ arg v; 
						("[BloomRecord] finished playing pattern "+patternCurrent).postln; 
						isPlaying = false;
						tickBetweenPatterns = ticksBetweenPatterns;
					}
				);
			},{
				if (tickBetweenPatterns>0,{
					tickBetweenPatterns = tickBetweenPatterns - 1;
					if (tickBetweenPatterns==0,{
						patternCurrent = patternCurrent + 1;
						if (patternCurrent > (patterns.size-1),{
							patternCurrent = 0;
						});
						if (patterns.size>0,{
							("[BloomRecord] playing pattern "+patternCurrent).postln; 
							patterns[patternCurrent].play(tick);
							isPlaying = true;
						},{
							tickBetweenPatterns = ticksBetweenPatterns;
						});
					});
				});
			});
		})}.fork;
	}

	record {
		arg v;
		if (ticksBetweenRecordings==0,{
			ticksBetweenRecordings = 3 / delta;
			patternRecording = BloomPattern();
		});
		patternRecording.record(tick, v);
	}

}