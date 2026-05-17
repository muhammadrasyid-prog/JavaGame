import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SpriteAnalyzer {
    public static void main(String[] args) throws Exception {
        asciiArt("d:/jekjek/assets/MC/idle.png", 120);
        System.out.println("---------------------------------");
        asciiArt("d:/jekjek/assets/MC/run.png", 120);
    }
    
    static void asciiArt(String path, int width) throws Exception {
        System.out.println("--- " + path + " ---");
        BufferedImage img = ImageIO.read(new File(path));
        double aspect = (double)img.getHeight() / img.getWidth();
        int height = (int)(aspect * width * 0.5); // 0.5 because font is usually twice as tall as wide
        
        char[] chars = {'B','S','#','&','@','$','%','*','!',':','.',' '};
        
        for (int y = 0; y < height; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < width; x++) {
                int srcX = (int)((x / (double)width) * img.getWidth());
                int srcY = (int)((y / (double)height) * img.getHeight());
                int pixel = img.getRGB(srcX, srcY);
                int alpha = (pixel >> 24) & 0xff;
                if (alpha < 128) {
                    sb.append(' ');
                } else {
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;
                    int intensity = (r + g + b) / 3;
                    int charIdx = intensity * chars.length / 256;
                    if (charIdx >= chars.length) charIdx = chars.length - 1;
                    sb.append(chars[charIdx]);
                }
            }
            System.out.println(sb.toString());
        }
    }
}
