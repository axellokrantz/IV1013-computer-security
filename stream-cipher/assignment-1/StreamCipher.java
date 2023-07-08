import java.io.*;
import java.util.Random;

public class StreamCipher {

    FileInputStream inputFile;
    FileOutputStream outputFile;
    BufferedOutputStream bufferOutput;
    BufferedInputStream bufferInput;


    public static void main(String[] args){

        StreamCipher sc = new StreamCipher();
        sc.validateArgs(args);
        Random rnd = sc.validateSeed(args[0]);
        sc.validateInputFile(args[1]);
        sc.bufferInput = new BufferedInputStream(sc.inputFile);   
        sc.validateOutputFile(args[2]);
        sc.bufferOutput = new BufferedOutputStream(sc.outputFile);
        sc.encryptOrDecrypt(rnd);
        sc.closeStream();

    }

    private void validateArgs(String[] args){
        if(args.length != 3){
            System.err.println ("Please pass 3 arguments");
			System.exit(1);
		}
    }

    private void closeStream(){
        try{
            this.bufferOutput.close();
            this.bufferInput.close();
            this.outputFile.close();
            this.inputFile.close();
        }
        catch(IOException e){
            System.err.print("Could not close stream.");
            System.exit(1);
        }
    }

    private void encryptOrDecrypt(Random rnd){
        try{
            int i = this.bufferInput.read();
            while(i != -1){
                this.bufferOutput.write(i ^ rnd.nextInt(256));
                i = this.bufferInput.read();
            }
        }
        catch(IOException e){
            System.err.print("Failed to cypher file.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void validateInputFile (String file){
        try{
            this.inputFile = new FileInputStream(file);
        }
        catch(IOException e){
            System.err.print("Invalid input file.");
            System.exit(1);
        }
    }

    private void validateOutputFile(String fileName){
        try{
            this.outputFile = new FileOutputStream(fileName);
        }
        catch(FileNotFoundException e){
            System.err.print("Invalid output file.");
            System.exit(1);
        }
    }

    private Random validateSeed(String string){
        try{
            long seed = Long.parseLong(string);
            if(seed == 0)
                throw new IllegalArgumentException("Invalid seed.");
            return new Random(seed);    
        }
        catch(NumberFormatException e){
            System.err.print("Cannot convert seed to long.");
            System.exit(1);
        }
        return null;
    }
}
