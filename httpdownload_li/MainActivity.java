package com.example.downloadtest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	private final String InitString = "Download status:";
	private final String DownloadDir = "/DownloadTest/";
	private final String DownloadFile = "test.txt";
	private final String MaxLength = "10000000";
	private final String TAG = "DownloadTest";
	
	//private final String TestURL = "http://www.google.com/robots.txt";
	private final String TestURL = "http://www.ietf.org/rfc/bcp-index.txt";
	
	private TextView textView;
	private EditText editText;
	
	private boolean continueDownload;
	private int startByte;
	private int endByte;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Get textView id
		textView = (TextView) findViewById(R.id.text_view);
		textView.setMovementMethod(new ScrollingMovementMethod());
		// Get editText id
		editText = (EditText) findViewById(R.id.edit_message);
		
		continueDownload = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	// Called when users click the start button
	public void startTest(View view) {
		// take piece size input
		String pieceSize = editText.getText().toString();
		if (!pieceSize.equals("")) {
			textView.append("Input piece size: " + pieceSize + "\n");
		} else {
			pieceSize = MaxLength;
			textView.append("No input piece size: Maximum range.");
		}
		
		startByte = 0;		
		//Log.d(TAG, "Input: " + editMsg);
		
		new DownloadFileTask().execute(TestURL, pieceSize);		
	}
	
	public void stopTest(View view) {
		continueDownload = false;
	}
	
	public void resumeTest(View view) {
		// take piece size input
		textView.append("Downloading resumed:" + "\n");
		String pieceSize = editText.getText().toString();
		if (!pieceSize.equals("")) {
			textView.append("Input piece size: " + pieceSize + "\n");
		} else {
			pieceSize = MaxLength;
			textView.append("No input piece size: Maximum range.");
		}
		
		continueDownload = true;
		new DownloadFileTask().execute(TestURL, pieceSize);	
	}
	
	private class DownloadFileTask extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			// open output stream from file
			File file = getFileInSdcard();
			FileOutputStream outputStream = null;
			
			try {
				outputStream = new FileOutputStream(file, true);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// prepare HTTP URL connection and download piece size		
			URL url = null;
			HttpURLConnection urlConnection = null;
			InputStream inputStream = null;
			int pieceSize = Integer.parseInt(params[1]);
			int maxSize = Integer.parseInt(MaxLength);
			
			// get object size from HTTP response header
			try {
				url = new URL(params[0]);
				urlConnection = (HttpURLConnection)url.openConnection();
				
				// show header info in log
				//getHttpResponseHeader(urlConnection);				

				// show content length in log before downloading
				//urlConnection.setRequestProperty("Range", "bytes=" + 0 + "-");
				//urlConnection.setRequestProperty("Accept-Encoding", "identity");
				Log.d(TAG, "pre-download file size is: " + getHttpContentLength(urlConnection));
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
						
			// start downloading			
			try {
				while (continueDownload) {
					//url = new URL(params[0]);
					urlConnection = (HttpURLConnection)url.openConnection();
					urlConnection.setRequestProperty("Accept-Encoding", "identity");					
					endByte = startByte + pieceSize - 1;					
					urlConnection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);					
					urlConnection.setDoInput(true);
					urlConnection.setDoOutput(true);					
					
					Log.d(TAG, "in-download file size is: " + getHttpContentLength(urlConnection));										
					
					inputStream = new BufferedInputStream(urlConnection.getInputStream());
					// getContentLength() doesn't work.
					//int fileSize = urlConnection.getContentLength();
					
					if (pieceSize != maxSize)
						publishProgress("Downloading range: " + (startByte + 1) + " - " + (endByte + 1));
					// save output stream to file
					saveStreamToSdcard(inputStream, outputStream);
					
					// next download range
					startByte+=pieceSize;					
					//printStream(in);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d(TAG, "out of range");
				continueDownload = false;
				publishProgress("Downloading is done.");
				//e.printStackTrace();
			} finally {
				urlConnection.disconnect();
			}
			
			
			// close output stream
			try {
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return null;
		}
		
		// Save file on SD card and close HTTP URL connection
		private void saveStreamToSdcard(InputStream in, FileOutputStream outPutStream) {
			// read input stream and store in a file
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];
			int len = 0;
			try {
				while ((len = in.read(buffer)) != -1) {
					outPutStream.write(buffer, 0, len);
				}
				outPutStream.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
		
		// Get file in SD card
		private File getFileInSdcard() {
			// create a directory to contain downloading file
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File(sdCard.getAbsolutePath() + DownloadDir);
			if(!dir.isDirectory())
				dir.mkdirs();
			File file = new File(dir, DownloadFile);
			return file;
		}
		
		// Print file on screen and close HTTP URL connection
		private void printStream(InputStream in) {
			InputStreamReader isReader = new InputStreamReader(in);
			BufferedReader bufReader = new BufferedReader(isReader);
			String line = null;
			try {
				while((line = bufReader.readLine()) != null) {
					publishProgress(line);				
				}				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		// Show HTTP Response Header in log
		private void getHttpResponseHeader(HttpURLConnection urlConnection) {
			Map<String, List<String>> map = urlConnection.getHeaderFields();
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				Log.d(TAG, "Key : " + entry.getKey() + ", Value : " + entry.getValue());
			}
		}
		
		// Get content length from HTTP Response Header before hand (before downloading) 
		private String getHttpContentLength(HttpURLConnection urlConnection) {			
			//urlConnection.setDoInput(true);
			//urlConnection.setDoOutput(true);
			String sLength = null;
			List values = urlConnection.getHeaderFields().get("content-length");
			if (values != null && !values.isEmpty()) {
				sLength = (String) values.get(0);				
			} else {
				sLength = "empty";
			}
	
			return sLength;
		}
				
		//@Override
		protected void onProgressUpdate(String... params) {
			//textView.append("line:" + "\n" + params[0] + "\n");
			textView.append(params[0] + "\n");	
		}
	}
	
	// Called when users click the clean button
	public void cleanTest(View view) {
		textView.setText(InitString + "\n");
		editText.setText("");
		
		// delete download file if existing
		File sdCard = Environment.getExternalStorageDirectory();
		File sdFile = new File(sdCard.getAbsolutePath() + DownloadDir + DownloadFile);
		if (sdFile.exists())
			sdFile.delete();
		
		continueDownload = true;
	}
}