BloomPattern {
	var <arrTime;
	var <arrValue;
	var <itPlay;
	var <timePlay;
	var <isPlaying;
	var <timePlaying;
	var <timeLast;

	*new {
		^super.new.init();
	}

	init {
		arrTime = Array.new();
		arrValue = Array.new();
		isPlaying = false;
	}

	record {
		arg time, value;
		// time is absolute time
		arrTime = arrTime.add(time);
		arrValue = arrValue.add(value);
		timePlaying = 0;
		isPlaying = false;
		itPlay = 0;
		("[BloomPattern] recorded"+value+"at"+time).postln;
	}

	finish {
		var arr0 = arrTime[0];
		arrTime.do({ arg v, i;
			arrTime[i] = v - arr0;
		});
	}

	play {
		arg time;
		if (arrTime.size>0,{
			timePlay = time;
			timeLast = time;
			itPlay = 0;
			isPlaying = true;
		});
	}

	emit {
		arg time, fnEmit, fnFinish;
		if (isPlaying,{
			timePlaying = timePlaying + (time-timeLast);
			timeLast = time;
			if ((time-timePlay)>arrTime[itPlay],{
				// play
				if (fnEmit.notNil,{
					fnEmit.(arrValue[itPlay],timePlaying);
				});
				// iterate
				itPlay = itPlay + 1;
				// check if finished
				if (itPlay==arrTime.size,{
					isPlaying = false;
					if (fnFinish.notNil,{
						fnFinish.(time);
					});
				});				
			});
		});
	}

	free {
		isPlaying = false;
	}
}