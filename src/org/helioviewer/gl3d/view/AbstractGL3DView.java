package org.helioviewer.gl3d.view;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.view.opengl.AbstractGLView;

/**
 * Default super class for all {@link GL3DView}s. Provides default behavior like
 * view chain traversal.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class AbstractGL3DView extends AbstractGLView implements GL3DView {

    public void renderGL(GL2 gl, boolean nextView) {
        render3D(GL3DState.get());
		GL3DState.get().checkGLErrors(this+".afterRender3D");
    }

    public void deactivate(GL3DState state) {
        if (getView() != null) {
            if (getView().getAdapter(GL3DView.class) != null) {
                getView().getAdapter(GL3DView.class).deactivate(state);
            }
        }

    }
}
