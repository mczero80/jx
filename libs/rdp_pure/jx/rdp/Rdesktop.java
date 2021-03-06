package jx.rdp;

import java.io.*;
import jx.rdp.crypto.*;
import java.net.*;
import java.awt.*;
import jx.zero.Debug;

public class Rdesktop extends Frame{
    
    
    public static void main(String[] args) throws OrderException, RdesktopException {
	Rdp RdpLayer = null;
	Rdesktop frame = new Rdesktop();
	/*try {*/
	    RdpLayer = new Rdp();
	    /*} catch(NoSuchProviderException e) {
	    Debug.out.println("Der Provider Cryptix konnte nicht gefunden werden!");
	    System.exit(0);
	} catch(NoSuchAlgorithmException f) {
	    Debug.out.println("Einer oder mehrere Kryptoalgorithmen konnten nicht gefunden werden!");
	    Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + f.getMessage());
	    System.exit(0);
	    }*/
	RdesktopFrame window = new RdesktopFrame(800, 600);
	RdpLayer.registerDrawingSurface(window);
	window.registerCommLayer(RdpLayer);
	window.registerKeyboard(KeyLayout.getLayout(KeyLayout.JX_INTEL));
	Debug.out.println("Connecting to Server ...");
	if(RdpLayer != null) {
	    try {
		RdpLayer.connect("root", args[0], Rdp.RDP_LOGON_NORMAL,  "", "", "", "");
	    } catch(UnknownHostException h) {
		Debug.out.println("Der Zielrechner wurde nicht gefunden!");
		System.exit(0);
	    } catch(SocketException i) {
		Debug.out.println("Beim Zugriff auf den Socket ist ein Fehler passiert!");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + i.getMessage());
		System.exit(0);
	    } catch(IOException g) {
		Debug.out.println("Die Verbindung konnte nicht hergestellt werden!");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + g.getMessage());
		System.exit(0);
	    } catch(CryptoException m) {
		Debug.out.println("Bei der Verschluesselung ist ein Fehler aufgetreten");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + m.getMessage());
		System.exit(0);
	    } /*catch(NoSuchAlgorithmException o) {
		Debug.out.println("Einer oder mehrere Kryptoalgorithmen konnten nicht gefunden werden!");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + o.getMessage());
		System.exit(0);
		}*/ catch(RdesktopException q) {
		Debug.out.println("Ein Protokollfehler ist aufgetreten!");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + q.getMessage());
		q.printStackTrace(System.err);
		System.exit(0);
	    }
	    Debug.out.println("Connection successful!");
	    window.show(true);
	    try {
	      RdpLayer.mainLoop();
	    } /*catch(NoSuchProviderException j) {
		Debug.out.println("Der Provider Cryptix konnte nicht gefunden werden!");
		RdpLayer.disconnect();
		window.show(false);
		window.quit();
		System.exit(0);
	    } catch(NoSuchAlgorithmException k) {
		Debug.out.println("Einer oder mehrere Kryptoalgorithmen konnten nicht gefunden werden!");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + k.getMessage());
		window.show(false);
		window.quit();
		RdpLayer.disconnect();
		System.exit(0);
	    }*/catch(CryptoException l) {
		Debug.out.println("Es konnte kein Schluessel registriert werden!");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + l.getMessage());
		window.show(false);
		window.quit();
		RdpLayer.disconnect();
		System.exit(0);
	    } catch(IOException n) {
		Debug.out.println("Bei der Uebertragung ist ein Fehler aufgetreten!");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + n.getMessage());
		window.show(false);
		window.quit();
		System.exit(0);
	    } catch(OrderException p) {
		Debug.out.println("Bei der Verarbeitung von Zeichenbefehlen is ein Fehler aufgetreten!");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + p.getMessage());
		p.printStackTrace(System.err);
		System.exit(0);
	    } catch(RdesktopException r) {
		Debug.out.println("Ein Protokollfehler ist aufgetreten!");
		Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + r.getMessage());
		r.printStackTrace(System.err);
		System.exit(0);
	    }
	    window.show(false);
	    window.quit();
	    Debug.out.println("Disconnecting ...");
	    RdpLayer.disconnect();
	    Debug.out.println("Disconnected");
	    System.exit(0);
	} else {
	    Debug.out.println("Die Kommunikationsschicht konnte nicht erzeugt werden!");
	}
    }

    public Rdesktop() {
	super("Rdesktop for JX");
	this.setSize(new Dimension(100, 50));
	this.setVisible(true);
    }
}

