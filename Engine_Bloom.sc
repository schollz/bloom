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

        bloom = BloomWildly(server);

        this.addCommand("key","ff",{ arg msg;
            bloom.record(0,[msg[1],msg[2]]);
        });
    }

    
	free {
		bloom.free;
	}
}
