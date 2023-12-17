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

        bloom = BloomWidly(server);
    }


	free {
		bufs.keysValuesDo({ arg k, val;
			val.free;
		});
		oscs.keysValuesDo({ arg k, val;
			val.free;
		});
		syns.keysValuesDo({ arg k, val;
			val.free;
		});
		loops.keysValuesDo({ arg k, val;
			val.free;
		});
		buses.keysValuesDo({ arg k, val;
			val.free;
		});
	}
}
