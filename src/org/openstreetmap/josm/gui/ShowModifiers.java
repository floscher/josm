package org.openstreetmap.josm.gui;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ShowModifiers extends JFrame implements AWTEventListener, Runnable {

	private JLabel ctrl, alt, shift;

	private int mouseMod = -1;
	private int wheelMod = 0;

	public ShowModifiers(int x, int y) {
		Toolkit.getDefaultToolkit().addAWTEventListener(this, 
				AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setUndecorated(true);
		setAlwaysOnTop(true);
		Box v = Box.createHorizontalBox();
		//v.add(mouse);
		JComponent mouseSpace = new JLabel();
		mouseSpace.setPreferredSize(new Dimension(63,103));
		v.add(mouseSpace);
		setContentPane(v);

		JPanel p = new JPanel(new GridLayout(3,1));
		v.add(p);
		p.add(ctrl = createLabel("Ctrl"));
		p.add(alt = createLabel("Alt"));
		p.add(shift = createLabel("Shift"));
		pack();
		setLocation(x-getWidth(),y-getHeight());
		setVisible(true);
	}

	private JLabel createLabel(String name) {
		JLabel l = new JLabel(name);
		l.setHorizontalAlignment(JLabel.CENTER);
		l.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.BLACK),
				BorderFactory.createEmptyBorder(3,3,3,3)));
		l.setFont(l.getFont().deriveFont(Font.PLAIN));
		l.setOpaque(true);
		return l;
	}

	public void eventDispatched(AWTEvent event) {
		int keyMod = -1;
		if (event instanceof InputEvent) {
			InputEvent e = (InputEvent)event;
			keyMod = e.getModifiersEx();
			ctrl.setBackground((keyMod & KeyEvent.CTRL_DOWN_MASK) != 0 ? Color.RED : null);
			alt.setBackground((keyMod & KeyEvent.ALT_DOWN_MASK) != 0 ? Color.RED : null);
			shift.setBackground((keyMod & KeyEvent.SHIFT_DOWN_MASK) != 0 ? Color.RED : null);
		}
		if (event instanceof MouseWheelEvent) {
			wheelMod = ((MouseWheelEvent)event).getWheelRotation();
			repaint();
		}
		if (event instanceof MouseEvent) {
			MouseEvent e = (MouseEvent)event;
			mouseMod = e.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK);
			repaint();
		}
		new Thread(this).start();
	}

	public static void main(String[] args) {
		new ShowModifiers(0,0);
	}

	public void run() {
		try {Thread.sleep(250);} catch (InterruptedException e) {}
		wheelMod = 0;
		Graphics g = getGraphics();
		if (g != null) {
			paint(g);
			g.dispose();
		}
	}
	
	private void paintMouse(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0,0,60,100);
		g.setColor(Color.BLACK);
		
		g.drawLine(5,5,55,5);
		g.drawLine(5,5,5,75);
		g.drawLine(55,5,55,75);
		g.drawArc(5,55,50,40,180,180);
		
		g.drawLine(5,30,55,30);
		g.drawLine(25,5,25,30);
		g.drawLine(35,5,35,30);
		g.drawLine(25,12,35,12);
		g.drawLine(25,23,35,23);
		
		if (mouseMod == -1)
			return;
		g.setColor(Color.RED);
		if ((mouseMod & MouseEvent.BUTTON1_DOWN_MASK) != 0)
			g.fillRect(6,6,19,24);
		if ((mouseMod & MouseEvent.BUTTON2_DOWN_MASK) != 0)
			g.fillRect(26,13,9,10);
		if ((mouseMod & MouseEvent.BUTTON3_DOWN_MASK) != 0)
			g.fillRect(36,6,19,24);
		
		if (wheelMod == 0)
			return;
		if (wheelMod < 0)
			g.fillRect(26,6,9,6);
		else
			g.fillRect(26,24,9,6);
	}

	@Override public void paint(Graphics g) {
		super.paint(g);
		paintMouse(g);
	}
}
