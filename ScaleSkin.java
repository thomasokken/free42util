import java.io.*;
import java.util.*;

public class ScaleSkin {
    private static final double SCALE = 0.5;

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("Plus42.layout"));
            PrintWriter writer = new PrintWriter("/Users/thomas/plus42/skins/Plus42.layout");
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(line, " ");
                StringBuffer buf = new StringBuffer();
                if (line.startsWith("Skin:")) {
                    buf.append(tok.nextToken());
                    buf.append(" ");
                    buf.append(transformRect(tok.nextToken(), 0));
                    writer.println(buf.toString());
                } else if (line.startsWith("Display:")) {
                    buf.append(tok.nextToken());
                    buf.append(" ");
                    buf.append(transformPoint(tok.nextToken(), 0));
                    String t;
                    while (tok.hasMoreTokens()) {
                        buf.append(" ");
                        buf.append(tok.nextToken());
                    }
                    writer.println(buf.toString());
                } else if (line.startsWith("Key:")) {
                    buf.append(tok.nextToken());
                    buf.append(" ");
                    buf.append(tok.nextToken());
                    buf.append(" ");
                    buf.append(transformRect(tok.nextToken(), 0));
                    buf.append(" ");
                    buf.append(transformRect(tok.nextToken(), 0));
                    buf.append(" ");
                    buf.append(transformPoint(tok.nextToken(), 0));
                    writer.println(buf.toString());
                } else if (line.startsWith("Annunciator:") || line.startsWith("AltBkgd:")) {
                    buf.append(tok.nextToken());
                    buf.append(" ");
                    buf.append(tok.nextToken());
                    buf.append(" ");
                    buf.append(transformRect(tok.nextToken(), 0));
                    buf.append(" ");
                    buf.append(transformPoint(tok.nextToken(), 0));
                    writer.println(buf.toString());
                } else if (line.startsWith("AltKey:")) {
                    buf.append(tok.nextToken());
                    buf.append(" ");
                    buf.append(tok.nextToken());
                    buf.append(" ");
                    buf.append(tok.nextToken());
                    buf.append(" ");
                    buf.append(transformPoint(tok.nextToken(), 0));
                    writer.println(buf.toString());
                } else {
                    writer.println(line);
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int floor(double x) {
        return (int) Math.floor(x);
    }

    private static int ceil(double x) {
        return (int) Math.ceil(x);
    }

    private static String transformPoint(String p, int offset) {
        StringTokenizer tok = new StringTokenizer(p, ",");
        int x = Integer.parseInt(tok.nextToken());
        int y = Integer.parseInt(tok.nextToken());
        return floor(x * SCALE) + "," + (floor(y * SCALE) + offset);
    }

    private static String transformRect(String p, int offset) {
        StringTokenizer tok = new StringTokenizer(p, ",");
        int x = Integer.parseInt(tok.nextToken());
        int y = Integer.parseInt(tok.nextToken());
        int w = Integer.parseInt(tok.nextToken());
        int h = Integer.parseInt(tok.nextToken());
        return floor(x * SCALE) + "," + (floor(y * SCALE) + offset)
            + "," + ceil(w * SCALE) + "," + ceil(h * SCALE);
    }
}
