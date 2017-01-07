/*
*   Mariana Chagoyan
*   Kevin Parker
*   CSS 430
*   Fall 2015
*   Test4.java
*   A user level testing prgram that conducts disk validity tests and measures buffer cache's improvement
*   performance in frequently-accessed disk blocks in memory.
*   Measure thi by creating four tests: Random Accesses, Localized Accesses, Mixed Accesses and Adversary Accessces.
*   Receives receives 2 arguments and performs a differnet test according to a combination of thoese arguemtns
*   The first argument directs a test that will use disk cache or not by using SysLib.rawread, SysLib.rawwrite,
*   and SysLib.sync system calls if the argument specifies disable meaning no disk cache. Otherwise, enable
*   specifies using SysLib.cread, SysLib.cwrite, and SysLib.csync sytem calls.
*   The second arguemnt  directs one of the above four performances tests.
*/

import java.lang.Integer;
import java.lang.String;
import java.lang.Thread;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.*;

public class Test4 extends Thread{

    private static final int diskBlockSize = 512;
    private static final int arraySize = 200;

    private byte[] writeCacheBlockArray;
    private byte[] readCacheBlockArray;

    private int testMethod = 0;
    private boolean cacheStatus = false;

    private long submissionReadTime;
    private long submissionWriteTime;
    private long completionReadTime;
    private long completionWriteTime;

    private Random randomGenerator;
    private String categoryTest = "";

    public Test4 (String args[]){

        if(args[0].equals("enabled")){

            cacheStatus = true;
        }
        else{

            cacheStatus = false;
        }

        testMethod = Integer.parseInt(args[1]);
        if(testMethod >= 1 && testMethod <=4)
            ;
        else
        {
            testMethod = 1;
            SysLib.cout("Invalid test type, defaulting to random access test \n");
        }

        writeCacheBlockArray = new byte[diskBlockSize];
        readCacheBlockArray = new byte[diskBlockSize];
        randomGenerator = new Random();
        randomGenerator.nextBytes(writeCacheBlockArray);

    }

    public void run(){

        SysLib.flush();

        switch (testMethod){

            case 1: randomAccesses();
                break;
            case 2: localizeAccesses();
                break;
            case 3: mixedAccesses();
                break;
            case 4: adversaryAccesses();
                break;
            default: SysLib.cout(" Invalid test type\n");
                break;
        }

        // validate and display results
        validateReadAndWriteBlocks();
        displayTestResults();

        sync();
        SysLib.exit();
    }

    /*
    *   randomAccesses
    *   Reads and writes many blocks randomly accross the disk and verifies the correctness of disk cache.
    *   Initiates by creating a byte array and filled with random generated data by calling randomGeneratorInteger
    */
    public void randomAccesses(){

        categoryTest = "Test Type: Random Access = ";
        int[] randomLocationArray = new int[arraySize];
        for (int i = 0; i < arraySize; i++){

            randomLocationArray[i]= randomGeneratorInteger(512);
        }
        submissionWriteTime = getTime();
        for (int i = 0; i < arraySize; i++){

            write(randomLocationArray[i], writeCacheBlockArray);
        }
        completionWriteTime = getTime();
        submissionReadTime = getTime();

        for(int i=0; i < arraySize; i++){

            read(randomLocationArray[i], readCacheBlockArray);
        }
        completionReadTime = getTime();
        //validateReadAndWriteBlocks();
        //displayTestResults();
    }

    /*
    *   localizeAccesses
    *    Reads and writes small selection of blocks many times to get a high ratio of cache hits
    *    Initiates by creating a byte array of 512 elements and fill it with data acting as a
    *    block of data. Calls writeCacheBlockArray to write the block of data to several blockIds
    *    that are closed togehter. Calls readCacheBlockArray to read the same blocks that were
    *    just written to see if their contents are the same as the byte array created previously
    */
    public void localizeAccesses(){

        categoryTest = " localize Accesses = ";
        submissionWriteTime = getTime();
        for (int i = 0; i < arraySize; i++){
            for(int j =0; j < 10; j++){
                write(j, writeCacheBlockArray);
            }
        }
        completionWriteTime = getTime();
        submissionReadTime = getTime();
        for (int i = 0; i < arraySize; i++){
            for(int j =0; j < 10; j++){
                read(j, readCacheBlockArray);
            }
        }
        completionReadTime = getTime();
        //validateReadAndWriteBlocks();
        //displayTestResults();
    }

    /*
    *   mixedAccesses
    *   Mixes disk accesses operations in which 90% of the total disk operations
    *   are localized and 10% are random accesses by using the Random class.
    *   Initiates by creating a mixedAccessesArray and calling randomGeneratorInteger
    *   to use the random class. If it is 0 to 8, it should be localized accesses an
    *   if it is 9, it should go random accesses.
    */
    public void mixedAccesses(){

        categoryTest = "Mixed Accesses =";
        int[] mixedAccessesArray = new int[arraySize];
        for(int i = 0; i < arraySize; i++){
            if(randomGeneratorInteger(10) < 9){
                mixedAccessesArray[i] = randomGeneratorInteger(10);
            }
            else {
                mixedAccessesArray[i] = randomGeneratorInteger(512);
            }
        }
        submissionWriteTime = getTime();

        for(int i=0; i < arraySize; i++){
            write(mixedAccessesArray[i], writeCacheBlockArray);
        }
        completionWriteTime = getTime();
        submissionReadTime = getTime();
        for (int i = 0; i < arraySize; i++){
            read(mixedAccessesArray[i], readCacheBlockArray);
        }
        completionReadTime = getTime();
        //validateReadAndWriteBlocks();
        //displayTestResults();
    }

    /*
    *   AdversaryAccesses
    *   Generates disk accesses that do not make good use of the disk cache each causes
    *   a cache miss. It chooses a block number which has not been chosen. Eac access moves
    *   a disk head a lot from one extreme point such as zero to the other extreme point
    *   such as 99 which belong to track 0,etc
    *
    */
    public void adversaryAccesses(){

        categoryTest = "Adversary Accesses =";

        int[] adversary = new int[arraySize];

        for(int i = 0; i < arraySize; i++){

            adversary[i] = (i % 11) * 90;

        }
        submissionWriteTime = getTime();

/*
        for(int i=0; i < diskBlockSize; i++){

            write(i, writeCacheBlockArray);
        }
*/
        for(int i=0; i < arraySize; i++){

            write(adversary[i], writeCacheBlockArray);
        }

        completionWriteTime = getTime();
        submissionReadTime = getTime();

        for(int i=0; i < arraySize; i++){

            read(i, readCacheBlockArray);
        }
        completionReadTime = getTime();
        //validateReadAndWriteBlocks();
        //displayTestResults();
    }

    public void sync(){

        if(cacheStatus){
            SysLib.csync();
        }
        else {
            SysLib.sync();
        }
    }

    public void read(int blockId, byte[] buffer){

        if(cacheStatus){
            SysLib.cread(blockId, buffer);
        }
        else {
            SysLib.rawread(blockId, buffer);
        }
    }

    public void write(int blockId, byte[] buffer){

        if(cacheStatus){
            SysLib.cwrite(blockId, buffer);
        }
        else {
            SysLib.rawwrite(blockId, buffer);
        }
    }

    public void validateReadAndWriteBlocks(){

        if(!Arrays.equals(readCacheBlockArray, writeCacheBlockArray)){

            SysLib.cout(" Read and Write blocks differ: \n");
        }
    }

    public void displayTestResults(){

        String categoryTestStatus = cacheStatus ? "Enabled" : "Disabled";
        SysLib.cout("Category test: " + categoryTest + " Disk Cache " + categoryTestStatus + "\n");
        SysLib.cout("Average Time Write: " + getAverageTimeWrite() + " msec Average Time Read :"
                + getAverageTimeRead() + " msec \n");
    }

    public int randomGeneratorInteger(int max){

        return (Math.abs(randomGenerator.nextInt() % max));
    }

    public long getAverageTimeWrite(){

        return (completionWriteTime - submissionWriteTime) /arraySize;
    }

    public long getAverageTimeRead(){

        return (completionReadTime - submissionReadTime) /arraySize;
    }

    public long getTime(){

        return new Date().getTime();
    }

}