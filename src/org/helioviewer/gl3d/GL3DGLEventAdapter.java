package org.helioviewer.gl3d;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.helioviewer.gl3d.view.GL3DComponentView;

/**
 * Adapter for a {@link GLEventListener}. Add an adapter to the
 * {@link GL3DComponentView}'s getComponent().
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DGLEventAdapter implements GLEventListener {

    public void display(GLAutoDrawable autoDrawable) {
    }

    public void displayChanged(GLAutoDrawable autoDrawable, boolean widt, boolean height) {
    }

    public void init(GLAutoDrawable autoDrawable) {
    };

    public void reshape(GLAutoDrawable autoDrawable, int x, int y, int width, int height) {
    }

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	};
}
