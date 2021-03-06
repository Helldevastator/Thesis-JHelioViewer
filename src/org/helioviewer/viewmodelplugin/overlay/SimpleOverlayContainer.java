package org.helioviewer.viewmodelplugin.overlay;

import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.opengl.OverlayPluginContainer;

/**
 * This basic class extends the {@link OverlayContainer} by a default
 * implementation of the install process of a overlay.
 * 
 * @author Stephan Pagel
 */
public abstract class SimpleOverlayContainer extends OverlayContainer {

    /**
     * {@inheritDoc}
     */

    protected void installOverlayImpl(OverlayView overlayView, OverlayControlComponentManager controlList) {
        OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
        overlayPluginContainer.setRenderer(getRenderer());
    	overlayView.addOverlay(overlayPluginContainer);
        controlList.add(new OverlayControlComponent(getControlComponent(), getName()));
    }

    /**
     * {@inheritDoc}
     */

    public Class<? extends PhysicalRenderer> getOverlayClass() {
        return getRenderer().getClass();
    }

    /**
     * Returns a new object of the contained overlay.
     * 
     * @return A new object of the contained overlay.
     */
    protected abstract PhysicalRenderer getRenderer();

    /**
     * Returns a new control component for the contained overlay.
     * 
     * @return a new control component for the contained overlay.
     */
    protected abstract OverlayPanel getControlComponent();
}
