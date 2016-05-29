package jx.awt.peer;

import java.awt.*;
import java.awt.event.*;
import jx.zero.*;
import jx.awt.*;


/**
 * This thread preforms all executive operations that are
 * initiated by a menu point selection.
 */
public class JXMenuThread
    extends Thread {

    private CPUManager cpuManager;
    private AtomicVariable av;
    private CPUState cpuState;

    private MenuItem selectedItem = null;
    private MenuHandler mh;
    private JXToolkit toolkit;
    


    /** Creates a new JXMenuThread instance */
    public JXMenuThread(JXToolkit toolkit) {
	this.toolkit = toolkit;
	cpuManager = (CPUManager)LookupHelper.waitUntilPortalAvailable(InitialNaming.getInitialNaming(),"CPUManager");
	av = cpuManager.getAtomicVariable();
	av.set(null);
	start();
    }



    /**
     * Sets the selected item and unblocks the thread. The selected
     * item is the menu point that was just selected. After running
     * this method, the Thread will awake and handle the item.
     */
    public void setSelectedItem(MenuItem m) {
	selectedItem = m;
	av.atomicUpdateUnblock(selectedItem, cpuState);
    }

    /**
     * This is the overridden thread method that executes the thread.
     * The thread blocks until an item is delivered, then it checks
     * the item and performs some item-dependent operations, like
     * resetting a menu bar, closing all menus, and firing some events.
     */
    public void run() {
	mh = toolkit.getMenuHandler();
	System.out.println("MenuThread started.");
	
	while (true) {
	    // block thread until activity needed
	    cpuState = cpuManager.getCPUState();
	    av.blockIfEqual(null);
	    cpuState = null;
	    // Thread has been unblocked...
	    // reset atomic variable
	    av.set(null);

	    boolean popup = mh.isPopupMenuOpen();
	    mh.deleteOpenMenus();
	    if (!popup) {
		// reset menubar display
		MenuBar mb = mh.getCurrentMenuBar();
		((JXMenuBarPeer) mb.getPeer()).resetMenuBar();
		mh.setCurrentMenuBar(null);
	    }

	    // send events according to the MenuItem type
	    EventQueue eq = toolkit.getSystemEventQueue();
	    if (selectedItem instanceof CheckboxMenuItem) {
		// toggle checkbox state
		boolean newState = !(((CheckboxMenuItem) selectedItem).getState());
		((CheckboxMenuItem) selectedItem).setState(newState);
		int newStateInt = (newState) ? ItemEvent.SELECTED : ItemEvent.DESELECTED;
		eq.postEvent(new ItemEvent((ItemSelectable) selectedItem,
					   ItemEvent.ITEM_STATE_CHANGED,
					   selectedItem, newStateInt));
	    } else
		if (selectedItem instanceof MenuItem) {
		    eq.postEvent(new ActionEvent(selectedItem, ActionEvent.ACTION_PERFORMED,
						 ((MenuItem) selectedItem).getActionCommand(), 0));
		}
	}
    }
}
