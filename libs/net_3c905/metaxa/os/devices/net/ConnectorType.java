package metaxa.os.devices.net;
import jx.zero.Debug;

class ConnectorType {

    final static byte CONNECTOR_10BASET = 0;
    final static byte CONNECTOR_10AUI = 1;
    final static byte CONNECTOR_10BASE2 = 3;
    final static byte CONNECTOR_100BASETX = 4;
    final static byte CONNECTOR_100BASEFX = 5;
    final static byte CONNECTOR_MII = 6;
    final static byte CONNECTOR_AUTONEGOTIATION = 8;
    final static byte CONNECTOR_EXTERNAL_MII = 9;
    final static byte CONNECTOR_UNKNOWN = (byte)0xFF;
    
    private int connector;

    public ConnectorType() {
	connector = CONNECTOR_UNKNOWN;
    }

    public ConnectorType(int c) throws UndefinedConnectorException {
	set_Connector(c);
    }

    public void set_Connector(int c) throws UndefinedConnectorException {
	if (!(c == CONNECTOR_10BASET || c== CONNECTOR_10AUI || c== CONNECTOR_10BASE2 || c== CONNECTOR_100BASETX  
	    || c== CONNECTOR_100BASEFX  || c== CONNECTOR_MII || c== CONNECTOR_AUTONEGOTIATION  
	    || c== CONNECTOR_EXTERNAL_MII || c== CONNECTOR_UNKNOWN)) {
	    throw new UndefinedConnectorException();
	}
	else {
	    connector = c;
	}
    }

    public int get_Connector() {
	return connector;
    }
}
