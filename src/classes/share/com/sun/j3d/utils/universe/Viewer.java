/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 */

package com.sun.j3d.utils.universe;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.net.URL;

import javax.media.j3d.AudioDevice;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.PhysicalEnvironment;
import javax.media.j3d.View;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.sun.j3d.audioengines.AudioEngine3DL2;

/**
 * The Viewer class holds all the information that describes the physical
 * and virtual "presence" in the Java 3D universe.  The Viewer object
 * consists of:
 * <UL>
 * <LI>Physical Objects</LI>
 *  <UL>
 *   <LI>Canvas3D's - used to render with.</LI>
 *   <LI>PhysicalEnvironment - holds characteristics of the hardware platform
 *    being used to render on.</LI>
 *   <LI>PhysicalBody -  holds the physical characteristics and personal
 *    preferences of the person who will be viewing the Java 3D universe.</LI>
 *  </UL>
 * <LI>Virtual Objects</LI>
 *  <UL>
 *   <LI>View - the Java 3D View object.</LI>
 *   <LI>ViewerAvatar - the geometry that is used by Java 3D to represent the
 *    person viewing the Java 3D universe.</LI>
 *  </UL>
 * </UL>
 * If the Viewer object is created without any Canvas3D's, or indirectly
 * through a configuration file, it will create the Canvas3D's as needed.
 * The default Viewer creates one Canvas3D.  If the Viewer object creates
 * the Canvas3D's, it will also create a JPanel and JFrame for each Canvas3D.
 *
 * Dynamic video resize is a new feature in Java 3D 1.3.1.
 * This feature provides a means for doing swap synchronous resizing
 * of the area that is to be magnified (or passed through) to the
 * output video resolution. This functionality allows an application
 * to draw into a smaller viewport in the framebuffer in order to reduce
 * the time spent doing pixel fill. The reduced size viewport is then
 * magnified up to the video output resolution using the SUN_video_resize
 * extension. This extension is only implemented in XVR-4000 and later
 * hardware with back end video out resizing capability.
 *
 * If video size compensation is enable, the line widths, point sizes and pixel
 * operations will be scaled internally with the resize factor to approximately
 * compensate for video resizing. The location of the pixel ( x, y ) in the
 * resized framebuffer = ( floor( x * factor + 0.5 ), floor( y * factor + 0.5 ) )
 *
 * <p>
 * @see Canvas3D
 * @see PhysicalEnvironment
 * @see PhysicalBody
 * @see View
 * @see ViewerAvatar
 */
public class Viewer
{
	private static final boolean debug = false;
	private static PhysicalBody physicalBody = null;
	private static PhysicalEnvironment physicalEnvironment = null;
	private View view = null;
	private ViewerAvatar avatar = null;
	private Canvas3D[] canvases = null;
	private JFrame[] j3dJFrames = null;
	private JPanel[] j3dJPanels = null;
	private Window[] j3dWindows = null;
	private ViewingPlatform viewingPlatform = null;

	/**
	 * Creates a default viewer object. The default values are used to create
	 * the PhysicalBody and PhysicalEnvironment.  A single RGB, double buffered
	 * and depth buffered Canvas3D object is created.  The View is created
	 * with a front clip distance of 0.1f and a back clip distance of 10.0f.
	 */
	public Viewer()
	{
		// Call main constructor with default values.
		this(null, null, null, true);
	}

	/**
	 * Creates a default viewer object. The default values are used to create
	 * the PhysicalBody and PhysicalEnvironment.  The View is created
	 * with a front clip distance of 0.1f and a back clip distance of 10.0f.
	 *
	 * @param userCanvas the Canvas3D object to be used for rendering;
	 *  if this is null then a single RGB, double buffered and depth buffered
	 *  Canvas3D object is created
	 * @since Java3D 1.1
	 */
	public Viewer(Canvas3D userCanvas)
	{
		// Call main constructor.
		this(userCanvas == null ? null : new Canvas3D[] { userCanvas }, null, null, true);
	}

	/**
	 * Creates a default viewer object. The default values are used to create
	 * the PhysicalBody and PhysicalEnvironment.  The View is created
	 * with a front clip distance of 0.1f and a back clip distance of 10.0f.
	 *
	 * @param userCanvases the Canvas3D objects to be used for rendering;
	 *  if this is null then a single RGB, double buffered and depth buffered
	 *  Canvas3D object is created
	 * @since Java3D 1.3
	 */
	public Viewer(Canvas3D[] userCanvases)
	{
		this(userCanvases, null, null, true);
	}

	/**
	 * Creates a viewer object. The Canvas3D objects, PhysicalEnvironment, and
	 * PhysicalBody are taken from the arguments.
	 *
	 * @param userCanvases the Canvas3D objects to be used for rendering;
	 *  if this is null then a single RGB, double buffered and depth buffered
	 *  Canvas3D object is created
	 * @param userBody the PhysicalBody to use for this Viewer; if it is
	 *  null, a default PhysicalBody object is created
	 * @param userEnvironment the PhysicalEnvironment to use for this Viewer;
	 *  if it is null, a default PhysicalEnvironment object is created
	 * @param setVisible determines if the Frames should be set to visible once created
	 * @since Java3D 1.3
	 */
	public Viewer(Canvas3D[] userCanvases, PhysicalBody userBody, PhysicalEnvironment userEnvironment, boolean setVisible)
	{

		if (userBody == null)
		{
			physicalBody = new PhysicalBody();
		}
		else
		{
			physicalBody = userBody;
		}

		if (userEnvironment == null)
		{
			physicalEnvironment = new PhysicalEnvironment();
		}
		else
		{
			physicalEnvironment = userEnvironment;
		}

		// Create Canvas3D object if none was passed in.
		if (userCanvases == null)
		{
			canvases = new Canvas3D[1];
			canvases[0] = new Canvas3D();
			//canvases[0].setFocusable(true);
			createFramesAndPanels(setVisible);
		}
		else
		{
			canvases = new Canvas3D[userCanvases.length];
			for (int i = 0; i < userCanvases.length; i++)
			{
				canvases[i] = userCanvases[i];
				//canvases[i].setFocusable(true);
			}
		}

		// Create a View and attach the Canvas3D and the physical
		// body and environment to the view.
		view = new View();

		// Fix to issue 424
		view.setUserHeadToVworldEnable(true);

		for (int i = 0; i < canvases.length; i++)
		{
			view.addCanvas3D(canvases[i]);
		}
		view.setPhysicalBody(physicalBody);
		view.setPhysicalEnvironment(physicalEnvironment);
	}

	/**
	 * Creates a default Viewer object. The default values are used to create
	 * the PhysicalEnvironment and PhysicalBody.  A single RGB, double buffered
	 * and depth buffered Canvas3D object is created.  The View is created
	 * with a front clip distance of 0.1f and a back clip distance of 10.0f.
	 *
	 * @param userConfig the URL of the user configuration file used to
	 *  initialize the PhysicalBody object; this is always ignored
	 * @since Java3D 1.1
	 * @deprecated create a ConfiguredUniverse to use a configuration file
	 */
	public Viewer(URL userConfig)
	{
		// Call main constructor.
		this(null, userConfig);
	}

	/**
	 * Creates a default viewer object. The default values are used to create
	 * the PhysicalEnvironment and PhysicalBody.  The View is created
	 * with a front clip distance of 0.1f and a back clip distance of 10.0f.
	 *
	 * @param userCanvas the Canvas3D object to be used for rendering;
	 *  if this is null then a single RGB, double buffered and depth buffered
	 *  Canvas3D object is created
	 * @param userConfig the URL of the user configuration file used to
	 *  initialize the PhysicalBody object; this is always ignored
	 * @since Java3D 1.1
	 * @deprecated create a ConfiguredUniverse to use a configuration file
	 */
	public Viewer(Canvas3D userCanvas, URL userConfig)
	{
		// Only one PhysicalBody per Universe.
		if (physicalBody == null)
		{
			physicalBody = new PhysicalBody();
		}

		// Only one PhysicalEnvironment per Universe.
		if (physicalEnvironment == null)
		{
			physicalEnvironment = new PhysicalEnvironment();
		}

		// Create Canvas3D object if none was passed in.
		if (userCanvas == null)
		{
			canvases = new Canvas3D[1];
			canvases[0] = new Canvas3D();
			createFramesAndPanels(true);
		}
		else
		{
			canvases = new Canvas3D[1];
			canvases[0] = userCanvas;
		}

		//canvases[0].setFocusable(true);

		// Create a View and attach the Canvas3D and the physical
		// body and environment to the view.
		view = new View();

		// Fix to issue 424
		view.setUserHeadToVworldEnable(true);

		view.addCanvas3D(canvases[0]);
		view.setPhysicalBody(physicalBody);
		view.setPhysicalEnvironment(physicalEnvironment);
	}

	// Create the JFrames and JPanels for application-supplied Canvas3D
	// objects.
	private void createFramesAndPanels(boolean setVisible)
	{
		j3dJFrames = new JFrame[canvases.length];
		j3dJPanels = new JPanel[canvases.length];
		j3dWindows = new Window[canvases.length];

		for (int i = 0; i < canvases.length; i++)
		{
			j3dWindows[i] = j3dJFrames[i] = new JFrame();
			j3dJFrames[i].getContentPane().setLayout(new BorderLayout());
			j3dJFrames[i].setSize(256, 256);

			// Put the Canvas3D into a JPanel.
			j3dJPanels[i] = new JPanel();
			j3dJPanels[i].setLayout(new BorderLayout());
			
			//FIXME: this is now buggered!
			
			//j3dJPanels[i].add("Center", canvases[i]);
			j3dJFrames[i].getContentPane().add("Center", j3dJPanels[i]);
			if (setVisible)
			{
				j3dJFrames[i].setVisible(true);
			}
			addWindowCloseListener(j3dJFrames[i]);
		}
	}

	/**
	 * Call setVisible() on all Window components created by this Viewer.
	 *
	 * @param visible boolean to be passed to the setVisible() calls on the
	 *  Window components created by this Viewer
	 * @since Java3D 1.3
	 */
	public void setVisible(boolean visible)
	{
		for (int i = 0; i < j3dWindows.length; i++)
		{
			j3dWindows[i].setVisible(visible);
		}
	}

	/**
	 * Returns the View object associated with the Viewer object.
	 *
	 * @return The View object of this Viewer.
	 */
	public View getView()
	{
		return view;
	}

	/**
	 * Set the ViewingPlatform object used by this Viewer.
	 *
	 * @param platform The ViewingPlatform object to set for this
	 *  Viewer object.  Use null to unset the current value and
	 *  not assign assign a new ViewingPlatform object.
	 */
	public void setViewingPlatform(ViewingPlatform platform)
	{
		if (viewingPlatform != null)
		{
			viewingPlatform.removeViewer(this);
		}

		viewingPlatform = platform;

		if (platform != null)
		{
			view.attachViewPlatform(platform.getViewPlatform());
			platform.addViewer(this);

			if (avatar != null)
				viewingPlatform.setAvatar(this, avatar);
		}
		else
			view.attachViewPlatform(null);
	}

	/**
	 * Get the ViewingPlatform object used by this Viewer.
	 *
	 * @return The ViewingPlatform object used by this
	 *  Viewer object.
	 */
	public ViewingPlatform getViewingPlatform()
	{
		return viewingPlatform;
	}

	/**
	 * Sets the geometry to be associated with the viewer's avatar.  The
	 * avatar is the geometry used to represent the viewer in the virtual
	 * world.
	 *
	 * @param avatar The geometry to associate with this Viewer object.
	 *  Passing in null will cause any geometry associated with the Viewer
	 *  to be removed from the scen graph.
	 */
	public void setAvatar(ViewerAvatar avatar)
	{
		// Just return if trying to set the same ViewerAvatar object.
		if (this.avatar == avatar)
			return;

		this.avatar = avatar;
		if (viewingPlatform != null)
			viewingPlatform.setAvatar(this, this.avatar);
	}

	/**
	 * Gets the geometry associated with the viewer's avatar.  The
	 * avatar is the geometry used to represent the viewer in the virtual
	 * world.
	 *
	 * @return The root of the scene graph that is used to represent the
	 *  viewer's avatar.
	 */
	public ViewerAvatar getAvatar()
	{
		return avatar;
	}

	/**
	 * Returns the PhysicalBody object associated with the Viewer object.
	 *
	 * @return A reference to the PhysicalBody object.
	 */
	public PhysicalBody getPhysicalBody()
	{
		return physicalBody;
	}

	/**
	 * Returns the PhysicalEnvironment object associated with the Viewer
	 * object.
	 *
	 * @return A reference to the PhysicalEnvironment object.
	 */
	public PhysicalEnvironment getPhysicalEnvironment()
	{
		return physicalEnvironment;
	}

	/**
	 * Returns the 0th Canvas3D object associated with this Viewer object
	 *
	 * @return a reference to the 0th Canvas3D object associated with this
	 *  Viewer object
	 * @since Java3D 1.3
	 */
	public Canvas3D getCanvas3D()
	{
		return canvases[0];
	}

	/**
	 * Returns the Canvas3D object at the specified index associated with
	 * this Viewer object.
	 *
	 * @param canvasNum the index of the Canvas3D object to retrieve;
	 *  if there is no Canvas3D object for the given index, null is returned
	 * @return a reference to a Canvas3D object associated with this
	 *  Viewer object
	 * @since Java3D 1.3
	 */
	public Canvas3D getCanvas3D(int canvasNum)
	{
		if (canvasNum > canvases.length)
		{
			return null;
		}
		return canvases[canvasNum];
	}

	/**
	 * Returns all the Canvas3D objects associated with this Viewer object.
	 *
	 * @return an array of references to the Canvas3D objects associated with
	 *  this Viewer object
	 * @since Java3D 1.3
	 */
	public Canvas3D[] getCanvas3Ds()
	{
		Canvas3D[] ret = new Canvas3D[canvases.length];
		for (int i = 0; i < canvases.length; i++)
		{
			ret[i] = canvases[i];
		}
		return ret;
	}

	/**
	 * Returns the canvas associated with this Viewer object.
	 * @deprecated superceded by getCanvas3D()
	 */
	public Canvas3D getCanvases()
	{
		return getCanvas3D();
	}

	/**
	 * This method is no longer supported since Java 3D 1.3.
	 * @exception UnsupportedOperationException if called.
	 * @deprecated AWT Frame components are no longer created by the
	 *  Viewer class.
	 */
	public Frame getFrame()
	{
		throw new UnsupportedOperationException("AWT Frame components are not created by the Viewer class");
	}

	/**
	 * Returns the JFrame object created by this Viewer object at the
	 * specified index.  If a Viewer is constructed without any Canvas3D
	 * objects then the Viewer object will create a Canva3D object, a JPanel
	 * containing the Canvas3D object, and a JFrame to place the JPanel in.
	 * <p>
	 * NOTE: When running under JDK 1.4 or newer, the JFrame always directly
	 * contains the JPanel which contains the Canvas3D.  When running under
	 * JDK 1.3.1 and creating a borderless full screen through a configuration
	 * file, the JFrame will instead contain a JWindow which will contain the
	 * JPanel and Canvas3D.
	 * <p>
	 * @param frameNum the index of the JFrame object to retrieve;
	 *  if there is no JFrame object for the given index, null is returned
	 * @return a reference to JFrame object created by this Viewer object
	 * @since Java3D 1.3
	 */
	public JFrame getJFrame(int frameNum)
	{
		if (j3dJFrames == null || frameNum > j3dJFrames.length)
		{
			return (null);
		}
		return j3dJFrames[frameNum];
	}

	/**
	 * Returns all the JFrames created by this Viewer object.  If a Viewer is
	 * constructed without any Canvas3D objects then the Viewer object will
	 * create a Canva3D object, a JPanel containing the Canvas3D object, and a
	 * JFrame to place the JPanel in.<p>
	 *
	 * NOTE: When running under JDK 1.4 or newer, the JFrame always directly
	 * contains the JPanel which contains the Canvas3D.  When running under
	 * JDK 1.3.1 and creating a borderless full screen through a configuration
	 * file, the JFrame will instead contain a JWindow which will contain the
	 * JPanel and Canvas3D.<p>
	 *
	 * @return an array of references to the JFrame objects created by
	 *  this Viewer object, or null if no JFrame objects were created
	 * @since Java3D 1.3
	 */
	public JFrame[] getJFrames()
	{
		if (j3dJFrames == null)
			return null;

		JFrame[] ret = new JFrame[j3dJFrames.length];
		for (int i = 0; i < j3dJFrames.length; i++)
		{
			ret[i] = j3dJFrames[i];
		}
		return ret;
	}

	/**
	 * This method is no longer supported since Java 3D 1.3.
	 * @exception UnsupportedOperationException if called.
	 * @deprecated AWT Panel components are no longer created by the
	 * Viewer class.
	 */
	public Panel getPanel()
	{
		throw new UnsupportedOperationException("AWT Panel components are not created by the Viewer class");
	}

	/**
	 * Returns the JPanel object created by this Viewer object at the
	 * specified index.  If a Viewer is constructed without any Canvas3D
	 * objects then the Viewer object will create a Canva3D object and a
	 * JPanel into which to place the Canvas3D object.
	 *
	 * @param panelNum the index of the JPanel object to retrieve;
	 *  if there is no JPanel object for the given index, null is returned
	 * @return a reference to a JPanel object created by this Viewer object
	 * @since Java3D 1.3
	 */
	public JPanel getJPanel(int panelNum)
	{
		if (j3dJPanels == null || panelNum > j3dJPanels.length)
		{
			return (null);
		}
		return j3dJPanels[panelNum];
	}

	/**
	 * Returns all the JPanel objects created by this Viewer object.  If a
	 * Viewer is constructed without any Canvas3D objects then the Viewer
	 * object will create a Canva3D object and a JPanel into which to place
	 * the Canvas3D object.
	 *
	 * @return an array of references to the JPanel objects created by
	 *  this Viewer object, or null or no JPanel objects were created
	 * @since Java3D 1.3
	 */
	public JPanel[] getJPanels()
	{
		if (j3dJPanels == null)
			return null;

		JPanel[] ret = new JPanel[j3dJPanels.length];
		for (int i = 0; i < j3dJPanels.length; i++)
		{
			ret[i] = j3dJPanels[i];
		}
		return ret;
	}

	/**
	 * Used to create and initialize a default AudioDevice3D used for sound
	 * rendering.
	 *
	 * @return reference to created AudioDevice, or null if error occurs.
	 */
	public AudioDevice createAudioDevice()
	{
		if (physicalEnvironment == null)
		{
			System.err.println("Java 3D: createAudioDevice: physicalEnvironment is null");
			return null;
		}

		try
		{
			String audioDeviceClassName = (String) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
				@Override
				public Object run()
				{
					return System.getProperty("j3d.audiodevice");
				}
			});

			if (audioDeviceClassName == null)
			{
				throw new UnsupportedOperationException("No AudioDevice specified");
			}

			// Issue 341: try the current class loader first before trying the
			// system class loader
			Class audioDeviceClass = null;
			try
			{
				audioDeviceClass = Class.forName(audioDeviceClassName);
			}
			catch (ClassNotFoundException ex)
			{
				// Ignore excpetion and try system class loader
			}

			if (audioDeviceClass == null)
			{
				ClassLoader audioDeviceClassLoader = (ClassLoader) java.security.AccessController
						.doPrivileged(new java.security.PrivilegedAction() {
							@Override
							public Object run()
							{
								return ClassLoader.getSystemClassLoader();
							}
						});

				if (audioDeviceClassLoader == null)
				{
					throw new IllegalStateException("System ClassLoader is null");
				}

				audioDeviceClass = Class.forName(audioDeviceClassName, true, audioDeviceClassLoader);
			}

			Class physEnvClass = PhysicalEnvironment.class;
			Constructor audioDeviceConstructor = audioDeviceClass.getConstructor(new Class[] { physEnvClass });
			PhysicalEnvironment[] args = new PhysicalEnvironment[] { physicalEnvironment };
			AudioEngine3DL2 mixer = (AudioEngine3DL2) audioDeviceConstructor.newInstance((Object[]) args);
			mixer.initialize();
			return mixer;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			physicalEnvironment.setAudioDevice(null);
			System.err.println("Java 3D: audio is disabled");
			return null;
		}
	}

	/**
	 * Returns the Universe to which this Viewer is attached
	 *
	 * @return the Universe to which this Viewer is attached
	 * @since Java 3D 1.3
	 */
	public SimpleUniverse getUniverse()
	{
		return getViewingPlatform().getUniverse();
	}

	/*
	 * Exit if run as an application
	 */
	void addWindowCloseListener(Window win)
	{
		SecurityManager sm = System.getSecurityManager();
		boolean doExit = true;

		if (sm != null)
		{
			try
			{
				sm.checkExit(0);
			}
			catch (SecurityException e)
			{
				doExit = false;
			}
		}
		final boolean _doExit = doExit;

		win.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent winEvent)
			{
				Window w = winEvent.getWindow();
				w.setVisible(false);
				try
				{
					w.dispose();
				}
				catch (IllegalStateException e)
				{
				}
				if (_doExit)
				{
					System.exit(0);
				}
			}
		});
	}
}
