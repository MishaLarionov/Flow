package login;

import gui.FlowClient;
import gui.PanelManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import message.Data;
import shared.Communicator;

public class LoginPane extends JPanel {
    private PanelManager panMan;
    private JPasswordField passwordEntry;
    private JButton loginButton;

    // Pan Man! https://i.imgur.com/19iZW9K.png

    public LoginPane(PanelManager panMan) {
	setBackground(Color.WHITE);

	this.panMan = panMan;
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

	java.awt.Component verticalStrut_4 = Box.createVerticalStrut(20);
	add(verticalStrut_4);
	// TODO wipe password on login and create new account
	// TODO add background picture

	JLabel title = new JLabel();
	title.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
	add(title);
	try {
	    title.setIcon(new ImageIcon(ImageIO.read(new File("images/flow.png")).getScaledInstance(414, 128, Image.SCALE_SMOOTH)));
	} catch (IOException e1) {
	    e1.printStackTrace();
	}

	java.awt.Component verticalStrut_3 = Box.createVerticalStrut(20);
	add(verticalStrut_3);

	JLabel usernamePrompt = new JLabel("Username");
	add(usernamePrompt);
	usernamePrompt.setSize(128, 28);
	usernamePrompt.setAlignmentX(CENTER_ALIGNMENT);

	UsernameBox usernameEntry = new UsernameBox();
	usernameEntry.setMaximumSize(new Dimension(128, 24));
	add(usernameEntry);

	java.awt.Component verticalStrut = Box.createVerticalStrut(20);
	add(verticalStrut);

	JLabel passwordPrompt = new JLabel("Password");
	add(passwordPrompt);
	passwordPrompt.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

	passwordEntry = new JPasswordField();
	passwordEntry.setMaximumSize(new Dimension(128, 24));
	add(passwordEntry);
	passwordEntry.addFocusListener(new FocusListener() {

	    @Override
	    public void focusLost(FocusEvent e) {
		// nothing
	    }

	    @Override
	    public void focusGained(FocusEvent e) {
		passwordEntry.setText("");
	    }
	});
	passwordEntry.setToolTipText("Your Flow password");
	passwordEntry.addKeyListener(new KeyListener() {

	    @Override
	    public void keyReleased(KeyEvent e) {
		// nothing
	    }

	    @Override
	    public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == KeyEvent.VK_ENTER)
		    loginButton.doClick();
	    }

	    @Override
	    public void keyPressed(KeyEvent e) {
		// nothing
	    }
	});

	java.awt.Component verticalStrut_1 = Box.createVerticalStrut(20);
	add(verticalStrut_1);

	loginButton = new JButton("Login");
	add(loginButton);
	loginButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

	java.awt.Component verticalStrut_2 = Box.createVerticalStrut(20);
	add(verticalStrut_2);

	JButton createAccountButton = new JButton("<html>No Account?<br>Create one!</html>");
	createAccountButton.setPreferredSize(new Dimension(128, 32));
	createAccountButton.setMinimumSize(new Dimension(32, 2));
	createAccountButton.setMaximumSize(new Dimension(128, 32));
	add(createAccountButton);
	createAccountButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
	createAccountButton.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		LoginPane.this.panMan.switchToCreateAccount();
	    }
	});
	loginButton.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (FlowClient.NETWORK) {
		    if (usernameEntry.getText().trim().length() >= 16) {
			JOptionPane.showConfirmDialog(null, "The username is too long.\nUsernames have a limit of 16 characters.", "Invalid username", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			return;
		    } else if (usernameEntry.getText().trim().equals("Username")) {
			JOptionPane.showConfirmDialog(null, "Please enter a username.", "Invalid username", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			return;

		    }

		    Data usernamePass = new Data("login");
		    usernamePass.put("username", usernameEntry.getText().trim());
		    usernamePass.put("password", String.copyValueOf(passwordEntry.getPassword()));

		    Data reply = Communicator.communicate(usernamePass);
		    if (reply == null) {
			JOptionPane.showConfirmDialog(null, "The server is currently offline. Please try again at another time.", "Server under maintenance", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			return;
		    }
		    String status = reply.get("status", String.class);
		    switch (status) {
		    case "USERNAME_DOES_NOT_EXIST":
			JOptionPane.showConfirmDialog(null, "The username does not exist.\nPlease enter a username that is valid, or create a new account.", "Invalid username", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			return;
		    case "PASSWORD_INCORRECT":
			JOptionPane.showConfirmDialog(null, "Whoops! Your password does not match the one we don't have. Try again.", "Incorrect password", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			return;
		    case "INVALID_CREDENTIALS":
			JOptionPane.showConfirmDialog(null, "Whoops! Your Your credentials are incorrect.\nTry again.", "Invalid credentials", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			return;
		    case "OK":
			LoginPane.this.panMan.switchToEditor();
			Communicator.setSessionID(reply.get("session_id", UUID.class));
			LoginPane.this.panMan.getEditPane().getDocTree().refreshProjectList();
			LoginPane.this.panMan.getEditPane().getDocTree().expandRow(0);
			LoginPane.this.panMan.getHistoryPane().getTree().refreshProjectList();
			LoginPane.this.panMan.getHistoryPane().getTree().expandRow(0);
			Communicator.setUsername(usernameEntry.getText().trim());
			return;
		    default:
			return;
		    }
		}

	    }
	});
    }

    public void resetPassFields() {
	passwordEntry.setText("");
    }
}
