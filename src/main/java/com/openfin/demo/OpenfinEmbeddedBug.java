package com.openfin.demo;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import com.openfin.desktop.Ack;
import com.openfin.desktop.AckListener;
import com.openfin.desktop.Application;
import com.openfin.desktop.ApplicationOptions;
import com.openfin.desktop.DesktopConnection;
import com.openfin.desktop.DesktopException;
import com.openfin.desktop.DesktopStateListener;
import com.openfin.desktop.RuntimeConfiguration;
import com.openfin.desktop.Window;
import com.openfin.desktop.WindowOptions;
import com.sun.jna.Native;

public class OpenfinEmbeddedBug {

	private DesktopConnection desktopConnection;
	private JInternalFrame ofFrame;
	protected Application app;
	private Canvas canvas;

	public OpenfinEmbeddedBug() {
		JFrame frame = new JFrame("Openfin Embedded Bug");

		JMenuBar menubar = new JMenuBar();
		JMenu menu = new JMenu("Click Me");
		for (int i = 0; i < 20; i++) {
			menu.add(new JMenuItem("Menu Item " + (i + 1)));
		}
		menubar.add(menu);

		frame.setJMenuBar(menubar);

		JDesktopPane desktop = new JDesktopPane();
		frame.getContentPane().add(desktop);

		for (int i = 0; i < 5; i++) {
			JInternalFrame iFrame = new JInternalFrame("Internal Frame " + (i + 1), true, true, true, true);
			iFrame.setSize(300, 200);
			iFrame.setLocation(10 + 10 * i, 10 + 10 * i);
			iFrame.setVisible(true);
			desktop.add(iFrame);
		}

		// add of embedded window.
		ofFrame = new JInternalFrame("Openfin Embedded", true, false, true, true);
		ofFrame.setSize(400, 300);
		ofFrame.setLocation(0, 0);
		ofFrame.setVisible(true);

		canvas = new Canvas();
		canvas.setBackground(Color.RED);
		canvas.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				super.componentResized(event);
				Dimension newSize = event.getComponent().getSize();
				try {
					if (app != null) {
						app.getWindow().embedComponentSizeChange((int) newSize.getWidth(), (int) newSize.getHeight());
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		ofFrame.getContentPane().add(canvas);

		desktop.add(ofFrame);

		frame.setSize(640, 480);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);

		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				try {
					app.close(true, new AckListener() {
						@Override
						public void onSuccess(Ack ack) {
							try {
								desktopConnection.disconnect();
								System.exit(0);
							}
							catch (DesktopException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						@Override
						public void onError(Ack ack) {
							// TODO Auto-generated method stub
							
						}
					});
				}
				catch (DesktopException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		startOpenfinRuntime();
	}

	private void startOpenfinRuntime() {
		try {
			desktopConnection = new DesktopConnection("OpenfinEmbeddedBug");
			DesktopStateListener listener = new DesktopStateListener() {
				@Override
				public void onReady() {
					// let's create an application
					ApplicationOptions appOpt = new ApplicationOptions("appGoogle", "appGoogle",
							"https://www.google.com");
					WindowOptions mainWindowOptions = new WindowOptions();
					mainWindowOptions.setAutoShow(false);
					mainWindowOptions.setDefaultLeft(10);
					mainWindowOptions.setDefaultTop(50);
					mainWindowOptions.setShowTaskbarIcon(true);
					mainWindowOptions.setSaveWindowState(false); // set to false so all windows start at same initial
																	// positions for each run
					mainWindowOptions.setFrame(false);
					mainWindowOptions.setContextMenu(true);
					mainWindowOptions.setResizeRegionSize(0); // need this to turn off resize region for embedded
																// (child) window
					appOpt.setMainWindowOptions(mainWindowOptions);
					app = new Application(appOpt, desktopConnection, new AckListener() {
						@Override
						public void onSuccess(Ack ack) {
							// app created, let's run the app and embed it into the internal frame.
							app.run(new AckListener() {
								@Override
								public void onSuccess(Ack ack) {
									Window html5Wnd = app.getWindow();
									long parentHWndId = Native.getComponentID(canvas);
									html5Wnd.embedInto(parentHWndId, canvas.getWidth(), canvas.getHeight(),
											new AckListener() {
												@Override
												public void onSuccess(Ack ack) {
												}

												@Override
												public void onError(Ack ack) {
												}
											});
								}

								@Override
								public void onError(Ack ack) {
								}
							});
						}

						@Override
						public void onError(Ack ack) {
						}
					});
				}

				@Override
				public void onClose(String error) {
				}

				@Override
				public void onError(String reason) {

				}

				@Override
				public void onMessage(String message) {
				}

				@Override
				public void onOutgoingMessage(String message) {
				}
			};
			RuntimeConfiguration configuration = new RuntimeConfiguration();
			configuration.setAdditionalRuntimeArguments(" --v=1 "); // enable additional logging from Runtime
			String desktopVersion = java.lang.System.getProperty("com.openfin.demo.version");
			if (desktopVersion == null) {
				desktopVersion = "stable";
			}
			configuration.setRuntimeVersion(desktopVersion);
			desktopConnection.connect(configuration, listener, 60);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				new OpenfinEmbeddedBug();
			}
		});
	}

}
