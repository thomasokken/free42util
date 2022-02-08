import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

public class SkinMuncher {
    
    private static class Mapping {
        public int srcX, srcY, srcWidth, srcHeight;
        public int dstX, dstY;
    }
    
    private static int outputWidth, outputHeight;
    private static int skinWidth, skinHeight;
    private static int displayWidth, displayHeight;
    private static List<Mapping> mappings = new ArrayList<Mapping>();
    
    private static class ParseException extends RuntimeException {
        public ParseException(int line, Throwable cause) {
            super("At line " + line, cause);
        }
    }
    
    private static String inputName, outputName;
    
    private static void loadMunchScript(String name) throws IOException, ParseException {
        InputStream is = new FileInputStream(name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        int lineno = 0;
        int skipped = 0;
        while ((line = reader.readLine()) != null) {
            lineno++;
            int hash = line.indexOf('#');
            if (hash != -1)
                line = line.substring(0, hash);
            line = line.trim();
            if (line.length() == 0)
                continue;
            try {
                StringTokenizer tok = new StringTokenizer(line);
                String t = tok.nextToken().toLowerCase();
                if (t.equals("input")) {
                    inputName = tok.nextToken();
                } else if (t.equals("output")) {
                    outputName = tok.nextToken();
                } else if (t.equals("map")) {
                    Mapping m = new Mapping();
                    m.srcX = Integer.parseInt(tok.nextToken());
                    m.srcY = Integer.parseInt(tok.nextToken());
                    m.srcWidth = Integer.parseInt(tok.nextToken());
                    m.srcHeight = Integer.parseInt(tok.nextToken());
                    t = tok.nextToken().toLowerCase();
                    if (!t.equals("to"))
                        throw new Exception("syntax error");
                    m.dstX = Integer.parseInt(tok.nextToken());
                    m.dstY = Integer.parseInt(tok.nextToken());
                    mappings.add(m);
                } else if (t.equals("skip")) {
                    Mapping m = mappings.remove(mappings.size() - 1);
                    while (tok.hasMoreTokens()) {
                        String range = tok.nextToken();
                        int dash = range.indexOf('-');
                        int from, to;
                        if (dash == -1)
                            from = to = Integer.parseInt(range);
                        else {
                            from = Integer.parseInt(range.substring(0, dash));
                            to = Integer.parseInt(range.substring(dash + 1));
                        }
                        if (to < from || from < m.srcY || to > m.srcY + m.srcHeight)
                            throw new Exception("bad skipper " + range);
                        skipped += to - from;
                        if (from > m.srcY) {
                            Mapping nm = new Mapping();
                            nm.srcX = m.srcX;
                            nm.srcY = m.srcY;
                            nm.srcWidth = m.srcWidth;
                            nm.srcHeight = from - m.srcY;
                            nm.dstX = m.dstX;
                            nm.dstY = m.dstY;
                            mappings.add(nm);
                            m.dstY += nm.srcHeight;
                        }
                        m.srcHeight -= to - m.srcY;
                        m.srcY = to;
                    }
                    if (m.srcHeight > 0)
                        mappings.add(m);
                } else if (t.equals("skin")) {
                    skinWidth = Integer.parseInt(tok.nextToken());
                    skinHeight = Integer.parseInt(tok.nextToken());
                } else if (t.equals("displaysize")) {
                    displayWidth = Integer.parseInt(tok.nextToken());
                    displayHeight = Integer.parseInt(tok.nextToken());
                } else {
                    throw new Exception("unrecognized token \"" + t + "\"");
                }
                try {
                    t = tok.nextToken();
                    throw new Exception("extraneous token \"" + t + "\"");
                } catch (Exception e2) {}
            } catch (Exception e) {
                throw new ParseException(lineno, e);
            }
        }
        for (int i = 0; i < mappings.size(); i++) {
            Mapping m = mappings.get(i);
            int bottom = m.dstY + m.srcHeight;
            int right = m.dstX + m.srcWidth;
            if (bottom > outputHeight)
                outputHeight = bottom;
            if (right > outputWidth)
                outputWidth = right;
        }
        skinHeight -= skipped;
    }
    
    private static void munchGif() throws IOException {
        BufferedImage srcImage = ImageIO.read(new File(inputName + ".gif"));
        WritableRaster srcRaster = srcImage.getRaster();
        WritableRaster dstRaster = srcImage.getRaster().createCompatibleWritableRaster(outputWidth, outputHeight);
        for (int i = 0; i < mappings.size(); i++) {
            Mapping mapping = mappings.get(i);
            int xmax = mapping.srcX + mapping.srcWidth;
            int ymax = mapping.srcY + mapping.srcHeight;
            int xoff = mapping.dstX - mapping.srcX;
            int yoff = mapping.dstY - mapping.srcY;
            for (int x = mapping.srcX; x < xmax; x++) {
                for (int y = mapping.srcY; y < ymax; y++) {
                    dstRaster.setDataElements(x + xoff, y + yoff, srcRaster.getDataElements(x, y, null));
                }
            }
        }
        BufferedImage dstImage = new BufferedImage(srcImage.getColorModel(), dstRaster, true, null);
        ImageIO.write(dstImage, "GIF", new File(outputName + ".gif"));
    }
    
    private static String transformPoint(String coords) throws Exception {
        StringTokenizer tok = new StringTokenizer(coords, ",");
        int x = Integer.parseInt(tok.nextToken());
        int y = Integer.parseInt(tok.nextToken());
        for (int i = 0; i < mappings.size(); i++) {
            Mapping mapping = mappings.get(i);
            if (x >= mapping.srcX && x < mapping.srcX + mapping.srcWidth
                    && y >= mapping.srcY && y < mapping.srcY + mapping.srcHeight)
                return (x - mapping.srcX + mapping.dstX) + "," + (y - mapping.srcY + mapping.dstY);
        }
        throw new Exception("unmappable point: " + coords);
    }
    
    private static int min(int x, int y) {
        return x < y ? x : y;
    }

    private static int max(int x, int y) {
        return x > y ? x : y;
    }

    private static int abs(int x) {
        return x < 0 ? -x : x;
    }

    private static int rectOverlap(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        if (x1 + w1 < x2 || x2 + w2 < x1)
            return 0;
        if (y1 + h1 < y2 || y2 + h2 < y1)
            return 0;
        int w = min(x1 + w1, x2 + w2) - max(x1, x2);
        int h = min(y1 + h1, y2 + h2) - max(y1, y2);
        return abs(w * h);
    }

    private static String transformRect(String coords) throws Exception {
        StringTokenizer tok = new StringTokenizer(coords, ",");
        int x = Integer.parseInt(tok.nextToken());
        int y = Integer.parseInt(tok.nextToken());
        int w = Integer.parseInt(tok.nextToken());
        int h = Integer.parseInt(tok.nextToken());
        int xo = 0, yo = 0;
        int amt = 0;
        for (int i = 0; i < mappings.size(); i++) {
            Mapping mapping = mappings.get(i);
            int a = rectOverlap(x, y, w, h, mapping.srcX, mapping.srcY, mapping.srcWidth, mapping.srcHeight);
            if (a > amt) {
                xo = mapping.srcX - mapping.dstX;
                yo = mapping.srcY - mapping.dstY;
                amt = a;
            }
        }
        if (amt == 0)
            throw new Exception("unmappable point: " + coords);
        return (x - xo) + "," + (y - yo) + "," + w + "," + h;
    }
    
    private static void munchLayout() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(inputName + ".layout"));
        PrintWriter writer = new PrintWriter(outputName + ".layout");
        String line;
        while ((line = reader.readLine()) != null) {
            StringTokenizer tok = new StringTokenizer(line, " ");
            StringBuffer buf = new StringBuffer();
            if (line.startsWith("Skin:")) {
                buf.append(tok.nextToken());
                buf.append(" ");
                buf.append("0,0," + skinWidth + "," + skinHeight);
                writer.println(buf.toString());
            } else if (line.startsWith("Display:")) {
                buf.append(tok.nextToken());
                buf.append(" ");
                buf.append(transformPoint(tok.nextToken()));
                while (tok.hasMoreTokens()) {
                    buf.append(" ");
                    buf.append(tok.nextToken());
                }
                writer.println(buf.toString());
                if (skinWidth > skinHeight)
                    writer.println("DisplaySize: " + displayWidth + "," + displayHeight + " -1 -1 " + displayHeight);
            } else if (line.startsWith("Key:")) {
                buf.append(tok.nextToken());
                buf.append(" ");
                buf.append(tok.nextToken());
                buf.append(" ");
                buf.append(transformRect(tok.nextToken()));
                buf.append(" ");
                buf.append(transformRect(tok.nextToken()));
                buf.append(" ");
                buf.append(transformPoint(tok.nextToken()));
                writer.println(buf.toString());
            } else if (line.startsWith("Annunciator:")) {
                buf.append(tok.nextToken());
                buf.append(" ");
                buf.append(tok.nextToken());
                buf.append(" ");
                buf.append(transformRect(tok.nextToken()));
                buf.append(" ");
                buf.append(transformPoint(tok.nextToken()));
                writer.println(buf.toString());
            } else {
                writer.println(line);
            }
        }
        writer.close();
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java SkinMuncher <munch-script>");
            return;
        }
        try {
            loadMunchScript(args[0]);
            if (mappings.isEmpty())
                throw new Exception("no mappings specified");
            if (inputName == null)
                throw new Exception("no input name specified");
            if (outputName == null)
                throw new Exception("no output name specified");
            if (skinWidth == 0 || skinHeight == 0)
                throw new Exception("no skin size specified");
            if (skinWidth > skinHeight && (displayWidth == 0 || displayHeight == 0))
                throw new Exception("no display size specified");
            munchGif();
            munchLayout();
        } catch (Exception e) {
            e.printStackTrace();
            if (outputName != null) {
                new File(outputName + ".gif").delete();
                new File(outputName + ".layout").delete();
            }
        }
    }
}
