package org.helioviewer.gl3d.camera;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.wcs.CoordinateSystem;

/**
 * The GL3DCamera is resposible for the view space transformation. It sets up
 * the perspective and is generates the view space transformation. This
 * transformation is in turn influenced by the user interaction. Different
 * styles of user interaction are supported. These interactions are encapsuled
 * in {@link GL3DInteraction} objects that can be selected in the main toolbar.
 * The interactions then change the rotation and translation fields out of which
 * the resulting cameraTransformation is generated.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DCamera {
    protected GLU glu = new GLU();

    public static final double MAX_DISTANCE = -Constants.SunMeanDistanceToEarth * 1.8;
    public static final double MIN_DISTANCE = -Constants.SunRadius * 1.2;

    private double clipNear = Constants.SunRadius / 10;
    private double clipFar = Constants.SunRadius * 1000;
    private double fov = 10;
    private double aspect = 0.0;
    private double width = 0.0;
    private double height = 0.0;
    public int currentMouseX = 0;
    public int currentMouseY = 0;

    private List<GL3DCameraListener> listeners = new ArrayList<GL3DCameraListener>();

    // This is the resulting cameraTransformation. All interactions should
    // modify this matrix
    private GL3DMat4d cameraTransformation;

    private GL3DQuatd rotation;
    protected GL3DVec3d translation;

    private Stack<GL3DCameraAnimation> cameraAnimations = new Stack<GL3DCameraAnimation>();

    public GL3DCamera(double clipNear, double clipFar) {
        this();
        this.clipNear = clipNear;
        this.clipFar = clipFar;
    }

    public GL3DCamera() {
        this.cameraTransformation = GL3DMat4d.identity();
        this.rotation = GL3DQuatd.createRotation(0.0, new GL3DVec3d(0, 1, 0));
        this.translation = new GL3DVec3d();
    }

    public abstract void reset();

    /**
     * This method is called when the camera changes and should copy the
     * required settings of the preceding camera objects.
     * 
     * @param precedingCamera
     */
    public void activate(GL3DCamera precedingCamera) {
        if (precedingCamera != null) {
            this.rotation = precedingCamera.getRotation().copy();
            this.translation = precedingCamera.translation.copy();
            this.width = precedingCamera.width;
            this.height = precedingCamera.height;
            this.updateCameraTransformation();

            // Also set the correct interaction
            if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getRotateInteraction())) {
                this.setCurrentInteraction(this.getRotateInteraction());
            } else if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getPanInteraction())) {
                this.setCurrentInteraction(this.getPanInteraction());
            } else if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getZoomInteraction())) {
                this.setCurrentInteraction(this.getZoomInteraction());
            }
        } else {
            Log.debug("GL3DCamera: No Preceding Camera, resetting Camera");
            this.reset();
        }
    }

    protected void setZTranslation(double z) {
        this.translation.z = Math.min(MIN_DISTANCE, Math.max(MAX_DISTANCE, z));
    }

    protected void addPanning(double x, double y) {
        setPanning(this.translation.x + x, this.translation.y + y);
    }

    public void setPanning(double x, double y) {
        this.translation.x = x;
        this.translation.y = y;
    }

    public GL3DVec3d getTranslation() {
        return this.translation;
    }

    public GL3DMat4d getCameraTransformation() {
        return this.cameraTransformation;
    }

    public double getZTranslation() {
        return getTranslation().z;
    }

    public GL3DQuatd getRotation() {
        return this.rotation;
    }

    public void deactivate() {
        this.cameraAnimations.clear();
    }

    public void applyPerspective(GL3DState state) {
        GL2 gl = state.gl.getGL2();
        int viewport[] = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        this.width = (double) viewport[2];
        this.height = (double) viewport[3];
        this.aspect = width / height;

        //gl.glMatrixMode(GL2.GL_PROJECTION);

        //gl.glPushMatrix();
        //gl.glLoadIdentity();
        //glu.gluPerspective(this.fov, this.aspect, this.clipNear, this.clipFar);

        //gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void resumePerspective(GL3DState state) {
        GL2 gl = state.gl.getGL2();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void updateCameraTransformation() {
        this.updateCameraTransformation(true);
    }

    public void updateCameraTransformation(GL3DMat4d transformation) {
    	this.cameraTransformation = transformation;
    	fireCameraMoved();
    }

    
    /**
     * Updates the camera transformation by applying the rotation and
     * translation information.
     */
    public void updateCameraTransformation(boolean fireEvent) {
        cameraTransformation = GL3DMat4d.identity();
        cameraTransformation.translate(this.translation);
        cameraTransformation.multiply(this.rotation.toMatrix());

        if (fireEvent) {
            fireCameraMoved();
        }
    }

    public void applyCamera(GL3DState state) {
        for (Iterator<GL3DCameraAnimation> iter = this.cameraAnimations.iterator(); iter.hasNext();) {
            GL3DCameraAnimation animation = iter.next();
            if (!animation.isFinished()) {
                animation.animate(this);
            } else {
                iter.remove();
            }
        }
        state.multiplyMV(cameraTransformation);
    }

    public void addCameraAnimation(GL3DCameraAnimation animation) {
        for (Iterator<GL3DCameraAnimation> iter = this.cameraAnimations.iterator(); iter.hasNext();) {
            GL3DCameraAnimation ani = iter.next();
            if (!ani.isFinished() && ani.getClass().isInstance(animation)) {
                ani.updateWithAnimation(animation);
                return;
            }
        }

        this.cameraAnimations.add(animation);
    }

    public abstract GL3DMat4d getVM();

    public abstract double getDistanceToSunSurface();

    public abstract GL3DInteraction getPanInteraction();

    public abstract GL3DInteraction getRotateInteraction();

    public abstract GL3DInteraction getZoomInteraction();

    public abstract String getName();

    public void drawCamera(GL3DState state) {
        getCurrentInteraction().drawInteractionFeedback(state, this);
    }

    public abstract GL3DInteraction getCurrentInteraction();

    public abstract void setCurrentInteraction(GL3DInteraction currentInteraction);

    public double getFOV() {
        return this.fov;
    }

    public double getClipNear() {
        return clipNear;
    }

    public double getClipFar() {
        return clipFar;
    }

    public double getAspect() {
        return aspect;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public String toString() {
        return getName();
    }

    public void addCameraListener(GL3DCameraListener listener) {
        this.listeners.add(listener);
    }

    public void removeCameraListener(GL3DCameraListener listener) {
        this.listeners.remove(listener);
    }

    protected void fireCameraMoved() {
        for (GL3DCameraListener l : this.listeners) {
            l.cameraMoved(this);
        }
    }

    protected void fireCameraMoving() {
        for (GL3DCameraListener l : this.listeners) {
            l.cameraMoving(this);
        }
    }

    public abstract CoordinateSystem getViewSpaceCoordinateSystem();
}
