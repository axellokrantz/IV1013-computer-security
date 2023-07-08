import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Hiddec {

    private static String hexKey;
    private static String ctr;
    private static String inputPath;
    private static String outputPath;

    private static byte [] checksum;
    private static Cipher cipher;

    public static void main(String[] args) {

        try{
            Hiddec hid = new Hiddec();
            hid.validateArgs(args);
            byte[] byteKey = hid.hexStringToByteArray(hexKey);
            byte[] hashKey = hid.computeHash(byteKey, "MD5");
            byte[] encryptedData = hid.readDataFromFile(inputPath);
            List<byte[]> segmentedData = hid.segmentFile(hashKey, encryptedData);
            cipher = hid.createCipherObject(byteKey);
            List<Integer> indices = hid.findBlobIndices(cipher, byteKey, hashKey, segmentedData);
            byte[] decryptedData = hid.extractBlob(indices, hashKey, encryptedData);
            hid.verifyData(decryptedData);
            hid.writeDataToFile(decryptedData, outputPath);
        }
        catch(IllegalArgumentException e){
            System.out.println(e.getMessage());
        }
        catch(IOException e){
            System.out.println("Failed to read data from file.");
        }
        catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        catch(NoSuchPaddingException e){
            e.printStackTrace();
        }
        catch(InvalidKeyException e){
            e.printStackTrace();
        }
        catch(InvalidAlgorithmParameterException e){
            e.printStackTrace();
        }
    }

    private void verifyData(byte[] decryptedData) throws NoSuchAlgorithmException{
        byte [] hashData = computeHash(decryptedData, "MD5");
        if(!Arrays.equals(hashData, checksum)){
            System.out.println("Operation failed: Data is not the hidden information.");
            System.exit(1);
        }
    }
    
    private byte[] extractBlob(List<Integer> indices, byte[] hashKey, byte[] encryptedData){
        byte[] blob = Arrays.copyOfRange(encryptedData, indices.get(0), indices.get(1) + (hashKey.length*2));
        byte[] decryptedBlob = cipher.update(blob);
        checksum = Arrays.copyOfRange(decryptedBlob, decryptedBlob.length - hashKey.length, decryptedBlob.length);
        return Arrays.copyOfRange(decryptedBlob, hashKey.length, decryptedBlob.length - (hashKey.length*2));
    }

    private List<Integer> findBlobIndices(Cipher cipher, byte[] byteKey, byte[] hashKey, List<byte[]> segmentedData){

        List<Integer> indices = new ArrayList<>();

        if(ctr == null){
            indices = getIndex(segmentedData, hashKey);
            verifyIndices(indices);
            return indices;
        }
        try{
            indices = getCTRIndex(segmentedData, byteKey, hashKey);
            verifyIndices(indices);
            updateCounter(byteKey);
            return indices;
        }
        catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        catch(InvalidAlgorithmParameterException e){
            e.printStackTrace();
        }
        catch(InvalidKeyException e){
            e.printStackTrace();
        }
        catch(NoSuchPaddingException e){
            e.printStackTrace();
        }
        return null;
    }

    private void verifyIndices(List<Integer> indices){
        if(indices.get(0) == null || indices.get(1) == null){
            System.out.println("Operation failed: Blob is not contained within the data.");
            System.exit(1);
        }
    }

    private void updateCounter(byte[] byteKey) throws NoSuchAlgorithmException,
    InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException {
            cipher = createCipherObject(byteKey);
    }

    private List<Integer> getIndex(List<byte[]> arrayList, byte[] hashKey) {
        List<Integer> indices = new ArrayList<Integer>();
        int index = 0;
        for (int i = 0; i < arrayList.size(); i++) {
            byte[] array = cipher.update(arrayList.get(i));
            if (Arrays.equals(array, hashKey)) {
                indices.add(index);
            }
            index += hashKey.length;
        }
        return indices;
    }

    private List<Integer> getCTRIndex(List<byte[]> arrayList, byte[] byteKey, byte[] hashKey) throws NoSuchAlgorithmException,
    InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException {
        boolean found = false;
        List<Integer> indices = new ArrayList<Integer>();
        int index = 0;
        for (int i = 0; i < arrayList.size(); i++) {
            if(!found){
                cipher = createCipherObject(byteKey);
            }
               byte[] array = cipher.update(arrayList.get(i));
    
            if (Arrays.equals(array, hashKey)) {
                found = true;  
                indices.add(index);
            }
            index += hashKey.length;
        }
        return indices;
    }

    private Cipher createCipherObject(byte[] byteKey) throws NoSuchAlgorithmException,
        InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException{

        SecretKeySpec secretKey = new SecretKeySpec(byteKey, "AES");    
        if(ctr == null){
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher;
        }
        else{
            IvParameterSpec iv = new IvParameterSpec(hexStringToByteArray(ctr));
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            return cipher;
        }
    }

    private void writeDataToFile(byte[] data, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
        }
    }

    private List<byte[]> segmentFile(byte[] hashKey, byte[] encryptedData){
        List<byte[]> segmentedData = new ArrayList<byte[]>();
        int numberOfSegments = getNumberOfSegments(hashKey, encryptedData);

        int startIndex = 0;
        int endIndex = 0;
        for (int i = 0; i < numberOfSegments; i++) {
            startIndex = hashKey.length * i;
            endIndex = Math.min(startIndex + hashKey.length, encryptedData.length);
            byte[] segment = new byte[endIndex - startIndex];
            System.arraycopy(encryptedData, startIndex, segment, 0, segment.length);
            segmentedData.add(segment);
        }
        return segmentedData;
    }

    private int getNumberOfSegments(byte[] hashKey, byte[] encryptedData){
        return (int) Math.ceil((double) encryptedData.length/ hashKey.length);
    }

    private byte[] computeHash(byte[] byteKey, String algorithm) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(byteKey);
        return md.digest();   
    }

    private byte[] readDataFromFile(String path) throws IOException{
        return Files.readAllBytes(Paths.get(path));
    }
    
    private byte[] hexStringToByteArray(String hexString) throws IllegalArgumentException{

        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string: " + hexString);
        }
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];
    
        for (int i = 0; i < length; i += 2) {
            String hexByte = hexString.substring(i, i + 2);
            byteArray[i / 2] = (byte) Integer.parseInt(hexByte, 16);
        }
        return byteArray;
    }

    private void validateArgs(String[] args) throws IllegalArgumentException{
   
        if(args.length < 3 || args.length > 5){
            throw new IllegalArgumentException("Please pass correct arguments: java Hiddec <--key=KEY> <--ctr=CTR>(opt) <--input=INPUT> <--output=OUTPUT>");
        }
        for (String arg : args) {
            String option = arg.split("=")[0];
            String value = arg.substring(option.length() + 1);
        
            switch (option) {
                case "--key":
                    hexKey = value;
                    break;
                case "--ctr":
                    ctr = value;
                    break;
                case "--input":
                    /* if (!value.endsWith(".data"))
                        throw new IllegalArgumentException("Input file must be of type '.data'"); */
                    inputPath = value;
                    break;
                case "--output":
                    /* if (!value.endsWith(".data"))
                        throw new IllegalArgumentException("Output file must be of type '.data'"); */
                    outputPath = value;
                    break;
            }
        }
        if (hexKey == null || inputPath == null || outputPath == null)
                throw new IllegalArgumentException("Please use the correct format for arguments: <--key=KEY> <--ctr=CTR> <--input=INPUT> <--output=OUTPUT>");
    }

}

