BloomRecord {
	var server;
	var <tick;
	var <timer;
	var <patternRecording;
	var <patternCurrent;
	var patternIterator;
	var ticksBetweenRecordings;
	var delta;
	var isPlaying;
	var ticksBetweenPatterns;
	var tickBetweenPatterns;
	var patternRemoveQueue;
	var patternHistory;
	var patternHistoryIterator;

	*new {
		arg argServer;
		^super.new.init(argServer);
	}

	clear {
		patternCurrent = 0;
		isPlaying = false;
		ticksBetweenPatterns = 1 / delta;
		tickBetweenPatterns = ticksBetweenPatterns;
		ticksBetweenRecordings = 0;
		patternRemoveQueue = Array.new();
		patternHistory = Dictionary.new();
		patternIterator = 0;
		patternHistoryIterator = 0;
	}

	init {
		arg argServer;
		server = argServer;
		delta = 0.1;
		tick = 0;
		this.clear();
	}

	run {
		arg fnEmit;
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
					patternHistoryIterator = patternHistoryIterator + 1;
					("[BloomRecord] stop recording pattern"+patternHistoryIterator).postln;
					patternRecording.finish();
					patternHistory.put(patternHistoryIterator,patternRecording);
				});
			});

			if (isPlaying,{
				if (patternHistory[patternCurrent].notNil,{
					patternHistory[patternCurrent].emit(tick,
						{ arg v, age; fnEmit.(patternCurrent, v, age); },
						{ arg v; 
							("[BloomRecord] finished playing pattern "+patternCurrent).postln; 
							isPlaying = false;
							tickBetweenPatterns = ticksBetweenPatterns;
						}
					);
				});
			},{
				if (tickBetweenPatterns>0,{
					tickBetweenPatterns = tickBetweenPatterns - 1;
					if (tickBetweenPatterns==0,{
						var patterns = patternHistory.keys().asArray.sort;
						if (patterns.size>0,{
							patternIterator = patternIterator + 1;
							patternCurrent = patterns[patternIterator.mod(patterns.size)];
							("[BloomRecord] playing pattern "+patternCurrent).postln; 
							patternHistory[patternCurrent].play(tick);
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

	remove {
		arg pattern;
		if (pattern==patternCurrent,{
			("[BloomRecord] finished playing pattern "+patternCurrent).postln; 
			isPlaying = false;
			tickBetweenPatterns = ticksBetweenPatterns;
		});
		("[BloomRecord] removed pattern"+pattern).postln;
		patternHistory.put(pattern,nil);
	}
}