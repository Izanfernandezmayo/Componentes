package com.mycompany.componentes;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.awt.geom.*;

/**
 * MiniGameComponents.java
 * Programa demo con componentes personalizados, timers y colisiones.
 */
public class MiniGameComponents {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MiniGameFrame().setVisible(true));
    }
}

/* --------------------- FRAME PRINCIPAL --------------------- */
class MiniGameFrame extends JFrame {
    CardLayout cards = new CardLayout();
    JPanel cardPanel = new JPanel(cards);
    MainMenuPanel mainMenu;
    GamePanel gamePanel;
    JMenuBar menuBar;

    MiniGameFrame() {
        setTitle("Componentes personalizados - MiniJuego");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        mainMenu = new MainMenuPanel(this);
        gamePanel = new GamePanel(this);

        cardPanel.add(mainMenu, "MENU");
        cardPanel.add(gamePanel, "GAME");

        setJMenuBar(createMenuBar());
        add(cardPanel);
    }

    JMenuBar createMenuBar() {
        menuBar = new JMenuBar();
        JMenu game = new JMenu("Juego");
        ColorMenuItem changeColor = new ColorMenuItem("Cambiar tema");
        changeColor.addActionListener(e -> gamePanel.toggleTheme());
        JMenuItem exit = new CustomMenuItem("Salir");
        exit.addActionListener(e -> System.exit(0));
        game.add(changeColor);
        game.addSeparator();
        game.add(exit);

        JMenu help = new JMenu("Ayuda");
        JMenuItem about = new CustomMenuItem("Acerca de...");
        about.addActionListener(e -> {
            InfoDialog d = new InfoDialog(this, "MiniJuego - Demo", false,
                    "Juego de ejemplo con componentes personalizados.\nUsa flechas/WASD para moverte. P para pausar.");
            d.setVisible(true);
        });
        help.add(about);

        menuBar.add(game);
        menuBar.add(help);
        return menuBar;
    }

    void showGame(String playerName) {
        gamePanel.startNewGame(playerName);
        cards.show(cardPanel, "GAME");
        gamePanel.requestFocusInWindow();
    }

    void showMenu() {
        cards.show(cardPanel, "MENU");
    }
}

/* --------------------- PANEL MENU --------------------- */
class MainMenuPanel extends JPanel {
    MiniGameFrame parent;
    NameField nameField;
    JList<String> powerList;

    MainMenuPanel(MiniGameFrame p) {
        this.parent = p;
        setLayout(null);
        setBackground(new Color(40, 44, 52));

        JLabel title = new JLabel("MINI COMPONENTS GAME");
        title.setBounds(50, 30, 600, 50);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        add(title);

        nameField = new NameField(20);
        nameField.setBounds(50, 110, 300, 36);
        nameField.setPlaceholder("Tu nombre...");
        add(nameField);

        CustomButton startBtn = new CustomButton("Jugar");
        startBtn.setBounds(370, 110, 140, 36);
        startBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Jugador";
            parent.showGame(name);
        });
        add(startBtn);

        DefaultListModel<String> lm = new DefaultListModel<>();
        lm.addElement("Power: Vida extra");
        lm.addElement("Power: Escudo");
        lm.addElement("Power: Velocidad");
        powerList = new JList<>(lm);
        powerList.setCellRenderer(new IconListRenderer());
        JScrollPane sp = new JScrollPane(powerList);
        sp.setBounds(50, 170, 300, 140);
        add(sp);

        CustomButton credits = new CustomButton("Créditos");
        credits.setBounds(50, 330, 120, 36);
        credits.addActionListener(e -> {
            InfoDialog dlg = new InfoDialog(parent, "Créditos", false,
                    "Repositorio de prácticas - Curso\nComponentes personalizados, timers, colisiones.");
            dlg.setVisible(true);
        });
        add(credits);

        Sprite logo = new Sprite(80, 360, 96, 96, Color.CYAN, true);
        add(logo);

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                logo.setPulsing(true);
            }
        });
    }
}

/* --------------------- PANEL DE JUEGO --------------------- */
class GamePanel extends JPanel implements KeyListener {
    MiniGameFrame parent;
    String playerName = "Jugador";
    PlayerSprite player;
    java.util.List<EnemySprite> enemies = Collections.synchronizedList(new ArrayList<>());
    java.util.List<RewardSprite> rewards = Collections.synchronizedList(new ArrayList<>());
    javax.swing.Timer gameTimer;
    javax.swing.Timer enemyTimer;
    int score = 0;
    int lives = 3;
    boolean paused = false;
    boolean darkTheme = true;

    GamePanel(MiniGameFrame p) {
        this.parent = p;
        setLayout(null);
        setFocusable(true);
        addKeyListener(this);

        player = new PlayerSprite(100, 300, 48, 48, Color.ORANGE);
        add(player);

        JLabel hud = new JLabel();
        hud.setBounds(10, 10, 300, 30);
        hud.setForeground(Color.WHITE);
        hud.setFont(new Font("Monospaced", Font.BOLD, 14));
        add(hud);

        gameTimer = new javax.swing.Timer(20, e -> {
            if (!paused) {
                updateGame();
                hud.setText(String.format("Jugador: %s  Punt: %d  Vidas: %d", playerName, score, lives));
            }
        });
        gameTimer.start();

        enemyTimer = new javax.swing.Timer(600, e -> {
            if (!paused) {
                spawnEnemyOrReward();
                synchronized (enemies) {
                    for (EnemySprite en : enemies) en.update();
                }
            }
        });
        enemyTimer.start();

        CustomButton pauseBtn = new CustomButton("Pausar (P)");
        pauseBtn.setBounds(760, 10, 120, 36);
        pauseBtn.addActionListener(ev -> togglePause());
        add(pauseBtn);

        CustomButton menuBtn = new CustomButton("Menu");
        menuBtn.setBounds(760, 52, 120, 36);
        menuBtn.addActionListener(ev -> {
            stopTimers();
            parent.showMenu();
        });
        add(menuBtn);
    }

    void startNewGame(String name) {
        this.playerName = name;
        score = 0;
        lives = 3;
        paused = false;
        enemies.clear();
        rewards.clear();
        player.setLocation(50, getHeight() / 2);
        requestFocusInWindow();
        startTimers();
    }

    void startTimers() {
        if (!gameTimer.isRunning()) gameTimer.start();
        if (!enemyTimer.isRunning()) enemyTimer.start();
    }

    void stopTimers() {
        gameTimer.stop();
        enemyTimer.stop();
    }

    void togglePause() {
        paused = !paused;
        if (paused) {
            InfoDialog dlg = new InfoDialog(parent, "Pausa", false, "Juego pausado.\nPulsa P para continuar.");
            dlg.setVisible(true);
        }
    }

    void toggleTheme() {
        darkTheme = !darkTheme;
        repaint();
    }

    void updateGame() {
        synchronized (enemies) {
            Iterator<EnemySprite> it = enemies.iterator();
            while (it.hasNext()) {
                EnemySprite en = it.next();
                en.moveStep();
                if (en.getBounds().intersects(player.getBounds())) {
                    lives--;
                    it.remove();
                    remove(en);
                    if (lives <= 0) {
                        gameOver();
                        return;
                    }
                } else if (en.getX() < -100) {
                    it.remove();
                    remove(en);
                    score += 5;
                }
            }
        }

        synchronized (rewards) {
            Iterator<RewardSprite> it2 = rewards.iterator();
            while (it2.hasNext()) {
                RewardSprite r = it2.next();
                if (r.getBounds().intersects(player.getBounds())) {
                    score += r.points;
                    it2.remove();
                    remove(r);
                }
            }
        }
        repaint();
    }

    void spawnEnemyOrReward() {
        double rnd = Math.random();
        if (rnd < 0.7) {
            EnemySprite e = new EnemySprite(getWidth() + 50, 50 + (int) (Math.random() * (getHeight() - 100)), 36, 36, Color.RED);
            enemies.add(e);
            add(e);
        } else {
            RewardSprite r = new RewardSprite(getWidth() + 50, 50 + (int) (Math.random() * (getHeight() - 120)), 28, 28, Color.GREEN, 10);
            rewards.add(r);
            add(r);
        }
    }

    void gameOver() {
        stopTimers();
        InfoDialog dlg = new InfoDialog(parent, "GAME OVER", true,
                "Fin del juego.\nPuntuación: " + score + "\nVolver al menú?");
        dlg.setVisible(true);
        parent.showMenu();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (darkTheme) {
            GradientPaint gp = new GradientPaint(0, 0, new Color(18, 18, 25), 0, getHeight(), new Color(40, 44, 52));
            g2.setPaint(gp);
        } else {
            GradientPaint gp = new GradientPaint(0, 0, new Color(230, 240, 255), 0, getHeight(), new Color(200, 230, 255));
            g2.setPaint(gp);
        }
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    public void keyTyped(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        boolean up = k == KeyEvent.VK_UP || k == KeyEvent.VK_W;
        boolean down = k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S;
        boolean left = k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A;
        boolean right = k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D;
        if (k == KeyEvent.VK_P) { togglePause(); return; }
        int dx = 0, dy = 0;
        if (up) dy = -8;
        if (down) dy = 8;
        if (left) dx = -8;
        if (right) dx = 8;
        player.translate(dx, dy);
        if (player.getX() < 0) player.setX(0);
        if (player.getY() < 40) player.setY(40);
        if (player.getX() > getWidth() - player.getWidth()) player.setX(getWidth() - player.getWidth());
        if (player.getY() > getHeight() - player.getHeight()) player.setY(getHeight() - player.getHeight());
    }
    public void keyReleased(KeyEvent e) {}
}

/* --------------------- COMPONENTES PERSONALIZADOS --------------------- */

class CustomButton extends JButton {
    private boolean hovered = false;
    CustomButton(String text) {
        super(text);
        setFocusPainted(false);
        setBorder(new EmptyBorder(6, 12, 6, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setContentAreaFilled(false);
        setForeground(Color.WHITE);
        setBackground(new Color(70, 130, 180));
        setFont(new Font("SansSerif", Font.BOLD, 14));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
    }
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        if (hovered) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, w, h, 20, 20);
        g2.setColor(getBackground().darker());
        g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        String txt = getText();
        int tx = (w - fm.stringWidth(txt)) / 2;
        int ty = (h + fm.getAscent()) / 2 - 2;
        g2.drawString(txt, tx, ty);
        g2.dispose();
    }
}

class NameField extends JTextField {
    private String placeholder = "";
    NameField(int cols) {
        super(cols);
        setBorder(new LineBorder(Color.GRAY, 1, true));
    }
    public void setPlaceholder(String p) { placeholder = p; }
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && placeholder != null && !placeholder.isEmpty()) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.LIGHT_GRAY);
            g2.setFont(getFont().deriveFont(Font.ITALIC));
            Insets ins = getInsets();
            g2.drawString(placeholder, ins.left + 4, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
        }
    }
}

class Sprite extends JLabel {
    protected int w, h;
    protected Color color;
    protected boolean pulsing = false;
    protected double scale = 1.0;
    protected boolean clockwise = true;

    Sprite(int x, int y, int w, int h, Color c, boolean pulsing) {
        this.w = w; this.h = h; this.color = c; this.pulsing = pulsing;
        setBounds(x, y, w, h);
        setOpaque(false);
        new javax.swing.Timer(120, e -> {
            if (pulsing) {
                scale += (clockwise ? 0.06 : -0.06);
                if (scale > 1.3) clockwise = false;
                if (scale < 0.8) clockwise = true;
                repaint();
            }
        }).start();
    }

    public void setPulsing(boolean p) { pulsing = p; }
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cw = (int) (w * scale), ch = (int) (h * scale);
        int sx = (getWidth() - cw) / 2, sy = (getHeight() - ch) / 2;
        g2.setColor(color);
        g2.fillOval(sx, sy, cw, ch);
        g2.setColor(color.darker());
        g2.drawOval(sx, sy, cw - 1, ch - 1);
        g2.dispose();
    }
}

class PlayerSprite extends Sprite {
    PlayerSprite(int x, int y, int w, int h, Color c) {
        super(x, y, w, h, c, false);
    }
    void translate(int dx, int dy) { setLocation(getX() + dx, getY() + dy); }
    void setX(int x) { setLocation(x, getY()); }
    void setY(int y) { setLocation(getX(), y); }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(getX(), getY(), getWidth(), getHeight());
    }
}

class EnemySprite extends Sprite {
    private int speed = 4;
    EnemySprite(int x, int y, int w, int h, Color c) {
        super(x, y, w, h, c, false);
    }
    void update() {}
    void moveStep() { setLocation(getX() - speed, getY()); }

    @Override
    public int getX() {
        return super.getX();
    }
}

class RewardSprite extends Sprite {
    int points;
    RewardSprite(int x, int y, int w, int h, Color c, int pts) {
        super(x, y, w, h, c, true);
        this.points = pts;
    }
}

class InfoDialog extends JDialog {
    InfoDialog(Frame owner, String title, boolean modal, String text) {
        super(owner, title, modal);
        setLayout(new BorderLayout(8, 8));
        JTextArea ta = new JTextArea(text);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBackground(getBackground());
        add(ta, BorderLayout.CENTER);
        CustomButton ok = new CustomButton("OK");
        ok.addActionListener(e -> setVisible(false));
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.add(ok);
        add(p, BorderLayout.SOUTH);
        setSize(340, 180);
        setLocationRelativeTo(owner);
    }
}

class CustomMenuItem extends JMenuItem {
    CustomMenuItem(String s) { super(s); }
}

class ColorMenuItem extends JMenuItem {
    ColorMenuItem(String s) {
        super(s);
        setIcon(new ColorIcon(12, 12, Color.ORANGE));
    }
}

class ColorIcon implements Icon {
    int w, h; Color c;
    ColorIcon(int w, int h, Color c) { this.w = w; this.h = h; this.c = c; }
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(this.c); g.fillOval(x, y, w, h);
    }
    public int getIconWidth() { return w; }
    public int getIconHeight() { return h; }
}

class IconListRenderer extends JLabel implements ListCellRenderer<String> {
    public IconListRenderer() {
        setOpaque(true);
        setBorder(new EmptyBorder(4,4,4,4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list,
                                                  String value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        setText(value);
        setIcon(new ColorIcon(16,16, index % 2 == 0 ? Color.MAGENTA : Color.CYAN));
        if (isSelected) {
            setBackground(new Color(80,120,160));
            setForeground(Color.WHITE);
        } else {
            setBackground(Color.WHITE);
            setForeground(Color.DARK_GRAY);
        }
        return this;
    }
}