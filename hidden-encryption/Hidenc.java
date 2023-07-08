import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Hidenc{

    private static String hexKey;
    private static String inputPath;
    private static String outputPath;
    private static String templatePath;
    private static String ctr;
    private static Integer offset;
    private static Integer size;

    public static void main(String[] args) {
        
        try{
            Hidenc hid = new Hidenc();
            hid.validateArgs(args);
            byte[] byteKey = hid.hexStringToByteArray(hexKey);
            byte[] data = hid.readDataFromFile(inputPath);
            byte[] hashKey = hid.computeHash(byteKey, "MD5");
            byte[] hashedData = hid.computeHash(data, "MD5");
            byte[] blob = hid.createBlob(hashKey, hashedData, data);
            byte[] container = hid.createContainer(blob.length);
            offset = hid.getOffset(blob.length, container.length);
            Cipher cipher = hid.createCipherObject(byteKey);
            byte[] encryptedBlob = cipher.update(blob);
            hid.plantBlob(encryptedBlob, container);
            hid.writeDataToFile(container, outputPath);
        }
        catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        catch(InvalidKeyException e){
            e.printStackTrace();
        }
        catch(NoSuchPaddingException e){
            e.printStackTrace();
        }
        catch(InvalidAlgorithmParameterException e){
            e.printStackTrace();
        }
        catch(IllegalArgumentException e){
            System.out.println(e);
        }
    }

    private void plantBlob(byte[] array, byte[] container){
        for (int i = 0; i < array.length; i++) {
            container[offset + i] = array[i];
        }
    }

    private Integer randomDivisableBySixteen(int blobLength, int containerLength){
        int maxValue = containerLength - blobLength;
            Random rnd = new Random();
            Integer randomNumber;
    
            do {
                randomNumber = rnd.nextInt(maxValue + 1);
            } while (randomNumber % 16 != 0);
            return randomNumber;
    }

    private Integer getOffset(int blobLength, int containerLength){
        if(offset == null){
            return randomDivisableBySixteen(blobLength, containerLength);
        }
        if(!(offset % 16 == 0) || offset > (containerLength - blobLength)){
            throw new IllegalArgumentException("Invalid offset value.");
        }
        else{
            return offset;
        }
    }

    private byte[] createContainer(int blobLength) throws IOException{
        
        byte[] tmp;
        if(templatePath == null){
            tmp = new byte[size];
            Random rnd = new Random();
            rnd.nextBytes(tmp);
        }
        else{
            tmp = readDataFromFile(templatePath);
        }

        if((tmp.length % 16 != 0) || tmp.length < blobLength)
            throw new IllegalArgumentException("Container size must be divisable with 16 and larger or equal to blob size.");
        return tmp;
    }

    private byte[] createBlob(byte[] hashKey, byte[] hashedData, byte[] data){
        int blobSize = ((hashKey.length * 2) + data.length + hashedData.length); 
        byte[] blob = new byte[blobSize];
        int offset = 0;

        System.arraycopy(hashKey, 0, blob, offset, hashKey.length);
        offset += hashKey.length;
    
        System.arraycopy(data, 0, blob, offset, data.length);
        offset += data.length;
    
        System.arraycopy(hashKey, 0, blob, offset, hashKey.length);
        offset += hashKey.length;
    
        System.arraycopy(hashedData, 0, blob, offset, hashedData.length);
    
        return blob;
    }

    private Cipher createCipherObject(byte[] byteKey) throws NoSuchAlgorithmException,
        InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException{

        SecretKeySpec secretKey = new SecretKeySpec(byteKey, "AES");
        if(ctr == null){    
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher;
        }
            IvParameterSpec iv = new IvParameterSpec(hexStringToByteArray(ctr));
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            return cipher;
    }

    private void writeDataToFile(byte[] data, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
        }
    }

    private byte[] readDataFromFile(String path) throws IOException{
        return Files.readAllBytes(Paths.get(path));
    }

    private byte[] computeHash(byte[] byteKey, String algorithm) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(byteKey);
        return md.digest();   
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
   
        if(args.length < 4 || args.length > 6){
            throw new IllegalArgumentException("Pass no more than 6 and no less than 5 arguments: <--key=KEY> <--input=INPUT> <--output=OUTPUT> <--ctr=CTR>(opt) <--offset=NUM>(opt) --template=TEMPLATE(opt) or --size=SIZE(opt)");
        }
        for (String arg : args) {
            String option = arg.split("=")[0];
            String value = arg.substring(option.length() + 1);
        
            switch (option) {
                case "--key":
                    hexKey = value;
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
                case "--offset":
                    offset = Integer.parseInt(value);
                break;
                case "--template":
                    templatePath = value;
                break;
                case "--size":
                    size = Integer.parseInt(value);
                break;
                case "--ctr":
                    ctr = value;
                break;                
            }
        }
        if (hexKey == null || inputPath == null || outputPath == null)
                throw new IllegalArgumentException("Please include the following arguments. <--key=KEY> <--ctr=CTR> <--input=INPUT> <--output=OUTPUT> <--size=SIZE> / <template=TEMPLATE>");
        else if((size == null && templatePath == null) || (size != null && templatePath != null))
                throw new IllegalArgumentException("Please include either <--size=SIZE> or <--template=TEMPLATE>");        
    }
}