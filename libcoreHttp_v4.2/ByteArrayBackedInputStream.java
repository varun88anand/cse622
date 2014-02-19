package libcore.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
/*
								Special Case - 
	Both workers are running and one of them is slow to an extent that, 
	the buffer gets completely filled up by the other worker. So, in this
	case, all the write() calls would block as it will wait for buffer to 
	be empty and the read() calls would block if "nextReadKey" is not available
	in the buffer. Deadlock, eh!
*/


public class ByteArrayBackedInputStream extends InputStream {
	private HashMap<Integer, byte[]> bucket;
    private HashSet<Integer> completedChunks;
	
	private int noOfBuckets;
	private int maxCap;
    private int size;
	
	private int nextReadKey;
    
	private int reads;
	private int writes;
	
	private boolean isSpecialCase;

    private final int WIFI = 1;
    private final int MOBILE = 2;
    
    private int wifiCounter = 0, mobileCounter = 0;
    public Object lock;
    
	private HttpHelper helper;

	public ByteArrayBackedInputStream(HttpHelper helper, int noOfBuckets, int size) {
		this.helper = helper;
		bucket = new HashMap<Integer, byte[]>(noOfBuckets);
		completedChunks = new HashSet<Integer>();
		this.noOfBuckets = noOfBuckets;
		maxCap = noOfBuckets - 2;
		this.size = size;
		lock = new Object();
		nextReadKey = 0;
		reads = writes = 0;
		isSpecialCase = false;
	}
	
	public int wifi_bytes() {
		return wifiCounter;
	}
	
	public int mobile_bytes() {
		return mobileCounter;
	}
	
    public void write(int key, byte arr[], int TYPE, int length, long startTime, long endTime) throws InterruptedException {
        synchronized(lock) {
            if(completedChunks.contains(key)) {
            	return;
            }
        	if(bucket.size() == maxCap && !isSpecialCase) {
                System.out.println("622: ByteArrayBackedInputStream write()- Buffer full...waiting for it to be read");
                lock.wait();
            }
			
			if(bucket.size() == noOfBuckets - 1 && isSpecialCase && key != nextReadKey)
			{
				System.out.println("622: ByteArrayBackedInputStream write()- Discarding KEY = " + key + "...Special Case, only one bucket left..");
				helper.insertToMissingList(key);
				return;
			}
			System.out.println("622: ByteArrayBackedInputStream write()- Inserting KEY = " + key + " to the buffer");
            bucket.put(key, arr);
			completedChunks.add(key);
            if(arr != null) {
				helper.insertStatistics(key, startTime, endTime, TYPE);
				++writes;
			}
			if(TYPE == WIFI)
                wifiCounter += length;
            else if(TYPE == MOBILE)
                mobileCounter += length;
			
			
			if(isSpecialCase && key == nextReadKey)
            {	
				isSpecialCase = false;
				lock.notifyAll();
            }
			else if(!isSpecialCase)
				lock.notifyAll();
        }
    }
	

    @Override
    public int read(byte[] bytes) {
        synchronized(helper.lock) {
			if(reads == writes && helper.isComplete) {
        		System.out.println("622: ByteArrayBackedInputStream read() - Nothing to read, Download Complete");
        		int total = wifi_bytes() + mobile_bytes();
				helper.dumpStatistics(wifi_bytes(), mobile_bytes(), total);
				return -1;
        	}
		}
        synchronized(lock) {
            if(bytes.length < size)
                return -1;
            else 
            {
                while(!bucket.containsKey(nextReadKey)) {
					System.out.println("622: ByteArrayBackedInputStream read() - Waiting for data to be available for key = " + nextReadKey);
                	try {
						if(bucket.size() == maxCap) {
							System.out.println("622: ByteArrayBackedInputStream read() - Invoking SPECIAL CASE, notifying workers");
							isSpecialCase = true;
							helper.insertToMissingList(nextReadKey);
							lock.notifyAll();
						}
						lock.wait();
					} catch (InterruptedException e) {
						System.out.println("622 - ByteArrayBackedInputStream: - Exception in read()...");
						e.printStackTrace();
						return -1;
					}
                }
                
                System.out.println("622: ByteArrayBackedInputStream read() - Reading data corresponding to key = " + nextReadKey);

				byte data[] = bucket.get(nextReadKey);
                if(data == null) {
					System.out.println("622: ByteArrayBackedInputStream read() - Nothing to read, Download Complete");
					int total = wifi_bytes() + mobile_bytes();
					//System.out.println("622 - STATISTICS: WIFI = " + wifiCounter + "/" + total + " and MOBILE = " + mobileCounter + "/" + total);
					helper.dumpStatistics(wifi_bytes(), mobile_bytes(), total);
					return -1;
				}
				System.arraycopy(data, 0, bytes, 0, data.length);
                bucket.remove(nextReadKey);
				nextReadKey += size;
                ++reads;
                lock.notifyAll();
                return data.length;
            }
        }
        
    }
	
	@Override
	public int read(byte[] bytes, int off, int len) {
		// @TODO Implement this
		return -1;
	}
	
	@Override
	public int read() throws IOException {
		// @TODO Implement this
		return -1;
	}
	
}

