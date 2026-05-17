import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class BlockCheck {
    public static void main(String[] args) throws Exception {
        checkBlocks("d:/jekjek/assets/MC/run.png", 512);
        System.out.println("----------");
        checkBlocks("d:/jekjek/assets/MC/run.png", 768);
    }
    
    static void checkBlocks(String path, int size) throws Exception {
        System.out.println("Checking " + size + "x" + size);
        BufferedImage img = ImageIO.read(new File(path));
        int cols = img.getWidth() / size;
        int rows = img.getHeight() / size;
        
        for (int r = 0; r < Math.min(rows, 3); r++) {
            for (int c = 0; c < cols; c++) {
                int count = 0;
                for (int y = r*size; y < (r+1)*size; y++) {
                    for (int x = c*size; x < (c+1)*size; x++) {
                        if (((img.getRGB(x,y)>>24)&0xff) > 50) count++;
                    }
                }
                System.out.printf("Row %d Col %d : %d pixels\n", r, c, count);
            }
        }
    }
}
