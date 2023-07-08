import java.util.Random;

public class MyRandom extends Random{

    private static final long m = 2147483647;
    private static final long a = 16807;
    private static final long b = 0;
    private long seed;
    private long x;

    MyRandom(){}

    MyRandom(long seed){
        setSeed(seed);
    }
    


    @Override
    public int next(int bitLength){
        int bits = (int) Math.pow(2, bitLength)-1;
        x = ((a * seed) + b) % m;
        seed = x;

        return bits & (int) x;
    }

    @Override
    public void setSeed(long seed){
        this.seed = seed;
    } 

    // Numbers taken from:
    // https://en.wikipedia.org/wiki/Linear_congruential_generator
    // Apple CarbonLib, C++11's minstd_rand0

    // m = 2147483647 (prime number)
    // multiplier a = 16807 (primitive root of m)
    // increment b = 0

}
