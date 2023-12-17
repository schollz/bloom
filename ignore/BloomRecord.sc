BloomRecord {
	var server;
	var <tick;
	var <timer;
	var <patternRecording;
	var <patternCurrent;
	var patternIterator;
	var ticksBetweenRecordings;
	var secondsBetweenRecordings;
	var delta;
	var isPlaying;
	var ticksBetweenPatterns;
	var tickBetweenPatterns;
	var patternRemoveQueue;
	var patternHistory;
	var patternHistoryIterator;
	var fnEmit;

	*new {
		arg argServer, argFnEmit;
		^super.new.init(argServer, argFnEmit);
	}

	clear {
		patternCurrent = 0;
		isPlaying = false;
		ticksBetweenPatterns = 4 / delta;
		tickBetweenPatterns = ticksBetweenPatterns;
		ticksBetweenRecordings = 0;
		patternRemoveQueue = Array.new();
		patternHistory = Dictionary.new();
		patternIterator = 0;
		patternHistoryIterator = 0;
	}

	init {
		arg argServer, argFnEmit;
		server = argServer;
		fnEmit = argFnEmit;
		delta = 0.1;
		tick = 0;
		secondsBetweenRecordings = 3;
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
					tickBetweenPatterns = ticksBetweenPatterns + rrand(0,10);
					patternHistoryIterator = patternHistoryIterator + 1;
					("[BloomRecord] stop recording pattern"+patternHistoryIterator).postln;
					patternRecording.finish();
					patternHistory.put(patternHistoryIterator,patternRecording);
				});
			});

			if (isPlaying,{
				if (patternHistory[patternCurrent].notNil,{
					patternHistory[patternCurrent].emit(tick,
						{ arg v, age, patternI, patternN; fnEmit.(patternCurrent, v, age,patternI, patternN); },
						{ arg v;
							("[BloomRecord] finished playing pattern "+patternCurrent).postln;
							isPlaying = false;
							tickBetweenPatterns = ticksBetweenPatterns + rrand(0,10);
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
							tickBetweenPatterns = ticksBetweenPatterns + rrand(0,10);
						});
					});
				});
			});
		})}.fork;
	}

	record {
		arg v;
		if (ticksBetweenRecordings==0,{
			patternRecording = BloomPattern();
		});
		ticksBetweenRecordings = secondsBetweenRecordings / delta;
		patternRecording.record(tick, v, { arg v, age; fnEmit.(patternCurrent, v, age, 0, 0); },);
	}

	setSecondsBetweenRecordings {
		arg v;
		("[BloomRecord] set seconds between recordings "+v).postln;
		secondsBetweenRecordings = v;
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

	free {
		if (timer.notNil,{
			timer.stop;
		});
	}
}