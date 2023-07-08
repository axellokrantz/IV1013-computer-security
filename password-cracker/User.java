public class User {
    
    private String firstName;
    private String surname;
    private String salt;
    private String hashValue;

    public User(String name, String surname, String salt, String hashValue){
        this.firstName = name;
        this.surname = surname;
        this.salt = salt;
        this.hashValue = hashValue;
    }

    public String getFirstName(){
        return firstName;
    }

    public String getSurname(){
        return surname;
    }

    public String getSalt(){
        return salt;
    }

    public String getHashValue(){
        return hashValue;
    }

    @Override
    public String toString(){
        return firstName + " " + surname;
    }
}
