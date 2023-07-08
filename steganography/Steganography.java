import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Steganography {

    private static final int BITS_IN_BYTE = 8;
    private static final int MAX_COLOR_VALUE = 255;
    public static void main(String[] args) {
        Steganography stg = new Steganography();
        stg.parseUserInput(args);
    }

    private void encode(String imagePath, String messagePath, String n, String outputPath){

        try{
            String encodedMessage = encodeMessage(messagePath);
            BufferedImage BGR = getBGR(imagePath);
            encodeImage(imageToByteArray(BGR), encodedMessage, 0, Integer.parseInt(n));
            saveImageToPath(BGR, new File(outputPath),"png");
        }
        catch(IOException e){
            if (e.getMessage().equals("Error in getBGR")) {
                System.err.println("Failed to convert image to byte array: " + e.getMessage());
            }    
            else if (e.getMessage().equals("Error in fileToString")){
                System.err.println("Failed to generate String from file." + e.getMessage());
            }
            else if (e.getMessage().equals("Error in fileToBufferedImage")){
                System.err.println("Failed to generate BufferedImage from file." + e.getMessage());
            }
            else{
                e.printStackTrace(); 
            }
        }
        catch(IllegalArgumentException e){
            System.out.println(e);
        }
    }

    private String encodeMessage(String messagePath) throws IOException{
            String message = fileToString(messagePath);
            byte[] byteArrayMessage = message.getBytes();
            String binaryStringMessage = byteArrayToBinaryString(byteArrayMessage);
            byte[] lengthByteArray = intToByteArray(byteArrayMessage.length);
            String lengthOfMessage = byteArrayToBinaryString(lengthByteArray);
            return lengthOfMessage + binaryStringMessage;
    }

    private BufferedImage getBGR(String imagePath) throws IOException{
        BufferedImage image = fileToBufferedImage(imagePath);
        return imageToBGRImage(image);
    }

    private void decode(String imagePath, String n, String messagePath){
        try{
            BufferedImage BGR = getBGR(imagePath);
            byte[] byteArrayImage = imageToByteArray(BGR);
            String message = decodeImage(byteArrayImage, 0, Integer.parseInt(n));
            saveMessageToPath(new File(messagePath), message);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private void saveMessageToPath(File file, String message) throws FileNotFoundException, IOException{
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fos);
        writer.write(message);
        writer.close();
        fos.close();
    }

    private void saveImageToPath(BufferedImage image, File file, String extension) {
		try {
			file.delete(); 
			ImageIO.write(image, extension, file);
		} catch (Exception exception) {
			System.out.println("Image file could not be saved. Error: " + exception);
		}
	}

    private String decodeImage(byte[] encoded, int start, int n) throws IOException{

        ArrayList<Integer> integerList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < encoded.length; i++) {
            int v = getV(encoded[i], n);
            integerList.add(v);
            sb.append((convertToBinary(v, n)));
        }

        int length = Integer.parseInt(sb.substring(0, 4 * BITS_IN_BYTE), 2);

        int startIndex = 4 * BITS_IN_BYTE;
        int endIndex = (length + 4) * BITS_IN_BYTE;

        if(n % BITS_IN_BYTE != 0 && endIndex % n != 0){
            StringBuilder sb2 = new StringBuilder();
            int rounds = (int) Math.floor(endIndex/n);
            int rest = endIndex % n;

            ArrayList<Integer> newArrayList = new ArrayList<>();
            for (int i = 0; i < rounds + 1; i++) {
                newArrayList.add(integerList.get(i));
            }
            
            for (int i = 0; i < newArrayList.size() - 1; i++) {
                int v = newArrayList.get(i);
                sb2.append((convertToBinary(v, n)));
            }
            int v = newArrayList.get(newArrayList.size()-1);
            sb2.append((convertToBinary(v, rest)));
            sb2.delete(0, startIndex);

            return binaryToText(sb2.toString());
        }
        else{
            String data = sb.substring(startIndex, endIndex);
            return binaryToText(data);
        }
    }

    private String binaryToText(String binaryString) {
        StringBuilder textBuilder = new StringBuilder();
        int length = binaryString.length();
    
        for (int i = 0; i < length; i += BITS_IN_BYTE) {
            String binaryByte = binaryString.substring(i, Math.min(i + BITS_IN_BYTE, length));
            int decimalValue = Integer.parseInt(binaryByte, 2);
            char character = (char) decimalValue;
            textBuilder.append(character);
        }
    
        return textBuilder.toString();
    }

    private String convertToBinary(int v, int n) {
        if (n <= 0 || n > 32) {
            throw new IllegalArgumentException("Number of bits should be between 1 and 32 (inclusive).");
        }
        StringBuilder binary = new StringBuilder();
        for (int i = n - 1; i >= 0; i--) {
            int bit = (v >> i) & 1;
            binary.append(bit);
        }
        return binary.toString();
    }

    private byte getV(byte encodedByte, int n){
        int cPrime = Byte.toUnsignedInt(encodedByte);
        int v = (int) (cPrime % Math.pow(2, n));
        return (byte) v;
    }

    private byte[] encodeImage(byte[] pixels, String message, int start, int n){
        
        int bitsToConceal = message.length();
        int endIndex = n;
        int startIndex = 0;
        if(bitsToConceal > pixels.length * n){
            throw new IllegalArgumentException("Message is too long to fit inside image.");
        }
            for (int i = 1; i < message.length() + 1; i+=n, start++) {
                byte c = pixels[start];
                String tmp = message.substring(startIndex, endIndex);
                int v = Integer.parseInt(tmp, 2);
                byte concealedByte = getcPrime(c, v, n);
                pixels[start] = concealedByte;
                startIndex += n;
                endIndex = Math.min(endIndex+n, message.length());
            }   

        return pixels;
    }

    private  byte getcPrime(byte pixelByte, int v, int n) {
        
        int c = Byte.toUnsignedInt(pixelByte);
        int cPrime = Integer.MAX_VALUE;
        
        for (int i = 0; i <= MAX_COLOR_VALUE; i++) {
            int tmp = i;
            if((tmp % Math.pow(2, n) == v)){
                
                if(Math.abs(c - tmp) < Math.abs(c - cPrime)){
                    cPrime = tmp;
                }
            }
        }
        return (byte) cPrime;
    }

    private String byteArrayToBinaryString(byte[] byteArray) {
        StringBuilder result = new StringBuilder();
        for (byte b : byteArray) {
            for (int i = 7; i >= 0; i--) {
                result.append((b >> i) & 1);
            }
        }
        return result.toString();
    }

    private byte[] intToByteArray(int value){
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value);
        return buffer.array();
    }

    private byte[] imageToByteArray(BufferedImage image) throws IOException {
        java.awt.image.WritableRaster raster = image.getRaster();
		DataBufferByte buffer = (DataBufferByte)raster.getDataBuffer();
		return buffer.getData();
    }

    private BufferedImage imageToBGRImage(BufferedImage image) {
        BufferedImage BGR = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics graphics = BGR.getGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return BGR;
    }

    private String fileToString(String filePath) throws IOException{
            byte[] encodedBytes = Files.readAllBytes(Path.of(filePath));
            return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    private BufferedImage fileToBufferedImage(String path) throws IOException{
        File imgFile = new File(path);
        return ImageIO.read(imgFile);
    }

    private void parseUserInput(String[] args){
        
        if(args.length == 1 && args[0].equals("-help")){
            displayHelp();
        }
        else if(args.length == 4){
            if(!(Integer.parseInt(args[2]) >= 1 && Integer.parseInt(args[2]) <= BITS_IN_BYTE)){
                System.out.println("Argument <n> permitted range: 1-8");
                System.exit(1);
            }
            else if(!args[0].toLowerCase().endsWith(".png") || !args[3].toLowerCase().endsWith(".png")){
                System.out.println("Image file format must be .png");
                System.exit(1);

            }
            encode(args[0], args[1], args[2], args[3]);
        }
        else if(args.length == 3){
            if(!(Integer.parseInt(args[1]) >= 1 && Integer.parseInt(args[1]) <= BITS_IN_BYTE)){
                System.out.println("Argument <n> permitted range: 1-8");
                System.exit(1);
            }
            else if(!args[0].toLowerCase().endsWith(".png")){
                System.out.println("Image file format must be .png");
                System.exit(1);
            }
            decode(args[0], args[1], args[2]);
        }   
    }

    private void displayHelp() {
        System.out.println();
        System.out.println("   [STEGANOGRAPHER USAGE]");
        System.out.println();
        System.out.println("*  [Help]: Display this help section.");
        System.out.println("   $ java Steganographer -help");
        System.out.println();
        System.out.println("*  [Encode]: Encode an image with a secret message.");
        System.out.println("   $ java Steganographer <input-image> <input-message> <n> <output-image>");
        System.out.println();
        System.out.println("*  [Decode]: Decode an image.");
        System.out.println("   $ java Steganographer <input-image> <n> <output-message>");
        System.out.println();
        System.out.println("*  Argument <n> permitted range 1-8: Controlls how much data is stored in each color level.");
        System.out.println("   The same n has to be used for encoding and decoding.");
        System.out.println();
        System.out.println("*  Allowed image file formats: png.");
    }

}

