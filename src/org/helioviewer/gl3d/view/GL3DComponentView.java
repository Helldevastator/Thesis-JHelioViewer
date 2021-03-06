package org.helioviewer.gl3d.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import jogamp.opengl.glu.mipmap.PixelStorageModes;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.model.image.GL3DImageLayer;
import org.helioviewer.gl3d.model.image.GL3DImageLayers;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.AbstractComponentView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.opengl.GLSharedContext;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.GLView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderView;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;


import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.awt.ImageUtil;

/**
 * The top-most View in the 3D View Chain. Let's the viewchain render to its
 * {@link GLCanvas}.
 * 
 * 
 * @author Simon Sp���rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DComponentView extends AbstractComponentView implements
		GLEventListener, ComponentView {
	public static final String SETTING_TILE_WIDTH = "gl.screenshot.tile.width";
	public static final String SETTING_TILE_HEIGHT = "gl.screenshot.tile.height";

	private GLCanvas canvas;
	private FPSAnimator animator;

	private Color backgroundColor = Color.BLACK;
	private Color outsideViewportColor = Color.DARK_GRAY;

	private boolean backGroundColorHasChanged = false;

	private boolean rebuildShadersRequest = false;

	private GLTextureHelper textureHelper = new GLTextureHelper();
	private GLShaderHelper shaderHelper = new GLShaderHelper();

	// private GL3DOrthoView orthoView;
	private ViewportView viewportView;
	private ViewportView screenshotViewportView;

	private GL3DImageTextureView activeLayer = null;
	
	private ReentrantLock animationLock = new ReentrantLock();

	private Vector2dInt viewportSize;
	private int[] frameBufferObject;
	private int[] renderBufferDepth;
	private int[] renderBufferColor;
	private static int defaultTileWidth = 2048;
	private static int defaultTileHeight = 2048;
	private int tileWidth = 512;
	private int tileHeight = 512;

	private boolean saveBufferedImage = true;
	private BufferedImage screenshot;
	private ByteBuffer screenshotBuffer;
	private TileRenderer tileRenderer;
	private Viewport viewport;
	private Viewport defaultViewport;
	
	private double zTranslation = 1;

	private double clipNear = Constants.SunRadius / 10;
	private double clipFar = Constants.SunRadius * 1000;
	private double fov = 10;
	private double aspect = 0.0;
	private double width = 0.0;
	private double height = 0.0;

	public GL3DComponentView() {
		this.canvas = new GLCanvas();
		// this.canvas = new GLCanvas(null, null,
		// GLSharedContext.getSharedContext(), null);
		// Just for testing...
		animator = new FPSAnimator(canvas, 10);
		this.canvas.addGLEventListener(this);
	}

	public void deactivate() {
		if (this.animator != null) {
			this.animator.stop();
			if (getAdapter(GL3DView.class) != null) {
				getAdapter(GL3DView.class).deactivate(GL3DState.get());
			}
		}
		animationLock.lock();
	}

	public void activate() {
		this.animator.start();
		if (this.animationLock.isLocked())
			animationLock.unlock();
	}

	public GLCanvas getComponent() {
		return this.canvas;
	}

	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
		Log.debug("GL3DComponentView.DisplayChanged");
	}

	public void init(GLAutoDrawable glAD) {
		Log.debug("GL3DComponentView.Init");
		GLSharedContext.setSharedContext(glAD.getContext());
		GL2 gl = glAD.getGL().getGL2();
		GL3DState.create(gl);

		frameBufferObject = new int[1];
		gl.glGenFramebuffers(1, frameBufferObject, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
		generateNewRenderBuffers(gl);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

		// GLTextureCoordinate.init(gl);
		textureHelper.delAllTextures(gl);
		GLTextureHelper.initHelper(gl);

		shaderHelper.delAllShaderIDs(gl);
		// gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
		// gl.glShadeModel(GL.GL_FLAT);
		gl.glShadeModel(GL2.GL_SMOOTH);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		// gl.glEnable(GL.GL_TEXTURE_1D);
		// gl.glEnable(GL.GL_TEXTURE_2D);gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_BLEND);
		// gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE,
		// GL.GL_REPLACE);
		gl.glEnable(GL2.GL_BLEND);
		gl.glEnable(GL2.GL_POINT_SMOOTH);
		gl.glEnable(GL2.GL_COLOR_MATERIAL);

		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_NORMALIZE);
		// gl.glEnable(GL.GL_CULL_FACE);
		gl.glCullFace(GL2.GL_BACK);
		gl.glFrontFace(GL2.GL_CCW);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		// gl.glDepthFunc(GL.GL_LESS);
		gl.glDepthFunc(GL2.GL_LEQUAL);

		// gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[] { 0.2f, 0.2f,
		// 0.2f }, 0);
		// gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, new float[] { 0.6f, 0.6f,
		// 0.6f }, 0);
		// gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, new float[] { 0.2f, 0.2f,
		// 0.2f }, 0);
		// gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, new float[] { 0, 0,
		// (float) Constants.SunMeanDistanceToEarth }, 0);
		gl.glEnable(GL2.GL_LIGHT0);

		viewportSize = new Vector2dInt(0, 0);
		this.rebuildShadersRequest = true;
		// gl.glColor3f(1.0f, 1.0f, 0.0f);
	}

	public void reshape(GLAutoDrawable glAD, int x, int y, int width, int height) {
		viewportSize = new Vector2dInt(width, height);
		// Log.debug("GL3DComponentView.Reshape");
		GL gl = glAD.getGL();

		gl.setSwapInterval(1);

		updateViewport();
		//
		// gl.glViewport(0, 0, width, height);
		// gl.glMatrixMode(GL.GL_PROJECTION);
		// gl.glLoadIdentity();
		//
		// gl.glOrtho(0, width, 0, height, -1, 10000);
		//
		// gl.glMatrixMode(GL.GL_MODELVIEW);
		// gl.glLoadIdentity();
	}

	public void display(GLAutoDrawable glAD) {
		
		GL2 gl = glAD.getGL().getGL2();

		if (defaultViewport != null)
			this.getAdapter(ViewportView.class).setViewport(defaultViewport,
					new ChangeEvent());

		int width = this.viewportSize.getX();
		int height = this.viewportSize.getY();

		GL3DState.getUpdated(gl, width, height);

		if (backGroundColorHasChanged) {
			gl.glClearColor(backgroundColor.getRed() / 255.0f,
					backgroundColor.getGreen() / 255.0f,
					backgroundColor.getBlue() / 255.0f,
					backgroundColor.getAlpha() / 255.0f);

			backGroundColorHasChanged = false;
		}

		// Rebuild all shaders, if necessary
		if (rebuildShadersRequest) {
			rebuildShaders(gl);
		}

		GL3DState.get().checkGLErrors("GL3DComponentView.afterRebuildShader");

		// Save Screenshot, if requested

		//gl.getContext().makeCurrent();
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

		gl.glClearColor(backgroundColor.getRed() / 255.0f,
				backgroundColor.getGreen() / 255.0f,
				backgroundColor.getBlue() / 255.0f,
				backgroundColor.getAlpha() / 255.0f);

		Viewport v = this.getAdapter(ViewportView.class).getViewport();

		this.width = v.getWidth();
		this.height = v.getHeight();

		this.aspect = this.width / this.height;
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		double fH = Math.tan(this.fov / 360.0 * Math.PI) * clipNear;
		double fW = fH * aspect;
		gl.glViewport(0, 0, v.getWidth(), v.getHeight());

		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D)
		gl.glFrustum(-fW, fW, -fH, fH, clipNear, clipFar);
		
		else {
			Region region = this.getAdapter(RegionView.class).getRegion();
			GL3DCamera camera = this.getAdapter(GL3DCameraView.class).getCurrentCamera();
			double scaleFactor = camera.getZTranslation() / -1.0054167950116766E10 - 0.1;
			gl.glOrtho(-1182667503.408581*scaleFactor, (1182667503.408581)*scaleFactor, -879625716.820895*scaleFactor, (879625716.820894)*scaleFactor, clipNear, clipFar);
		}

		// GLU glu = new GLU();
		// glu.gluPerspective(this.fov, this.aspect, this.clipNear,
		// this.clipFar);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		// this.aspect = width / height;
		// tileRenderer.trPerspective(this.fov, this.aspect, this.clipNear,
		// this.clipFar);

		// gl.glClearColor(outsideViewportColor.getRed() / 255.0f,
		// outsideViewportColor.getGreen() / 255.0f,
		// outsideViewportColor.getBlue() / 255.0f,
		// outsideViewportColor.getAlpha() / 255.0f);

		// ViewportImageSize viewportImageSize =
		// ViewHelper.calculateViewportImageSize(view);

		displayBody(gl);

		//canvas.getContext().release();

	}

	private void displayBody(GL2 gl) {

		int width = this.viewportSize.getX();
		int height = this.viewportSize.getY();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glColor4f(1, 1, 1, 1);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_DEPTH_TEST);

		gl.glLoadIdentity();

		gl.glPushMatrix();
		if (this.getView() instanceof GLView) {
			((GLView) this.getView()).renderGL(gl, true);
		}
		
		GL3DState.get().checkGLErrors("GL3DComponentView.afterRenderGL");

		gl.glPopMatrix();

		gl.glPushMatrix();
		if (!this.postRenderers.isEmpty()) {

			// gl.glViewport((int)(width*0.55), (int)(height*0.55),
			// (int)(width*0.45), (int)(height*0.45));

			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();

			gl.glOrtho(0, width, 0, height, -1, 10000);

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glTranslatef(0.0f, height, 0.0f);
			gl.glScalef(1.0f, -1.0f, 1.0f);
			gl.glDisable(GL2.GL_LIGHTING);
			gl.glColor4f(1, 1, 1, 0);
			gl.glDisable(GL2.GL_DEPTH_TEST);
			gl.glEnable(GL2.GL_TEXTURE_2D);
			GLScreenRenderGraphics glRenderer = new GLScreenRenderGraphics(gl);
			synchronized (postRenderers) {
				for (ScreenRenderer r : postRenderers) {
					r.render(glRenderer);
				}
			}

			gl.glDisable(GL2.GL_TEXTURE_2D);

		}
		gl.glPopMatrix();
		GL3DState.get().checkGLErrors("GL3DComponentView.afterPostRenderers");
	}

	private void generateNewRenderBuffers(GL gl) {
		// tileWidth = defaultTileWidth;
		// tileHeight = defaultTileHeight;
		if (renderBufferDepth != null) {
			gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
		}
		renderBufferDepth = new int[1];
		gl.glGenRenderbuffers(1, renderBufferDepth, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferDepth[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT,
				tileWidth, tileHeight);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER,
				renderBufferDepth[0]);

		if (renderBufferColor != null) {
			gl.glDeleteRenderbuffers(1, renderBufferColor, 0);
		}
		renderBufferColor = new int[1];
		gl.glGenRenderbuffers(1, renderBufferColor, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferColor[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, tileWidth,
				tileHeight);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER,
				renderBufferColor[0]);
	}

	public void saveScreenshot(String imageFormat, File outputFile)
			throws IOException {
		ImageIO.write(this.getBufferedImage(), imageFormat, outputFile);
	}

	public void saveScreenshot(String imageFormat, File outputFile, int width,
			int height) {
		// this.animator.stop();
		try {
			ImageIO.write(this.getBufferedImage(width, height), imageFormat,
					outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// this.animator.start();
	}

	public void setBackgroundColor(Color background) {
		backgroundColor = background;
		backGroundColorHasChanged = true;
	}

	public BufferedImage getBufferedImage() {
		return getBufferedImage(this.canvas.getWidth(), this.canvas.getHeight());
	}

	public BufferedImage getBufferedImage(int width, int height) {
		defaultViewport = this.getAdapter(ViewportView.class).getViewport();
		Viewport viewport = StaticViewport.createAdaptedViewport(width, height);
		this.getAdapter(ViewportView.class).setViewport(viewport,
				new ChangeEvent());

		tileWidth = width < defaultTileWidth ? width : defaultTileWidth;
		tileHeight = height < defaultTileHeight ? height : defaultTileHeight;

		Log.trace(">> GLComponentView.display() > Start taking screenshot");
		double xTiles = width / (double) tileWidth;
		double yTiles = height / (double) tileHeight;
		int countXTiles = width % tileWidth == 0 ? (int) xTiles
				: (int) xTiles + 1;
		int countYTiles = height % tileHeight == 0 ? (int) yTiles
				: (int) yTiles + 1;

		GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile
				.getDefault());
		GLProfile profile = GLProfile.get(GLProfile.GL2);
		profile = GLProfile.getDefault();
		GLCapabilities capabilities = new GLCapabilities(profile);
		capabilities.setDoubleBuffered(false);
		capabilities.setOnscreen(false);
		capabilities.setHardwareAccelerated(true);
		capabilities.setFBO(true);
		
		GLDrawable offscreenDrawable = factory.createOffscreenDrawable(null,
				capabilities, null, tileWidth, tileHeight);
		
		offscreenDrawable.setRealized(true);
		GLContext offscreenContext = canvas.getContext();
		//GLContext offscreenContext = offscreenDrawable.createContext(this.canvas.getContext());
		offscreenDrawable.setRealized(true);
		offscreenContext.makeCurrent();
		GL2 offscreenGL = offscreenContext.getGL().getGL2();
		//GL2 offscreenGL = canvas.getContext().getGL().getGL2();
		
		offscreenGL.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
		generateNewRenderBuffers(offscreenGL);

		screenshot = new BufferedImage(viewport.getWidth(),
				viewport.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		screenshotBuffer = ByteBuffer.wrap(((DataBufferByte) screenshot
				.getRaster().getDataBuffer()).getData());

		offscreenGL.glViewport(0, 0, tileWidth, tileHeight);

		double aspect = width / (double) height;
		double top = Math.tan(this.fov / 360.0 * Math.PI) * clipNear;
		double right = top * aspect;
		double left = -right;
		double bottom = -top;

		offscreenGL.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		double tileLeft, tileRight, tileBottom, tileTop;

		for (int x = 0; x < countXTiles; x++) {
			for (int y = 0; y < countYTiles; y++) {
				tileLeft = left + (right - left) / xTiles * x;
				tileRight = left + (right - left) / xTiles * (x + 1);
				tileBottom = bottom + (top - bottom) / yTiles * y;
				tileTop = bottom + (top - bottom) / yTiles * (y + 1);
				//offscreenGL.glFlush();

				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glLoadIdentity();
				offscreenGL.glViewport(0, 0, tileWidth, tileHeight);
				offscreenGL.glFrustum(tileLeft, tileRight, tileBottom, tileTop,
						clipNear, clipFar);
				
				offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);
				offscreenGL.glLoadIdentity();

				// double factor =
				int destX = tileWidth * x;
				int destY = tileHeight * y;

				GL3DState.get().checkGLErrors(
						"GL3DComponentView.beforeTileRenderer");

				displayBody(offscreenGL);

				offscreenGL.glPixelStorei(GL2.GL_PACK_ROW_LENGTH, width);
				offscreenGL.glPixelStorei(GL2.GL_PACK_SKIP_ROWS, destY);
				offscreenGL.glPixelStorei(GL2.GL_PACK_SKIP_PIXELS, destX);
				offscreenGL.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);

				int cutOffX = width >= (x + 1) * tileWidth ? tileWidth
						: viewport.getWidth() - x * tileWidth;
				int cutOffY = height >= (y + 1) * tileHeight ? tileHeight
						: viewport.getHeight() - y * tileHeight;

				offscreenGL.glReadPixels(0, 0, cutOffX, cutOffY, GL2.GL_BGR,
						GL2.GL_UNSIGNED_BYTE, ByteBuffer
								.wrap(((DataBufferByte) screenshot.getRaster()
										.getDataBuffer()).getData()));

				GL3DState.get().checkGLErrors(
						"GL3DComponentView.afterTileRenderer");

			}
		}

		ImageUtil.flipImageVertically(screenshot);

		 //offscreenContext.release();
		// saveBufferedImage = true;
		// this.getAdapter(ViewportView.class).setViewport(defaultViewport, new
		// ChangeEvent());
		 this.canvas.display();
		viewport = null;
		//if (screenshot == null){
		//	screenshot = getBufferedImage(width, height);
		//}
		return screenshot;
	}

	public static void setTileSize(int width, int height) {
		// defaultTileWidth = width;
		// defaultTileHeight = height;
	}

	public void setOffset(Vector2dInt offset) {
		// if(this.orthoView!=null) {
		// orthoView.setOffset(offset);
		// }
	}

	public void updateMainImagePanelSize(Vector2dInt size) {
		super.updateMainImagePanelSize(size);
		this.viewportSize = size;

		// if(this.orthoView!=null) {
		// this.orthoView.updateMainImagePanelSize(size);
		// }
		if (this.viewportView != null) {
			Viewport viewport = StaticViewport.createAdaptedViewport(
					Math.max(1, size.getX()), Math.max(1, size.getY()));
			this.viewportView.setViewport(viewport, null);
		}
	}

	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
		// this.orthoView = getAdapter(GL3DOrthoView.class);
		this.viewportView = getAdapter(ViewportView.class);
	}

	private void updateViewport() {
		// this.orthoView.updateMainImagePanelSize(mainImagePanelSize);
	}

	public void viewChanged(View sender, ChangeEvent aEvent) {
		// this.saveBufferedImage = true;

		if (this.animationLock.isLocked()) {
			return;
		}

		// rebuild shaders, if necessary
		if (aEvent.reasonOccurred(ViewChainChangedReason.class)
				|| (aEvent.reasonOccurred(LayerChangedReason.class) && aEvent
						.getLastChangedReasonByType(LayerChangedReason.class)
						.getLayerChangeType() == LayerChangeType.LAYER_ADDED)) {
			rebuildShadersRequest = true;
		}
		notifyViewListeners(aEvent);
	}

	private void rebuildShaders(GL2 gl) {
		rebuildShadersRequest = false;
		shaderHelper.delAllShaderIDs(gl);

		GLFragmentShaderView fragmentView = view
				.getAdapter(GLFragmentShaderView.class);
		if (fragmentView != null) {
			// create new shader builder
			GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl.getGL2(),
					GL2.GL_FRAGMENT_PROGRAM_ARB);

			// fill with standard values
			GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
			minimalProgram.build(newShaderBuilder);

			// fill with other filters and compile
			fragmentView.buildFragmentShader(newShaderBuilder).compile();
		}

		GLVertexShaderView vertexView = view
				.getAdapter(GLVertexShaderView.class);
		if (vertexView != null) {
			// create new shader builder
			GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl.getGL2(),
					GL2.GL_VERTEX_PROGRAM_ARB);

			// fill with standard values
			GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
			minimalProgram.build(newShaderBuilder);

			// fill with other filters and compile
			vertexView.buildVertexShader(newShaderBuilder).compile();
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();
		gl.glDeleteFramebuffers(1, frameBufferObject, 0);
		gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
		gl.glDeleteRenderbuffers(1, renderBufferColor, 0);

		if (screenshot != null) {
			screenshot.flush();
			screenshot = null;
		}
		screenshotBuffer = null;
		tileRenderer = null;

	}

	public Dimension getCanavasSize() {
		return new Dimension(canvas.getWidth(), canvas.getHeight());
	}

	@Override
	public void stop() {
		animator.stop();
		defaultViewport = this.getAdapter(ViewportView.class).getViewport();

	}

	@Override
	public void start() {
		animator.start();
		this.getAdapter(ViewportView.class).setViewport(defaultViewport,
				new ChangeEvent());
	}
	
	public void setActiveLayer(GL3DImageTextureView activeLayer){
		this.activeLayer = activeLayer;
	}
	
}
