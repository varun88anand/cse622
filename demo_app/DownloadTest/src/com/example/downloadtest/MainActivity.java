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
	private final String TestURL = "http://www-student.cse.buffalo.edu/~varunana/temp/100_MB.txt";//"http://www.google.com/robots.txt";
	private final String DownloadDir = "/DownloadTest/";
	private final String DownloadFile = "file.txt";
	private final String TAG = "DownloadTest";
	private TextView textView;
	//private EditText editText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Get textView id
		textView = (TextView) findViewById(R.id.text_view);
		textView.setMovementMethod(new ScrollingMovementMethod());
		// Get editText id
		//editText = (EditText) findViewById(R.id.edit_message);
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
		//String pieceSize = editText.getText().toString();
		//Log.d(TAG, "Input: " + editMsg);
		new DownloadFileTask().execute(TestURL);		
	}
	
	private class DownloadFileTask extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			// get a piece size first
			//int pieceSize = 10000000;
			/*if (!params[1].equals("")) {
				pieceSize = Integer.parseInt(params[1]);
				publishProgress("Input piece size: " + params[1]);
			} else {
				publishProgress("No input piece size. Download with maximum range.");
			}*/
			
			// open output stream from file
			File file = getFileInSdcard();
			FileOutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream(file, true);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// use HTTP URL to download file
			boolean continueDownload = true;
			/*int startByte = 0;
			int endByte = startByte + pieceSize;
			*/URL url = null;
			HttpURLConnection urlConnection = null;
			InputStream inputStream = null;
			
			while (continueDownload) {
				try {
					url = new URL(params[0]);
					urlConnection = (HttpURLConnection)url.openConnection();
					//urlConnection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
					urlConnection.setDoInput(true);
					urlConnection.setDoOutput(true);
					publishProgress("Downloading..."); 
					inputStream = new BufferedInputStream(urlConnection.getInputStream());
					// getContentLength() doesn't work.
					//int fileSize = urlConnection.getContentLength();
					
					//range: " + startByte + " - " + endByte);
					// save output stream to file
					saveStreamToSdcard(inputStream, outputStream);
					
					// next download range
					/*startByte+=(pieceSize+1);
					endByte = startByte + pieceSize;
					*/continueDownload = false;
					publishProgress("Done...");
					//printStream(in);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.d(TAG, "out of range");
					continueDownload = false;
					//e.printStackTrace();
				} finally {
					urlConnection.disconnect();
				}
			}
			
			try {
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			publishProgress("Downloading is done.");
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
				
		//@Override
		protected void onProgressUpdate(String... params) {
			//textView.append("line:" + "\n" + params[0] + "\n");
			textView.append(params[0] + "\n");	
		}
	}
	
	// Called when users click the clean button
	public void cleanTest(View view) {
		textView.setText(InitString + "\n");
		// delete download file if existing
		File sdCard = Environment.getExternalStorageDirectory();
		File sdFile = new File(sdCard.getAbsolutePath() + DownloadDir + DownloadFile);
		if (sdFile.exists())
			sdFile.delete();
	}
}