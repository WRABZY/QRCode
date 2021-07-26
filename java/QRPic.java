import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;

class QRPic {

    private int moduleSize;
    private byte[][] code;
    
    QRPic (int moduleSize, byte[][] code) {
        this.moduleSize = moduleSize;
        this.code = code;
    }

    class QRPanel extends JPanel {
        @Override
        public void paintComponent(Graphics canvas) {
            super.paintComponent(canvas);
            Module m;
            for (int i = 0; i < code.length; i++) {
                for (int j = 0; j < code[i].length; j++) {
                    m = new Module(j * moduleSize, i * moduleSize, moduleSize, code[i][j] == 1);
                    m.draw(canvas);
                }
            }
        }
    }
    
    public void show() {
        JFrame frame = new JFrame("WRABZY's QR Code");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new QRPanel();
        frame.getContentPane().add(panel);
        frame.setSize(new Dimension(moduleSize * code.length + 3 * moduleSize, moduleSize * code.length + 6 * moduleSize));
        frame.setVisible(true);
    }
    
    private class Module {
        private int x;
        private int y;
        private int size;
        private Color color;
        
        Module(int x, int y, int size, boolean black) {
            this.x = x;
            this.y = y;
            this.size = size;
            color = black ? Color.BLACK : Color.WHITE;
        }
        
        public void draw(Graphics canvas) {
            canvas.setColor(color);
            canvas.fillRect(x, y, x + size, y + size);
        }
    }
}



    
    