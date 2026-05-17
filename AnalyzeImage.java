import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class AnalyzeImage {
    public static void main(String[] args) throws Exception {
        analyze("d:/jekjek/assets/MC/idle.png");
        System.out.println("-----");
        analyze("d:/jekjek/assets/MC/run.png");
    }

    static void analyze(String path) throws Exception {
        System.out.println("Analyzing " + path);
        BufferedImage img = ImageIO.read(new File(path));
        int w = img.getWidth();
        int h = img.getHeight();
        System.out.println("Size: " + w + "x" + h);

        List<int[]> rows = new ArrayList<>();
        boolean inChar = false;
        int start = 0;

        for (int y = 0; y < h; y++) {
            long rowAlpha = 0;
            for (int x = 0; x < w; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                rowAlpha += alpha;
            }
            if (rowAlpha > 0 && !inChar) {
                inChar = true;
                start = y;
            } else if (rowAlpha == 0 && inChar) {
                inChar = false;
                rows.add(new int[]{start, y});
            }
        }
        if (inChar) {
            rows.add(new int[]{start, h});
        }

        System.out.println("Rows of characters: " + rows.size());
        for (int i = 0; i < rows.size(); i++) {
            int[] r = rows.get(i);
            System.out.println("Row " + i + ": y=" + r[0] + " to " + r[1] + " (height: " + (r[1] - r[0]) + ")");
        }
    }
}
