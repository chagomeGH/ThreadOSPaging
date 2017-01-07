/*
*   Mariana Chagoyan
*   Kevin Parker
*   CSS 430
*   Fall 2015
*   Test4.java
*   Description: A user level testing prgram that conducts disk validity tests and measures the performance
*   Performs four tests: Random Accesses, Localized Accesses, Mixed Accesses and Adversary Accessces.
*   Receives receives 2 arguments and performs a differnet test according to a combination of thoese arguemtns
*   The first argument directs a test that will use disk cache or not by using SysLib.rawread, SysLib.rawwrite,
*   and SysLib.sync system calls if the argument specifies disable meaning no disk cache. Otherwise, enable
*   specifies using SysLib.cread, SysLib.cwrite, and SysLib.csync sytem calls.
*   The second arguemnt  directs one of the above four performances tests.
*/

import java.util.Arrays;
import java.util.Date;
import java.util.Random;

public class Test4 extends Thread{

    private static final int diskBlockSize = 512;
    private static final int arraySize = 200; // to be defined

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

    /*
    *   Default constructor. Expects two arguments from command line, 1: enabled/disabled to define if the cache is
    *   enabled or disabled. 2: integer values 1,2,3,4 to determine which type of test to run. Generates the thread
    *   based on that input and our earlier defined sizes, before it is able to run.
    */
    public Test4 (String args[]){

        // Read the first argument and set cache status
        if(args[0].equals("enabled"))
        {

            cacheStatus = true;
        }
        else
        {

            cacheStatus = false;
        }

        // test to run is 2nd argument, should be 1-4.
        // if it isnt, default to 1 and alert user
        testMethod = Integer.parseInt(args[1]);
        if(testMethod >=1 && testMethod <=4)
            ;
        else
        {
            testMethod = 1;
            SysLib.cout(" Invalid test type, defaulting to random access test \n");
        }

        // initialize read and write blocks
        writeCacheBlockArray = new byte[diskBlockSize];
        readCacheBlockArray = new byte[diskBlockSize];

        // Store a new random number for tests thats require random
        randomGenerator = new Random();
        randomGenerator.nextBytes(writeCacheBlockArray);
    }

    /*
    *   Main driver of the test.  When run, it switches on the 2nd command line argument and determines which test
    *   case to run.  Each valid case, 1-4, calls the testing algorithm that we wrote. Other entries trigger the default
    *   switch case and breaks out there.
     */
    public void run()
    {
        // Make sure recent tests are flushed
        SysLib.flush();

        // Decide which test to run
        switch (testMethod){

            case 1: randomAccesses();
                break;
            case 2: localizeAccesses();
                break;
            case 3: mixedAccesses();
                break;
            case 4: adversaryAccesses();
                break;
            // shouldnt be necessary due to checking in constructor, but do anyway
            default: SysLib.cout(" Invalid test type\n");
                break;
        }

        // validate and display results
        validateReadAndWriteBlocks();
        displayTestResults();
        //sync and exit
        sync();
        SysLib.exit();
    }

    /*
    *   randomAccesses
    *   Reads and writes many blocks randomly accross the disk and verifies the correctness of disk cache.
    *   Initiates by creating a byte array and filled with random generated data by calling randomGeneratorInteger
    */
    public void randomAccesses()
    {
        categoryTest = "Test Type: Random Access";
        int[]  randomLocationArray = new int[arraySize];

        // populate an array of randon integeres to write from buffer
        for (int i =0; i< arraySize; i++)
        {
            randomLocationArray[i] = randomGeneratorInteger(512);
        }
        // store the starting time
        submissionWriteTime = getTime();

        // Do the writing from buffer to cache/disk
        for (int i=0; i<arraySize; i++)
        {
            write(randomLocationArray[i], writeCacheBlockArray);
        }
        // store the completion of write time
        completionWriteTime = getTime();


        // start read test
        submissionReadTime = getTime();

        // Do the reading from disk/cache to buffer
        for (int i=0;i<arraySize;i++)
        {
            read(randomLocationArray[i], readCacheBlockArray);
        }

        //store completion of read time
        completionReadTime = getTime();

    }

    /*
    *   localizeAccesses
    *    Reads and writes small selection of blocks many times to get a high ratio of cache hits
    *    Initiates by creating a byte array of 512 elements and fill it with data acting as a
    *    block of data. Calls writeCacheBlockArray to write the block of data to several blockIds
    *    that are closed togehter. Calls readCacheBlockArray to read the same blocks that were
    *    just written to see if their contents are the same as the byte array created previously
    */
    public void localizeAccesses()
    {
        categoryTest = " localize Accesses ";
        // store time for start of write test
        submissionWriteTime = getTime();

        // do the write test,  keep inner loop small to simluate "local" data
        for (int i = 0; i < arraySize; i++){
            for(int j =0; j < 10; j++){
                write(j, writeCacheBlockArray);
            }
        }
        // store write end time
        completionWriteTime = getTime();

        // store read start time
        submissionReadTime = getTime();

        // do the read test. keep inner loop small to simulate "local" data
        for (int i = 0; i < arraySize; i++){
            for(int j =0; j < 10; j++){
                read(j, readCacheBlockArray);
            }
        }

        // store the read end time
        completionReadTime = getTime();
    }

    /*
    *   mixedAccesses
    *   Mixes disk accesses operations in which 90% of the total disk operations
    *   are localized and 10% are random accesses by using the Random class.
    *   Initiates by creating a mixedAccessesArray and calling randomGeneratorInteger
    *   to use the random class. If it is 0 to 8, it should be localized accesses an
    *   if it is 9, it should go random accesses.
    */
    public void mixedAccesses()
    {

        categoryTest = "Mixed Accesses";

        // per description, mixed acces is 90% local, 10% random
        //create an array to store the mixed results
        int[] mixedAccessesArray = new int[arraySize];
        for(int i = 0; i < arraySize; i++){
            // 90% of the time, we want local access, use 10
            if(randomGeneratorInteger(10) < 9){
                mixedAccessesArray[i] = randomGeneratorInteger(10);
            }
            // 10% of the time we want random, use 512
            else {
                mixedAccessesArray[i] = randomGeneratorInteger(512);
            }
        }

        // store write test start time
        submissionWriteTime = getTime();

        // do the writing
        for(int i=0; i < arraySize; i++){
            write(mixedAccessesArray[i], writeCacheBlockArray);
        }
        //store write test end time
        completionWriteTime = getTime();

        // store read test start time
        submissionReadTime = getTime();
        // do the reading
        for (int i = 0; i < arraySize; i++){
            read(mixedAccessesArray[i], readCacheBlockArray);
        }
        //store read test end time
        completionReadTime = getTime();
    }

    /*
    *   AdversaryAccesses
    *   Generates disk accesses that do not make good use of the disk cache each causes
    *   a cache miss. It chooses a block number which has not been chosen. Eac access moves
    *   a disk head a lot from one extreme point such as zero to the other extreme point
    *   such as 99 which belong to track 0,etc
    *
    */
    public void adversaryAccesses()
    {

        categoryTest = "Adversary Accesses";
        // generate an adversary array - blocks are groups by 100, so modulo 11 then multiply by 90 to get units
        // 100ish apart, but within bounds still..
        int [] adversary = new int[arraySize];
        for (int i = 0; i < arraySize; i++)
        {
            adversary[i] = (i%11)*90;
        }

        // store write test start time
        submissionWriteTime = getTime();

        // do the write test
        for(int i=0; i < arraySize; i++){

            write(adversary[i], writeCacheBlockArray);
        }
        // store write test end time
        completionWriteTime = getTime();

        // store read test start time
        submissionReadTime = getTime();
        // do the reading
        for(int i=0; i < arraySize; i++){

            read(adversary[i], readCacheBlockArray);
        }
        // store read test end time
        completionReadTime = getTime();
    }

    /*
    *   Method to call the correct version of sync.  If cache enabled, call csync.  If cache disabled, call sync.
     */
    public void sync(){

        if(cacheStatus){
            SysLib.csync();
        }
        else {
            SysLib.sync();
        }
    }

    /*
    *   Method to call the correct version of read.  If cache enabled, call cread.  If cache disabled, call rawread.
     */
    public void read(int blockId, byte buffer[]){

        if(cacheStatus){
            SysLib.cread(blockId, buffer);
        }
        else {
            SysLib.rawread(blockId, buffer);
        }
    }

    /*
    *   Method to call the correct version of write.  If cache enabled, call cwrite.  If cache disabled, call rawwrite.
    */
    public void write(int blockId, byte buffer[]){

        if(cacheStatus){
            SysLib.cwrite(blockId, buffer);
        }
        else {
            SysLib.rawwrite(blockId, buffer);
        }
    }

    public void validateReadAndWriteBlocks(){

        if(!Arrays.equals(readCacheBlockArray, writeCacheBlockArray)){

            SysLib.cout(" read and write block differ \n");
        }
    }

    /*
    *   Method to display on screen the results of the test.   Print the test type and then read and write results
     */
    public void displayTestResults(){

        String categoryTestStatus = cacheStatus ? "Enable" : "Disable";
        SysLib.cout("Category test: " + categoryTest + " Disk Cache" + categoryTestStatus + "\n");
        SysLib.cout("Average Time Write: " + getAverageTimeWrite() + "msec Average Time Read"
                + getAverageTimeRead() + " msec \n");
    }

    /*
    *   Quicker call to generate random integer for test case needs
     */
    public int randomGeneratorInteger(int max){

        return (Math.abs(randomGenerator.nextInt() % max));
    }

    /*
    *   Average write time is calculated as the difference between the end and start times, divided by array size
     */
    public long getAverageTimeWrite(){

        return (completionWriteTime - submissionWriteTime) /arraySize;
    }

    /*
    *   Average read time is calculated as the difference between the end and start times, divided by array size
    */
    public long getAverageTimeRead(){

        return (completionReadTime - submissionReadTime) /arraySize;
    }

    /*
    *   Quicker call to get the current time for generating timestamps.
     */
    public long getTime(){

        return new Date().getTime();
    }
}