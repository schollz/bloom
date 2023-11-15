BloomPattern {
	var <arrTime;
	var <arrValue;
	var <itPlay;
	var <isPlaying;
	var <isRecording;
	var <timePlaying;
	var <timeLast;

	*new {
		^super.new.init();
	}

	init {
		arrTime = Array.new();
		arrValue = Array.new();
		isPlaying = false;
		isRecording = false;
	}

	record {
		arg time, value;
		if (isRecording,{
		},{
			// not recording
			isRecording = true;
			timeLast = time;
		});
		arrTime = arrTime.add(time-timeLast);
		arrValue = arrValue.add(value);
		timePlaying = 0;
		isPlaying = false;
		isRecording = true;
		itPlay = 0;
		timeLast = time;
		("[BloomPattern] recorded"+value+"at"+time).postln;
	}

	finish {
		isRecording = false;
	}

	play {
		arg time;
		if (arrTime.size>0,{
			timeLast = time;
			itPlay = 0;
			isPlaying = true;
		});
	}

	emit {
		arg time, fnEmit, fnFinish;
		if (isPlaying,{
			if ((time-timeLast)>arrTime[itPlay],{
				// record ght time playing
				timePlaying = timePlaying + (time-timeLast);
				// play
				if (fnEmit.notNil,{
					fnEmit.(arrValue[itPlay],timePlaying);
				});
				// TODO: randomly modulate the delta
				// arrTime[itPlay] = arrTime[itPlay] + (rrand(-1000,1000)/1000);

				// iterate
				itPlay = itPlay + 1;
				// check if finished
				if (itPlay==arrTime.size,{
					isPlaying = false;
					if (fnFinish.notNil,{
						fnFinish.(time);
					});
				});			
				timeLast = time;
			});
		});
	}

	free {
		isPlaying = false;
	}
}