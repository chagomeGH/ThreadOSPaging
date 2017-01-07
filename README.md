# ThreadOSPaging
Enhanced Second Chance Algorithm
Operating Systems 
Paging

Descriptions:

1.	Purpose  

This assignment focuses on page replacement mechanism and the performance improvements achieved by implementing a buffer cache that stores frequently accessed disk blocks in memory.  

The Enhanced Second chance algorithm is implemented located on class Cache. Based on the descriptions taken form textbook – Operating System with Java Concepts 8th Edition Silberschatz Galvin, Gagne Chapter 9.4. Then its performance it is measured when running various test cases, and consider the merits and demerits of the implementation. 

2.	Disk Caching

Caching data from slower external main memory to the faster internal CPU memory takes advantage of both spatial and temporal locality of data reference. User programs are likely to access the same data or the memory locations very close to the data previously accessed. This is called spatial locality and is a key reason why an operating system reads a block or page of data from the disk rather than reading just the few bytes of data the program has accessed. User programs also tend to access the same data again in a very short period of time which in turn means that the least-recently used blocks or pages are unlikely to be used and could be the victims for page replacement. 

To accelerate disk access in ThreadOS, a cache is implemented that stores frequently accessed disk blocks into main memory. Therefore, subsequent access to the same disk block can quickly read the data cache in the memory. However, when the disk cache is full and another block needs to be cache, the OS must select a victim to replace. The Enhanced Second Chance algorithm is employed to choose the block to replace. If the block to be replace has been updated while in the cache it must be written back to disk; otherwise, it can just be overwritten with the new block. To maintain this data consistency between disk and cache block, each cache block must have the following entry information:

Entry Items	Descriptions

block frame number: 	
Contains the disk block number of cached data. It should be se to -1 this entry does not have valid block information.
reference bit:
Is set to 1 or true whenever this block is accessed. Reset it to 0 of false by the Enhanced-Second-Chance algorithm when searching the next victim.
dirty bit:
Is set to 1 or true whenever this block is written. Reset it to 0 or false when this block is written back to the disk.





 

