import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SpriteBounds {
    public static void main(String[] args) throws Exception {
        findGrid("d:/jekjek/assets/MC/idle.png");
        findGrid("d:/jekjek/assets/MC/run.png");
    }
    
    static void findGrid(String path) throws Exception {
        System.out.println("--- " + path + " ---");
        BufferedImage img = ImageIO.read(new File(path));
        int w = img.getWidth();
        int h = img.getHeight();
        
        int[] colFilled = new int[w];
        int[] rowFilled = new int[h];
        
        for(int y=0; y<h; y++) {
            for(int x=0; x<w; x++) {
                if (((img.getRGB(x,y)>>24)&0xff) > 50) {
                    colFilled[x]++;
                    rowFilled[y]++;
                }
            }
        }
        
        System.out.println("Filled columns segments:");
        boolean inCol = false;
        int start = 0;
        int count = 0;
        for(int x=0; x<w; x++) {
            if(colFilled[x] > 0 && !inCol) {
                inCol = true;
                start = x;
            } else if (colFilled[x] == 0 && inCol) {
                inCol = false;
                System.out.println("Col: " + start + " to " + (x-1) + " (width " + (x-start) + ")");
                count++;
            }
        }
        if(inCol) {
            System.out.println("Col: " + start + " to " + (w-1) + " (width " + (w-start) + ")");
            count++;
        }
        System.out.println("Total columns: " + count);
        
        System.out.println("Filled rows segments:");
        boolean inRow = false;
        start = 0;
        int rCount = 0;
        for(int y=0; y<h; y++) {
            if(rowFilled[y] > 0 && !inRow) {
                inRow = true;
                start = y;
            } else if (rowFilled[y] == 0 && inRow) {
                inRow = false;
                System.out.println("Row: " + start + " to " + (y-1) + " (height " + (y-start) + ")");
                rCount++;
            }
        }
        if(inRow) {
            System.out.println("Row: " + start + " to " + (h-1) + " (height " + (h-start) + ")");
            rCount++;
        }
        System.out.println("Total rows: " + rCount);
    }
}
