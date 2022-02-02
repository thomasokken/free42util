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
                } else if (t.equals("size")) {
                    outputWidth = Integer.parseInt(tok.nextToken());
                    outputHeight = Integer.parseInt(tok.nextToken());
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
    
    private static String transformRect(String coords) throws Exception {
        int comma = coords.indexOf(',');
        if (comma == -1)
            throw new Exception("bad rect: " + coords);
        comma = coords.indexOf(',', comma + 1);
        if (comma == -1)
            throw new Exception("bad rect: " + coords);
        return transformPoint(coords.substring(0, comma)) + coords.substring(comma);
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
                buf.append(transformRect(tok.nextToken()));
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
            if (inputName == null)
                throw new Exception("no input name specified");
            if (outputName == null)
                throw new Exception("no output name specified");
            if (outputWidth == 0 || outputHeight == 0)
                throw new Exception("no output size specified");
            if (mappings.isEmpty())
                throw new Exception("no mappings specified");
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
