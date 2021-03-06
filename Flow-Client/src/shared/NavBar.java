package shared;

import gui.FlowClient;
import gui.PanelManager;

import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

public class NavBar extends JToolBar {

    private PanelManager manager;

    public static final byte EDIT = 71;
    public final static byte DEBUG = -18;
    public static final byte HISTORY = 0;
    public static final byte SETTINGS = -35;

    private EditButton editButton;
    private DebugButton debugButton;
    private HistoryButton historyButton;
    private SettingsButton settingsButton;

    public NavBar(PanelManager panMan) {
	manager = panMan;
	setBorder(FlowClient.EMPTY_BORDER);
	setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

	editButton = new EditButton();
	debugButton = new DebugButton();
	historyButton = new HistoryButton();
	settingsButton = new SettingsButton();

	add(editButton);
	add(debugButton);
	add(historyButton);
	add(settingsButton);
	addSeparator();

	setFloatable(false);
	setRollover(true);
    }

    public void disableButton(byte button) {
	switch (button) {
	case EDIT:
	    editButton.setEnabled(false);
	    return;
	case DEBUG:
	    debugButton.setEnabled(false);
	    return;
	case HISTORY:
	    historyButton.setEnabled(false);
	    return;
	case SETTINGS:
	    settingsButton.setEnabled(false);
	    return;
	}
    }

    private class EditButton extends JButton {

	private EditButton() {
	    setToolTipText("Switch to the editing view");
	    setBorder(FlowClient.EMPTY_BORDER);
	    try {
		setIcon(new ImageIcon(ImageIO.read(
			new File("images/editWindow.png")).getScaledInstance(
			FlowClient.BUTTON_ICON_SIZE,
			FlowClient.BUTTON_ICON_SIZE, Image.SCALE_SMOOTH)));
	    } catch (IOException e1) {
		e1.printStackTrace();
	    }
	    setFocusable(false);
	    setBorder(FlowClient.EMPTY_BORDER);
	    addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    manager.switchToEditor();
		}
	    });
	}
    }

    private class DebugButton extends JButton {

	private DebugButton() {
	    setToolTipText("Switch to the debug view");
	    setBorder(FlowClient.EMPTY_BORDER);
	    try {
		setIcon(new ImageIcon(ImageIO.read(
			new File("images/debugWindow.png")).getScaledInstance(
			FlowClient.BUTTON_ICON_SIZE,
			FlowClient.BUTTON_ICON_SIZE, Image.SCALE_SMOOTH)));
	    } catch (IOException e1) {
		e1.printStackTrace();
	    }
	    setFocusable(false);
	    setBorder(FlowClient.EMPTY_BORDER);
	    addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    manager.switchToDebug();
		    // TODO if the last open window is the editor, then get the
		    // currently open file in the editor and open it in the
		    // debug's tab view, and switch to that tab.
		}
	    });
	}
    }

    private class HistoryButton extends JButton {

	private HistoryButton() {
	    setToolTipText("Switch to the version history view");
	    setBorder(FlowClient.EMPTY_BORDER);
	    try {
		setIcon(new ImageIcon(
			ImageIO.read(new File("images/historyWindow.png"))
				.getScaledInstance(FlowClient.BUTTON_ICON_SIZE,
					FlowClient.BUTTON_ICON_SIZE,
					Image.SCALE_SMOOTH)));
	    } catch (IOException e1) {
		e1.printStackTrace();
	    }
	    setFocusable(false);
	    setBorder(FlowClient.EMPTY_BORDER);
	    addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    manager.switchToHistory();
		    // TODO if the last open window is the editor, then get the
		    // currently open file in the editor and open it in the
		    // debug's tab view, and switch to that tab.
		}
	    });
	}
    }

    private class SettingsButton extends JButton {

	private SettingsButton() {
	    setToolTipText("Switch to the settings view");
	    setBorder(FlowClient.EMPTY_BORDER);
	    try {
		setIcon(new ImageIcon(
			ImageIO.read(new File("images/settingsWindow.png"))
				.getScaledInstance(FlowClient.BUTTON_ICON_SIZE,
					FlowClient.BUTTON_ICON_SIZE,
					Image.SCALE_SMOOTH)));
	    } catch (IOException e1) {
		e1.printStackTrace();
	    }
	    setFocusable(false);
	    setBorder(FlowClient.EMPTY_BORDER);
	    addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    manager.switchToSettings();
		}
	    });
	}
    }

}
