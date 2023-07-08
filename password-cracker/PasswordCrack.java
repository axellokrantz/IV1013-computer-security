import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.io.BufferedReader;

public class PasswordCrack {

    BufferedReader dictionaryReader;
    BufferedReader commonPasswordsReader;
    BufferedReader passwordReader;

    HashSet<String> dictionary;
    HashSet<String> commonPasswords;
    ArrayList<User> users;
    ArrayList<User> tempUsers;

    public static void main(String[] args) {
        
        PasswordCrack pc = new PasswordCrack();
        validateArgs(args);
        pc.validateInputFiles(args);
        pc.createDictionary();
        pc.createUsers();
        pc.crack();
    }

    /* 
    1. Take a word from the wordlist;
    2. For each password entry, hash the word with the salt, and compare;
    3. Repeat the above steps for all words in the wordlist;
    4. Redo step 2, using one mangle of the words;
    5. Redo step 2, using two mangles of the words, and so on.
    */

    private void crack(){
        tempUsers = new ArrayList<User>();
        seedUsernames();
        dictionaryCrack(this.dictionary);
        crackCommonPasswords();
        mangle(this.dictionary, 2);
    }

    /*
     * Mangle phases
     * 01. All letters to upper case, e.g. STRING
     * 02. Reverse the string, e.g. gnirts
     * 03. Reflect the string, e.g. stringgnirts
     * 04. Reflect the string, e.g. gnirtsstring
     * 05. First letter to upper case, e.g. String
     * 06. All letters to upper case except first one, e.g. sTRING
     * 07. Duplicated word, e.g. stringstring
     * 08. Delete the first letter, e.g. tring
     * 09. Delete the last letter, e.g. strin
     * 10. Prepend number, e.g. string0
     * 11. Append number, e.g. 9string
     * 12. Toggle string, e.g. sTrInG or StRiNG
     */

    private void crackCommonPasswords(){

        try{
            this.commonPasswordsReader = new BufferedReader(new FileReader("commonPasswords.txt"));
        }
        catch(IOException e){
            System.err.print("Invalid input file.");
            System.exit(1);
        }

        this.commonPasswords = new HashSet<>();
        String line;

        try {
            while ((line = this.commonPasswordsReader.readLine()) != null)
                this.commonPasswords.add(line);
        } catch (IOException e) {
            System.err.println("Unable to convert dictionary");
            System.exit(1);
        }
            dictionaryCrack(this.commonPasswords);

    }
     
    private void mangle(HashSet<String> dictionary, int numberOfMangles) {
        
        if (numberOfMangles == 0) {
            return; 
        }
        
        HashSet<String> newWords = new HashSet<String>();
    
        for (String word : dictionary) {
            newWords.add(nCapitalize(word));
            newWords.addAll(toggleString(word));
            newWords.addAll(prependAppend(word));
            newWords.add(word + word);
            newWords.add(word.toUpperCase());
            newWords.add(reverse(word));  
        }
        dictionaryCrack(newWords);
        mangle(newWords, numberOfMangles -1);
        newWords.clear();

        for (String word : dictionary) {
            newWords.add(reflectPrepend(word));
            newWords.add(reflectAppend(word));
            newWords.add(letterToUpperCase(word));
            newWords.add(letterToLowerCase(word));
            newWords.add(deleteFirstLetter(word));
            newWords.add(deleteLastLetter(word));
        }
        dictionaryCrack(newWords);
        mangle(newWords, numberOfMangles -1);
        newWords.clear();
    }

    private String nCapitalize(String input) {
        if (input.length() > 0 && Character.isLowerCase(input.charAt(0))) {
            return input.substring(0, 1).toLowerCase() + input.substring(1).toUpperCase();
        }
        return input;
    }

    private String reverse(String word){
        return new StringBuilder(word).reverse().toString();
    }
    
    private String letterToUpperCase(String word){
        char[] charArray = word.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                if (Character.isLowerCase(charArray[i])) {
                    charArray[i] = Character.toUpperCase(charArray[i]);
                    break; 
                }
            }
        return new String(charArray);
    }

    private String reflectPrepend(String word){
        return word + new StringBuilder(word).reverse();
    }

    private String reflectAppend(String word){
        return new StringBuilder(word).reverse() + word;
    }

    private String letterToLowerCase(String word){
        char[] charArray = word.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            if (Character.isUpperCase(charArray[i])) {
                charArray[i] = Character.toLowerCase(charArray[i]);
                break; 
            }
        }
        return new String(charArray);
    }

    private String deleteFirstLetter(String word){
        if (word.length() == 1) {
            return word; 
        }
        return word.substring(1); 
    }

    private String deleteLastLetter(String word){
        if (word.length() == 1) {
            return word;
        }
        return word.substring(0, word.length() - 1); 
    }

    private ArrayList<String> prependAppend(String word){

        ArrayList<String> variations = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            variations.add(i + word);
            variations.add(word + i);  
        }
        return variations;
    }

    private ArrayList<String> toggleString(String word){
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        ArrayList<String> toggle = new ArrayList<String>();

        for (int i = 0; i < word.length(); i++) {
            char currentChar = word.charAt(i);
            if (i % 2 == 0) {
                sb1.append(Character.toUpperCase(currentChar));
                sb2.append(Character.toLowerCase(currentChar));
            } else {
                sb1.append(Character.toLowerCase(currentChar));
                sb2.append(Character.toUpperCase(currentChar));
            }
        }
        toggle.add(sb1.toString());
        toggle.add(sb2.toString());
        return toggle;
    }

    private void dictionaryCrack(HashSet<String> words) {
        ArrayList<Thread> threads = new ArrayList<>();
    
        for (User user : users) {
            Thread thread = new Thread(() -> {
                crackUserPasswords(user, words);
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        users.removeAll(tempUsers);
        tempUsers.clear();
    }
    
    private void crackUserPasswords(User user, HashSet<String> words) {
        for (String word : words) {
            if (jcrypt.crypt(user.getSalt(), word).equals(user.getSalt() + user.getHashValue())) {
                System.out.println(word);
                synchronized (tempUsers) {
                    tempUsers.add(user);
                }
                break;
            }
        }
    }
    
    private void seedUsernames() {
        for (User user : this.users) {
            this.dictionary.add(user.getFirstName());
            this.dictionary.add(user.getSurname());
            this.dictionary.add(user.getSurname() + user.getFirstName());
            this.dictionary.add(user.getFirstName() + user.getSurname());
        }
    }

    private void createUsers(){
        String line;
        users = new ArrayList<User>();

        try{
            while ((line = this.passwordReader.readLine()) != null) {
                String[] parts = line.split(":");
                String salt = parts[1].substring(0,2);
                String hashValue = parts[1].substring(2,parts[1].length());
                String name = parts[4];
                    
                if(name.contains("."))
                    name = trimUserName(name);
                
                int i = name.indexOf(' ');
                String firstName = name.substring(0, i).toLowerCase();
                String surname = name.substring(i+1, name.length()).toLowerCase();
                this.users.add(new User(firstName, surname, salt, hashValue));
            }
        }
        catch(IOException e){
            System.err.println("Failed to convert users list.");
        }
    }

    private String trimUserName(String name){
        int dotIndex = name.indexOf('.');
        return name.substring(0, dotIndex -1) + name.substring(dotIndex + 2, name.length());    
    }

    private void createDictionary() {
        this.dictionary = new HashSet<String>();
        String line;
    
        try {
            while ((line = this.dictionaryReader.readLine()) != null) {
                dictionary.add(line);
            }
        } catch (IOException e) {
            System.err.println("Unable to convert dictionary");
            System.exit(1);
        }
    }

    public static void validateArgs(String[] args){
        if(args.length != 2){
            System.err.println("Please pass two arguments: java PasswordCrack <dictionary> <password>");
            System.exit(1);
        }
    }

    private void validateInputFiles (String[] files){
        try{
            this.dictionaryReader = new BufferedReader(new FileReader(files[0]));
            this.passwordReader = new BufferedReader(new FileReader(files[1]));
        }
        catch(IOException e){
            System.err.print("Invalid input file.");
            System.exit(1);
        }
    }

}
