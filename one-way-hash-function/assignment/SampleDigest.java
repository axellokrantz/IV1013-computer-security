import java.io.UnsupportedEncodingException;
import java.security.*;

public class SampleDigest {
private static final String ASCII_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ ";
public static final String digestAlgorithm = "SHA-256";
public static final String textEncoding = "UTF-8";
public static int count = 0;

    public static void main(String[] args) {

        String [] inputText = {"IV1013 security", "Security is fun", "Yes, indeed"};
        String [] solutions = new String [3];

        try {
            
            solutions[0] = getSolution(inputText[0]);
            solutions[1] = getSolution(inputText[1]);
            solutions[2] = getSolution(inputText[2]);

            for (int i = 0; i < inputText.length; i++) {
                boolean[] found = {false};
                generateStrings(inputText[i].length(), "", solutions[i], found);
            }

        } catch (NoSuchAlgorithmException e) {
            System.out.println("Algorithm \"" + digestAlgorithm + "\" is not available");
        } catch (Exception e) {
            System.out.println("Exception "+e);
        }

    }

    public static String getSolution(String inputText1) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        byte[] digest = createHash(inputText1);
        return trimHash(digest);
    }

    public static byte[] createHash(String inputText) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance(digestAlgorithm);
        byte[] inputBytes = inputText.getBytes(textEncoding);
        md.update(inputBytes);
        return md.digest();
    }

    public static void printDigest(String inputText, byte[]
    digest) {
        System.out.println("Digest for the message \"" + inputText +"\", using " + digestAlgorithm + " is:");
        for (int i=0; i<digest.length; i++)
            System.out.format("%02x", digest[i]&0xff);
        System.out.println();
    }

    public static String trimHash(byte[] digest){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            sb.append(String.format("%02x", digest[i]&0xff)); 
        }
        return sb.toString().substring(0, 6);
    }

    public static void generateStrings(int length, String prefix, String solution, boolean[] found) {
        if (length == 0) {
            try {
                count++;
                byte[] hash = createHash(prefix);
                String trimmedString = trimHash(hash);
                if (trimmedString.equals(solution)) {
                    System.out.println("Solution found after: " + count + " iterations.");
                    System.out.println("Matching string: " + prefix);
                    printDigest(prefix, hash);
                    System.out.println("Solution is: " + solution);
                    System.out.println();
                    count = 0;
                    found[0] = true;
                    return;
                }
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Algorithm \"" + digestAlgorithm + "\" is not available");
            } catch (Exception e) {
                System.out.println("Exception " + e);
            }
        } else {
            for (int i = 0; i < ASCII_CHARS.length(); i++) {
                if (!found[0]) {
                    char c = ASCII_CHARS.charAt(i);
                    generateStrings(length - 1, prefix + c, solution, found);
                } else {
                    return;
                }
            }
        }
    }

}