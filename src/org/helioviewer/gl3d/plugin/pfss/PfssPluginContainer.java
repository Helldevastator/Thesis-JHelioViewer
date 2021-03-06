package org.helioviewer.gl3d.plugin.pfss;

import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.opengl.OverlayPluginContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponent;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponentManager;

/**
 * Plugincontainer for Pfss
 * 
 * @author Stefan Meier, Jonas Schwammberger
 */
public class PfssPluginContainer extends OverlayContainer {
	private PfssPluginPanel pfssPluginPanel;
	private boolean builtin_mode = false;

	public PfssPluginContainer(boolean builtin_mode) {
		this.builtin_mode = builtin_mode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void installOverlayImpl(OverlayView overlayView,
			OverlayControlComponentManager controlList) {
		PfssPlugin3dRenderer renderer = new PfssPlugin3dRenderer();
		pfssPluginPanel = new PfssPluginPanel(renderer);
		OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
		overlayPluginContainer
				.setRenderer3d(renderer);
		overlayView.addOverlay(overlayPluginContainer);
		controlList
				.add(new OverlayControlComponent(pfssPluginPanel, getName()));

	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "PFSS " + (builtin_mode ? "Built-In Version" : "");
	}


	@Override
	public Class<? extends PhysicalRenderer> getOverlayClass() {
		// TODO Auto-generated method stub
		return PfssPlugin3dRenderer.class;
	}

}
