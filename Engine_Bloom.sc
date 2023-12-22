// Engine_Bloom

// Inherit methods from CroneEngine
Engine_Bloom : CroneEngine {

    // Bloom specific v0.1.0
	var server;
    var bloom;
    // Bloom ^

    *new { arg context, doneCallback;
        ^super.new(context, doneCallback);
    }

    alloc {
        // Bloom specific v0.0.1
        var server = context.server;

        bloom = BloomWildly(server,"/home/we/dust/code/bloom/samples/kalimba_small");

        this.addCommand("record","iff",{ arg msg;
            bloom.record(msg[1],[msg[2],msg[3]]);
        });
        this.addCommand("setScale","s",{ arg msg;
            bloom.setScale(msg[1].asString);
        });
        this.addCommand("setPatternDuration","f",{ arg msg;
            bloom.setPatternDuration(msg[1]);
        });
        this.addCommand("setSecondsBetweenRecordings","f",{ arg msg;
            bloom.setSecondsBetweenRecordings(msg[1]);
        });
        this.addCommand("setDroneVolume","f",{ arg msg;
            bloom.setDroneVolume(msg[1]);
        });
        this.addCommand("setSecondsBetweenPatterns","f",{ arg msg;
            bloom.setSecondsBetweenPatterns(msg[1]);
        });
        this.addCommand("setBlend","f",{ arg msg;
            bloom.setBlend(msg[1]);
        });
        this.addCommand("removeAll","",{ arg msg;
            bloom.removeAll();
        });
        this.addCommand("setShimmer","f",{ arg msg;
            bloom.setShimmer(msg[1]);
        });
        this.addCommand("setRoot","f",{ arg msg;
            bloom.setRoot(msg[1]);
        });
        this.addCommand("setBell","sf",{ arg msg;
            bloom.setBell(msg[1],msg[2]);
        });
        this.addCommand("setFinal","sf",{ arg msg;
            bloom.setFinal(msg[1],msg[2]);
        });

    }

    
	free {
		bloom.free;
	}
}
