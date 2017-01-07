/*
*   Mariana Chagoyan
*   Kevin Parker
*   CSS 430
*   Fall 2015
*   HW 4 - Caching
*   This class implements a cache for the ThreadOS using the enhanced second chance 
*   algorithm. The cache is structured as an array of entries (a privately defined sub-class)
*   that tracks the reference and dirty bits, and the actual byte data for the cache entry.
*   
*   We implement public methods for reading and writing data between a given block ID 
*   and byte buffer, as well as methods to sync the cache to disk and flush the cache. 
*   There are a number of private functions to assist with these functions, such as methods
*   to find a free cache block, or the next victim cache block, or assist with read/write 
*   functions and keep the cache entry meta data correct.
*/

import java.util.*;

public class Cache {


    /*
    *   Private data to describe the cache details - pagetable and frame size
     */
    private Entry[] pageTable = null;       // the page table [array]
    private int pageTableSize;              // Side of the page table
    private int blockFrameSize;             // Size of one frame

    /*
    *   Private variables to track data about cache and tracking bit status
     */
    private static int empty = -1;              // if empty, is -1
    private static int invalid = -2;            // if invalid, is -2
    private static boolean isSet = true;        // bit set = true
    private static boolean notSet = false;      // bit not set = false
    private int victimEntry;                    // ID of the next victim
    private int targetEntry;                    // compare the next target

    /*
    *   Page table entry
     */
    private class Entry {
        byte[] cacheBlockData;  // The data in the page
        int blockFrameNumber;   // its frame number
        boolean referenceBit;   // 1 if recently used, 0 if not
        boolean dirtyBit;       // 1 if dirty [not written to disk] 0 if clean

        /*
        *   Default Entry constructor
        *   Takes in an integer defining the size of the page and initializes data
         */
        private Entry(int blockFrameSize){
            cacheBlockData = new byte[blockFrameSize];
            blockFrameNumber = empty;
            referenceBit = false;
            dirtyBit = false;
        }
    }

    /*
    *   Default Cache Constructor
    *   Takes two integer values, the block size and number of blocks, to initialize a new cache
     */
    public Cache(int blockSize, int cacheBlocks) {

        // Create and initialize a new page table object of the given size, store the size in internal variable
        pageTable = new Entry[cacheBlocks];
        pageTableSize = cacheBlocks;
        for (int i=0; i<pageTableSize; i++)
        {
            pageTable[i] = new Entry(blockSize);
        }

        // Initialize the size of a frame
        blockFrameSize = blockSize;
        // Initialize next victim. Will start out at end (beginning) of table
        victimEntry = pageTableSize - 1;
    }

    /*
    *   Private helper function to find the next free page, if any.
    *   Loops through the page table and checks if the next block is empty.
    *   If so, we return that value. If not, we return -1
     */
    private int findFreePage() {

        // use helper method to find an empty page
        return (findPage(empty));
    }

    /*
    *   Private helper function to find the next victim. Used if we did not find any free pages.
     *   Implements the enhanced second change algorithm. Start at the beginning of the cache and look
     *   for an entry
     */
    private int nextVictim() {
        while(true)
        {
            // Move the victim entry to the next position.  Loops to beginning at end of table
            victimEntry = ((++victimEntry) % pageTableSize);
            // Check if the reference bit is toggled
            if(!(pageTable[victimEntry].referenceBit))
            {
                //if not set, this will be the next victim
                return victimEntry;
            }
            // otherwise, toggle it to 0 and check the next
            pageTable[victimEntry].referenceBit = notSet;
        }
//        return -1;
    }

    /*
    *   Private helper function to write data from a cache block to disk.
    *   Check if the cache entry's dirty bit is set, and it is not empty.
    *   If we meet these criteria, use syslib.rawwrite to write it
     */
    private void writeBack(int victimEntry) {
        // Check if entry is dirty, and not empty
        if((pageTable[victimEntry].dirtyBit)
                && (pageTable[victimEntry].blockFrameNumber!= empty))
        {
            // Write it to disk, update the dirty bit
            SysLib.rawwrite(pageTable[victimEntry].blockFrameNumber, pageTable[victimEntry].cacheBlockData);
            pageTable[victimEntry].dirtyBit = notSet;
        }
    }

    /*
    *   Public cache enabled read method.
    *   Reads a given block ID into a buffer
    *   We first validate the block ID, and then see if its in the cache to read.
    *   If it isnt in the cache, we see if we have any empty cache blocks to read it to
    *   If not that, then we use the enhanced second change algorithm to pick a victim and then read
     */
    public synchronized boolean read(int blockId, byte buffer[]) {
        // Make sure we have a valid block ID (non-negative)
        if(blockId < 0)
        {
            return false;
        }

        // Check if its already in the cache
        targetEntry = findPage(blockId);
        if(targetEntry != invalid)
        {
            // if it is (by being valid) read it and return true
            readCacheBlock(targetEntry, blockId, buffer);
            return true;
        }

        // next, look for an empty block to read to
        targetEntry = findFreePage();
        if (targetEntry != invalid)
        {
            // read from disk to cache
            SysLib.rawread(blockId, pageTable[targetEntry].cacheBlockData);
            // then read to buffer from cache
            readCacheBlock(targetEntry, blockId, buffer);
            return true;
        }

        // otherwise, must find a victim, replace, and then read
        writeBack(nextVictim());
        SysLib.rawwrite(blockId, pageTable[victimEntry].cacheBlockData);
        readCacheBlock(victimEntry, blockId, buffer);
        return true;

    }

    /*
    *   Public cache enabled write method
    *   Writes a buffer to a given block ID.
    *   First make sure the block ID is valid, and then find where we can write to.
    *   If its already in the cache, just update it.  If it isnt, find if we have any empty blocks
    *   and write to them.  IF not, then use the enhanced second chance algorithm and find a victim to replace
     */
    public synchronized boolean write(int blockId, byte buffer[]) {
        // Make sure we have a valid block ID (non-negative)
        if(blockId < 0)
        {
            return false;
        }

        // Check if its already in the cache
        targetEntry = findPage(blockId);
        if(targetEntry != invalid)
        {
            // if so, copy the buffer to that cache block and update
            copyIntoCacheBlock(targetEntry, blockId, buffer);
            return true;
        }

        // next try to find an empty block
        targetEntry = findFreePage();
        if(targetEntry != invalid)
        {
            // and copy that buffer to the empty block
            copyIntoCacheBlock(targetEntry, blockId, buffer);
            return true;
        }

        // otherwise, we must find a victim to save, then write the data
        writeBack(nextVictim());
        copyIntoCacheBlock(victimEntry, blockId, buffer);
        return true;

    }

    /*
    * Sync the cache. Go through each entry and write to disk if dirty
     */
    public synchronized void sync() {
        // Loop through the page table
        for (int i=0; i<pageTableSize; i++)
        {
            // write to disk if we need to
            writeBack(i);
        }
        // Sync
        SysLib.sync();
    }

    /*
    *   Flush the cache. Go through each entry and write to disk if needed and then
    *   clear it
     */
    public synchronized void flush()
    {
        // loop through the cache
        for (int i=0; i<pageTableSize; i++)
        {
            // Write to disk if necessary
            writeBack(i);
            // Update that block to reflect it is empty, clear reference bit
            updateCacheBlock(i, empty, notSet);
        }
        // sync
        SysLib.sync();
    }

    /*
    *   Private helper function to find a given page by the blockFrameNumber
    *   if we find the page, return its location in the page table.  If not,
    *   return -1
     */
    private int findPage(int blockID)
    {
        // Loop through the cache
        for (int i=0; i<pageTableSize; i++)
        {
            if(pageTable[i].blockFrameNumber == blockID)
            {
                return i;
            }
        }
        return invalid;
    }

    /*
    *   Private helper function to update the status of a cache block
    *   Get passed a cache block ID, the frame number, and what to set the reference bit to
     */
    private void updateCacheBlock(int pageToReplace, int frame, boolean refBit){

        // Update the frame number
        pageTable[pageToReplace].blockFrameNumber = frame;
        // Set the reference bit to the passed boolean value
        pageTable[pageToReplace].referenceBit = refBit;
    }

    /*
    *   Private helper function to read data from the buffer into the selected cache block.
    *   Uses arraycopy to copy the data, and then make sure the dirty bit and reference bits are set appropiately
     */
    private void copyIntoCacheBlock(int pageToReplace, int blockID, byte[] buffer)
    {
        // copy from buffer to cache
        System.arraycopy(buffer, 0, pageTable[pageToReplace].cacheBlockData, 0, blockFrameSize);
        // set the dirty bit
        pageTable[pageToReplace].dirtyBit = isSet;
        // Update the cache entry to the block we reference and that it has been referenced
        updateCacheBlock(pageToReplace, blockID, isSet);
    }

    /*
    *   Private helper function to read data from a cache block to the buffer.
    *   Also updates the reference bit to be set
     */
    private void readCacheBlock(int toRead, int blockID, byte[] buffer ){
        // copy from cache to buffer
        System.arraycopy(pageTable[toRead].cacheBlockData, 0, buffer, 0, blockFrameSize);
        // update cache entry to the block we've referenced and that it has been referenced
        updateCacheBlock(victimEntry, blockID, isSet);
    }
}
