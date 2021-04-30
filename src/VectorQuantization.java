import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;
import java.util.Vector;


public class VectorQuantization {
    static int oldHeight, oldWidth, newWidth, newHeight;
    static int BlockH, BlockW, codeBlockLength;
    static String path = System.getProperty("user.home") + "\\Desktop\\017.jpg";

    static int[][] Resize(int[][] image) {
        if ((newHeight = oldHeight) % BlockH != 0) newHeight = ((oldHeight / BlockH) + 1) * BlockH;
        if ((newWidth = oldWidth) % BlockW != 0) newWidth = ((oldWidth / BlockW) + 1) * BlockW;


        int column, row;
        int[][] resizedImage = new int[newHeight][newWidth];
        for (int i = 0; i < newHeight; i++) {
            column = i;


            for (int j = 0; j < newWidth; j++) {
                row = j;

                if (i+1> oldHeight || j+1> oldWidth){
                    resizedImage[i][j] = 0;
                }
                else  resizedImage[i][j] = image[column][row];
            }
        }
        return resizedImage;
    }

    static Vector <Vector <Integer>> CreateBlocks(int[][] resizedImage) {
        Vector <Vector <Integer>> Vectors = new Vector <>();

        for (int i = 0; i < newHeight; i += BlockH)
            for (int j = 0; j < newWidth; j += BlockW) {
                var v = new Vector();
                for (int x = i; x < i + BlockH; x++)
                    for (int y = j; y < j + BlockW; y++)
                        v.add(resizedImage[x][y]);
                Vectors.add(v);
            }
        return Vectors;
    }

    static void WriteToFile(Vector <Vector <Integer>> codeBlocks, Vector <Integer> Indices) throws IOException {

        var HelperFile = new ObjectOutputStream(new FileOutputStream(path.substring(0, path.lastIndexOf('.')) + ".txt"));
        HelperFile.writeInt(oldWidth);
        HelperFile.writeInt(oldHeight);
        HelperFile.writeInt(newWidth);
        HelperFile.writeInt(newHeight);
        HelperFile.writeInt(BlockW);
        HelperFile.writeInt(BlockH);
        HelperFile.writeObject(Indices);
        HelperFile.writeObject(codeBlocks);
        HelperFile.close();
    }

    static Vector <Integer> Mean(Vector <Vector <Integer>> Vectors) {
        var cumulative = new int[Vectors.elementAt(0).size()]; //size = block elements
        var numberOfBlocks = Vectors.size();
        var result = new Vector <Integer>();
        for (var block : Vectors)
            for (int i = 0; i < block.size(); i++)
                cumulative[i] += block.elementAt(i);
        for (int i = 0; i < cumulative.length; i++)
            result.add(cumulative[i] / numberOfBlocks);
        return result;
    }
    /*
    1   2       5   6       9   10
    3   4       7   8       11  12
    15/3    18/3
    21/3    24/3
     */

    static int minDistance(Vector <Integer> x, Vector <Integer> y, int PlusOrMinus) {
        int dist = 0;
        for (int i = 0; i < x.size(); i++)

            dist += Math.abs(x.get(i) - y.get(i) + PlusOrMinus);
        return dist;
    }

    private static Vector <Integer> rearrange(Vector <Vector <Integer>> Vectors, Vector <Vector <Integer>> codeBlocks) {
        Vector <Integer> result = new Vector <>();
        for (Vector <Integer> vector : Vectors) {
            int min = 1000000, index = -1, temp;

            for (int i = 1; i < codeBlocks.size(); i++) {
                if ((temp = minDistance(vector, codeBlocks.get(i), 0)) < min) {
                    min = temp;
                    index = i;
                }
            }
            result.add(index);
        }


        return result;
    }

    static Vector <Integer> Quantization(int L, Vector <Vector <Integer>> Vectors, Vector <Vector <Integer>> codeBlocks) {
        if (L == 1) {
            if (Vectors.size() > 0)
                codeBlocks.add(Mean(Vectors));
            return rearrange(Vectors, codeBlocks);
        }

        Vector <Vector <Integer>> Lefts = new Vector(), Rights = new Vector();

        Vector <Integer> mean = Mean(Vectors);
        for (var vec : Vectors) {
            int left = minDistance(vec, mean, 1);
            int right = minDistance(vec, mean, -1);
            if (left > right) Lefts.add(vec);
            else Rights.add(vec);
        }
        Quantization(L / 2, Rights, codeBlocks);
        Quantization(L / 2, Lefts, codeBlocks);
        return rearrange(Vectors, codeBlocks);
    }
    /*
        3 10 20
                11
        3   10      20

            64
         32     32
      16    16  16  16
     */

    public static int[][] readImage(String filePath) {

        File f = new File(filePath);

        int[][] imageMAtrix = null;

        try {
            BufferedImage img = ImageIO.read(f);
            oldWidth = img.getWidth();
            oldHeight = img.getHeight();

            imageMAtrix = new int[oldHeight][oldWidth];

            for (int y = 0; y < oldHeight; y++) {
                for (int x = 0; x < oldWidth; x++) {
                    int p = img.getRGB(x, y);
                    int a = (p >> 24) & 0xff;
                    int r = (p >> 16) & 0xff;
                    int g = (p >> 8) & 0xff;
                    int b = p & 0xff;


                    imageMAtrix[y][x] = r;


                    p = (a << 24) | (r << 16) | (g << 8) | b;
                    img.setRGB(x, y, p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageMAtrix;
    }

    public static void writeImage(int[][] imagePixels, String outPath) {
        int oldH = imagePixels.length;
        int oldW = imagePixels[0].length;
        BufferedImage img = new BufferedImage(oldW, oldH, BufferedImage.TYPE_3BYTE_BGR);

        for (int y = 0; y < oldH; y++) {
            for (int x = 0; x < oldW; x++) {

                int a = 255;
                int pix = imagePixels[y][x];
                int p = (a << 24) | (pix << 16) | (pix << 8) | pix;

                img.setRGB(x, y, p);

            }
        }

        File f = new File(outPath);
        try {
            ImageIO.write(img, "jpg", f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void Compress(String Path) throws IOException {

        int[][] image = readImage(Path);

        int[][] resizedImage = Resize(image);

        Vector <Vector <Integer>> Vectors = CreateBlocks(resizedImage);
        Vector <Vector <Integer>> codeBlocks = new Vector <>();

        Vector <Integer> Indices = Quantization(codeBlockLength, Vectors, codeBlocks);

        WriteToFile(codeBlocks, Indices);


    }

    static void Decompress(String Path) throws IOException, ClassNotFoundException {

        InputStream file = new FileInputStream(Path);
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput input = new ObjectInputStream(buffer);

        oldWidth = input.readInt();
        oldHeight = input.readInt();
        int NEWidth = input.readInt();int NEWoldH = input.readInt();
        int BlockW = input.readInt();int BlockH = input.readInt();
        Vector <Integer> Indices = (Vector <Integer>) input.readObject();
        Vector <Vector <Integer>> codeBlocks = (Vector <Vector <Integer>>) input.readObject();

        int[][] newImg = new int[NEWoldH][NEWidth];
        for (int i = 0; i < Indices.size(); i++) {
            int x = i / (NEWidth / BlockW);
            int y = i % (NEWidth / BlockW);
            x *= BlockH;
            y *= BlockW;
            int v = 0;
            for (int j = x; j < x + BlockH; j++) {
                for (int k = y; k < y + BlockW; k++) {
                    newImg[j][k] = codeBlocks.get(Indices.get(i)).get(v++);
                }
            }
        }
        writeImage(newImg, (Path.substring(0, path.lastIndexOf('.')) + "_Reconstructed.jpg"));
    }

    public static void main(String[] args) {

        try {
            Scanner input = new Scanner(System.in);
            System.out.println("Enter your Block Height");
            BlockH = input.nextInt();
            System.out.println("Enter your Block Width");
            BlockW = input.nextInt();
            System.out.println("Enter your CodeBookLength");
            codeBlockLength = input.nextInt();
            Compress(path);
            Decompress(path.substring(0, path.lastIndexOf('.')) + ".txt");
            System.out.println();
            System.out.println("CONVERSION IS DONE SUCCESSFULLY");
        } catch (IOException e1) {
            System.out.println("There is an error occurred");
        } catch (ClassNotFoundException e1) {
            System.out.println("There is an error occurred");
        }
    }
}