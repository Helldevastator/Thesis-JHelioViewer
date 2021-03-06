package org.helioviewer.jhv.gui.dialogs.observation;

import javax.swing.JPanel;

/**
 * Abstract base class for UI components. Objects which are derived from this
 * base class can be added to the {@link ObservationDialog}.
 * 
 * @author Stephan Pagel
 * */
public abstract class ObservationDialogPanel extends JPanel {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    protected static final int GRIDLAYOUT_HGAP = 5;
    protected static final int GRIDLAYOUT_VGAP = 2;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This method will be executed when the {@link ObservationDialog} shows up.
     * */
    public abstract void dialogOpened();

    /**
     * This method will be executed when the panel has been selected within the
     * dialog.
     * */
    public abstract void selected();

    /**
     * This method will be executed when the panel has been displayed within the
     * dialog and another panel is selected.
     * */
    public abstract void deselected();

    /**
     * This method will be called when the add button within the dialog has been
     * pressed.
     * */
    public abstract boolean loadButtonPressed();

    /**
     * This method will be called when the cancel button within the dialog has
     * been pressed.
     * */
    public abstract void cancelButtonPressed();
}
