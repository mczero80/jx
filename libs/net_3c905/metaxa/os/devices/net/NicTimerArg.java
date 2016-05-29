package metaxa.os.devices.net;

class NicTimerArg {
    private D3C905 Handle;
    private NicInformation Adapter;

    public NicTimerArg(D3C905 handle, NicInformation adapter) {
	Handle = handle;
	Adapter = adapter;
    }

    public D3C905 get_Handle() {
	return Handle;
    }
    public NicInformation get_NicInfo() {
	return Adapter;
    }
}
