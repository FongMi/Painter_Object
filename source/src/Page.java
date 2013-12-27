
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JPanel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Page extends JPanel {
    /*起始點、結束點、圖形起始點*/
    private Point p1, p2, loc;
    /*圖形寬高、線條粗細、圖形計量、線條起點*/
    private int width, height, lineWidth, OBJ_counter, Start;
    /*畫筆顏色、橡皮擦顏色*/
    private Color PenColor, EraserColor;
    /*畫筆型式*/
    private Stroke PenStroke;
    /*圖形暫存*/
    private Shape shape = null;
    /*DrawObject 暫存*/
    private DrawObject drawobject;
    /*Ctrl 事件*/
    private boolean CtrlDown = false;
    /*是否要填滿*/
    public boolean isFill = false;
    /*畫筆型態、狀態*/
    public Status type, status;
    /*儲存線條及圖形*/
    private final ArrayList<DrawObject> shapeList = new ArrayList();
    /*儲存線條起點終點*/
    private final ArrayList<DrawObject> freeList = new ArrayList();

    Page(MainWindow parant) {
        this.setBackground(Color.WHITE);
        this.setLayout(null);
        this.addMouseListener(new myMouseAdapter());
        this.addMouseMotionListener(new myMouseAdapter());
        this.addKeyListener(new myKeyAdapter());
        lineWidth = 2; /*粗細預設=2*/
        OBJ_counter = -1; /*圖形物件預設=-1*/
        type = Status.Pen; /*畫筆型態預設=Pen*/
        status = Status.Draw; /*狀態預設=Draw*/
        PenStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        /*畫出線條及橡皮擦*/
        for (DrawObject temp : shapeList) {
            if (temp.type == Status.Pen || temp.type == Status.Eraser) {
                g2d.setStroke(temp.stroke);
                g2d.setColor(temp.color);
                g2d.draw(temp.shape);
            }
        }
        /*畫出拖曳軌跡*/
        if (shape != null && type != Status.Eraser) {
            g2d.setStroke(PenStroke);
            g2d.setColor(PenColor);
            if (isFill) {
                g2d.fill(shape);
            }
            g2d.draw(shape);
            shape = null;
        }
    }

    public void Undo() {
        int f_size = freeList.size() - 1;
        if (freeList.size() > 0 && shapeList.size() == freeList.get(f_size).end) {
            int i = freeList.get(f_size).start;
            int j = freeList.get(f_size).end - 1;
            for (; j > i; j--) {
                shapeList.remove(j);
            }
            freeList.remove(f_size);
        }
        /*如果是線條或橡皮擦就移除*/
        if (shapeList.size() > 0) {
            if (shapeList.get(shapeList.size() - 1).type == Status.Pen
                    || shapeList.get(shapeList.size() - 1).type == Status.Eraser) {
                shapeList.remove(shapeList.size() - 1);
            } else {
                /*不是就移除物件*/
                shapeList.remove(shapeList.size() - 1);
                this.remove(OBJ_counter);
                OBJ_counter--;
            }
        }
        repaint();
    }

    /*選擇顏色*/
    public void ChooseColor() {
        Color c = JColorChooser.showDialog(this, "選擇顏色", getBackground());
        if (c != null) {
            if (ToolBar.colorJTBtn[0].isSelected()) {
                ToolBar.setcolorPanel[0].setBackground(c);
            } else if (ToolBar.colorJTBtn[1].isSelected()) {
                ToolBar.setcolorPanel[1].setBackground(c);
            }
        }
    }

    /*設定筆刷粗細*/
    public void SetStroke(int lineWidth) {
        this.lineWidth = lineWidth;
        PenStroke = new BasicStroke(this.lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
        this.requestFocus();
    }

    /*鍵盤監聽事件*/
    class myKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            /*Ctrl + Z 復原*/
            if (e.getKeyCode() == KeyEvent.VK_Z && e.isControlDown()) {
                Undo();
            }
            /*Ctrl + + 變粗*/
            if (e.getKeyCode() == KeyEvent.VK_ADD && e.isControlDown() && lineWidth < 30) {
                SetStroke(lineWidth + 1);
            }
            /*Ctrl + - 變細*/
            if (e.getKeyCode() == KeyEvent.VK_SUBTRACT && e.isControlDown() && lineWidth > 0) {
                SetStroke(lineWidth - 1);
            }
            /*按下Ctrl*/
            if (e.isControlDown()) {
                CtrlDown = true;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            /*放開Ctrl*/
            if (!e.isControlDown()) {
                CtrlDown = false;
            }
        }
    }

    /*滑鼠監聽事件*/
    class myMouseAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (status == Status.Idle) {
                if (drawobject != null) {
                    /*將 drawobject 狀態變成 Idle*/
                    drawobject.status = Status.Idle;
                }
            }
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            /*取得起點*/
            p1 = e.getPoint();

            /*取得顏色*/
            PenColor = ToolBar.setcolorPanel[0].getBackground();
            EraserColor = ToolBar.setcolorPanel[1].getBackground();
            
            switch (type) {
                case Pen:
                case Eraser:
                    Start = shapeList.size();
                    break;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            /*取得拖曳中的點*/
            p2 = e.getPoint();

            /*計算圖形長寬*/
            width = Math.abs(p2.x - p1.x);
            height = Math.abs(p2.y - p1.y);

            /*計算圖形起點*/
            loc = new Point(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y));

            /*按下Ctrl 高=寬*/
            if (CtrlDown) {
                height = width;
            }
            
            if (status == Status.Draw) {
                switch (type) {
                    case Pen:
                        /*畫出線條*/
                        shape = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
                        /*建立物件*/
                        drawobject = new DrawObject(Page.this, shape, type);
                        /*設定起點、終點、顏色、粗細*/
                        drawobject.format(p1, p2, PenColor, PenStroke);
                        /*加到ArrayList*/
                        shapeList.add(drawobject);
                        /*設定起點終點*/
                        drawobject.point(Start, shapeList.size());
                        /*更新起點*/
                        p1 = p2;
                        break;
                    case Eraser:
                        shape = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
                        drawobject = new DrawObject(Page.this, shape, type);
                        drawobject.format(p1, p2, EraserColor, PenStroke);
                        shapeList.add(drawobject);
                        drawobject.point(Start, shapeList.size());
                        p1 = p2;
                        break;
                    case Line:
                        shape = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
                        break;
                    case Rectangle:
                        shape = new Rectangle2D.Double(loc.x, loc.y, width, height);
                        break;
                    case Round_Rectangle:
                        shape = new RoundRectangle2D.Double(loc.x, loc.y, width, height, 30, 30);
                        break;
                    case Oval:
                        shape = new Ellipse2D.Double(loc.x, loc.y, width, height);
                        break;
                }
                repaint();
            }
            MainWindow.statusBar.setText("滑鼠座標: (" + e.getX() + "," + e.getY() + ")");
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (status == Status.Draw) {
                switch (type) {
                    case Pen:
                    case Eraser:
                        /*新增線條區段*/
                        freeList.add(drawobject);
                        break;
                    case Line:
                        /*建立物件*/
                        drawobject = new DrawObject(Page.this, shape, type);
                        /*設定起點終點*/
                        drawobject.format(p1, p2, PenColor, PenStroke);
                        break;
                    case Rectangle:
                    case Round_Rectangle:
                    case Oval:
                        /*建立物件*/
                        drawobject = new DrawObject(Page.this, shape, type);
                        /*設定起點、寬高、顏色、粗細、填滿*/
                        drawobject.format(loc, width, height, PenColor, lineWidth, PenStroke, isFill);
                        /*設定 drawobject 狀態為選擇*/
                        drawobject.status = Status.Selected;
                        /*加到ArrayList*/
                        shapeList.add(drawobject);
                        /*加到 Page 畫面*/
                        Page.this.add(drawobject);
                        OBJ_counter++;
                        /*狀態 = Idle*/
                        break;
                }
                repaint();
                status = Status.Idle;
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            MainWindow.statusBar.setText("滑鼠座標: (" + e.getX() + "," + e.getY() + ")");
        }
    }

    /*清除畫面*/
    public void NewPage() {
        /*移除所有ArrayList*/
        shapeList.removeAll(shapeList);
        freeList.removeAll(freeList);
        /*移除所有圖形*/
        this.removeAll();
        OBJ_counter = -1;
        repaint();
    }

    /*開啟檔案*/
    public void Open() {
        JFileChooser Open_JC = new JFileChooser();
        Open_JC.setFileSelectionMode(JFileChooser.FILES_ONLY);
        Open_JC.setDialogTitle("開啟檔案");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "jpg", "gif", "png");
        Open_JC.setFileFilter(filter);
        int result = Open_JC.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = Open_JC.getSelectedFile();
            //try {
            //    image = ImageIO.read(new File(file.getAbsolutePath()));
            //    repaint();
            //} catch (IOException e) {
            //}
        }
    }

    /*儲存檔案*/
    public void Save() {
        JFileChooser Save_JC = new JFileChooser();
        Save_JC.setFileSelectionMode(JFileChooser.SAVE_DIALOG | JFileChooser.DIRECTORIES_ONLY);
        Save_JC.setDialogTitle("儲存檔案");
        int result = Save_JC.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String path = Save_JC.getSelectedFile().getAbsolutePath();
            BufferedImage image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = image.getGraphics();
            this.paint(g);
            if (path != null) {
                try {
                    File file = new File(path, "未命名.png");
                    ImageIO.write(image, "png", file);
                } catch (IOException ex) {
                    Logger.getLogger(Page.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
