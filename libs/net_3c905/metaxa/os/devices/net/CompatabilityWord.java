package metaxa.os.devices.net;	

class CompatabilityWord {
    private byte warninglevel;
    private byte failurelevel;

    public CompatabilityWord(byte warning, byte failure) {
	warninglevel = warning;
	failurelevel = failure;
    }

    public byte get_warninglevel() {
	return warninglevel;
    }
    public byte get_failurelevel() {
	return failurelevel;
    }
}
