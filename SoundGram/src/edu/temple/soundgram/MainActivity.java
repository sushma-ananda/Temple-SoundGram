package edu.temple.soundgram;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.app.Activity;
import edu.temple.soundgram.util.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	int userId = 111;
	Boolean flag;

	int TAKE_PICTURE_REQUEST_CODE = 11111111;
	int RECORD_AUDIO_REQUEST_CODE = 11111112;
	
	File photo, audio;
	
	LinearLayout ll;
	
	
	// Refresh stream
	private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        	if (intent.getAction().equals(UploadSoundGramService.REFRESH_ACTION)){
        		try {
        			loadStream();
        		} catch (Exception e) {}
        	}
        }
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		// Register listener for messages received while app is in foreground
        IntentFilter filter = new IntentFilter();
        filter.addAction(UploadSoundGramService.REFRESH_ACTION);
        registerReceiver(refreshReceiver, filter);
		
		
		ll = (LinearLayout) findViewById(R.id.imageLinearLayout);
		
		loadStream();
		//Creating cache directories for Soundgram - tuf77221
		File dir = new File(Environment.getExternalStorageDirectory().toString()+"/"+getString(R.string.app_name));
		dir.mkdir();
		dir = new File(Environment.getExternalStorageDirectory().toString()+"/"+getString(R.string.app_name)+"/cache");
		dir.mkdir();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.new_soundgram:
			newSoundGram();
			return true;
		case R.id.load_soundgram:
			loadStream();
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	Uri imageUri;
	private void newSoundGram(){
		
		Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		File storageDirectory = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name));
		
		storageDirectory.mkdir();
		
		photo = new File(storageDirectory, String.valueOf(System.currentTimeMillis()) + ".jpg"); // Temporary file name
		pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(photo));
		
		imageUri = Uri.fromFile(photo);
		startActivityForResult(pictureIntent, TAKE_PICTURE_REQUEST_CODE); // Launches an external activity/application to take a picture
		
		Toast.makeText(this, "Creating new SoundGram", Toast.LENGTH_LONG).show();
	}
	ImageView imageView;
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK && requestCode == TAKE_PICTURE_REQUEST_CODE) {
			
			imageView = new ImageView(this);
			
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(600, 600); // Set our image view to thumbnail size

			imageView.setLayoutParams(lp);

			ImageLoader.getInstance().displayImage(imageUri.toString(), imageView);
			getAudioClip();
			
			
		} else if (resultCode == Activity.RESULT_OK && requestCode == RECORD_AUDIO_REQUEST_CODE){
			
			imageView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//commented for now as this is handled while caching audio files - tuf77221
					MediaPlayer mPlayer = new MediaPlayer();
			        try {
			            mPlayer.setDataSource(audio.toString());
			            mPlayer.prepare();
			            mPlayer.start();
			        } catch (IOException e) {
			            e.printStackTrace();
			        }					 
				}
			});
			
			//addViewToStream(imageView);
			
			uploadSoundGram();
		}
		
	}
	
	private void getAudioClip(){
		Intent audioIntent = new Intent(this, RecordAudio.class);
		File storageDirectory = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name));
		audio = new File(storageDirectory, String.valueOf(System.currentTimeMillis())); // Temporary file name
		
		audioIntent.putExtra("fileName", audio.getAbsolutePath());
		
		startActivityForResult(audioIntent, RECORD_AUDIO_REQUEST_CODE);
	}
	
	
	private void addViewToStream(View view){
		ll.addView(view);
		
		
		View seperatorLine = new View(this);
        TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        layoutParams.setMargins(30,30,30,30);
        seperatorLine.setLayoutParams(layoutParams);
        seperatorLine.setBackgroundColor(Color.rgb(180, 180, 180));
        ll.addView(seperatorLine);
	}
	
	private void uploadSoundGram(){
		
		Intent uploadSoundGramIntent = new Intent(this, UploadSoundGramService.class);
		uploadSoundGramIntent.putExtra(UploadSoundGramService.directory, Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name));
		uploadSoundGramIntent.putExtra(UploadSoundGramService.image, photo.getAbsolutePath());
		uploadSoundGramIntent.putExtra(UploadSoundGramService.audio, audio.getAbsolutePath());

		startService(uploadSoundGramIntent);
		Toast.makeText(this, "Uploading SoundGram", Toast.LENGTH_SHORT).show();
	}
	
	private void loadStream(){
		
		Thread t = new Thread(){
			@Override
			public void run(){
				try {
					JSONArray streamArray = API.getSoundGrams(MainActivity.this, userId);
					
					Message msg = Message.obtain();
					msg.obj = streamArray;
					
					displayStreamHandler.sendMessage(msg);
				} catch (Exception e) {
				}
			}
		};
		t.start();
		
	}
	
	Handler displayStreamHandler = new Handler(new Handler.Callback() {
		
		@Override
		public boolean handleMessage(Message msg) {
			
			
			JSONArray streamArray = (JSONArray) msg.obj;
			if (streamArray != null) {
				ll.removeAllViews();
				for (int i = 0; i < streamArray.length(); i++){
					try {
						addViewToStream(getSoundGramView(streamArray.getJSONObject(i)));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
			return false;
		}
	});
	
	
	//handler for playing audio in media player from cache - tuf77221
		final Handler playAudioFile = new Handler(new Handler.Callback() {
			
			@Override
			public boolean handleMessage(Message msg) {
				// TODO Auto-generated method stub
				//Load browser
				playAudio(((File)msg.obj).toString());
				Log.i("Soundgram Cache", "File created. Played from Cache.");
				Log.e(INPUT_SERVICE, msg.obj.toString());
				return false;
			}
		});
	
	private View getSoundGramView(final JSONObject soundgramObject){
		LinearLayout soundgramLayout = new LinearLayout(this);
		
		
		ImageView soundgramImageView = new ImageView(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(600, 600); // Set our image view to thumbnail size
		soundgramImageView.setLayoutParams(lp);
		try {
			ImageLoader.getInstance().displayImage(soundgramObject.getString("image_url"), soundgramImageView);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		
		soundgramImageView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/*MediaPlayer mPlayer = new MediaPlayer(); 
						try { 
							//mPlayer.setDataSource(soundgramObject.getString("audio_url")); 
							mPlayer.setDataSource(audio.getAbsolutePath());
							mPlayer.prepare(); 
							mPlayer.start(); 
							} catch (Exception e) { 
								e.printStackTrace(); 
							} */
				
				//to play audio file from cache or save to cache and play
				String fileURL ="";
							try{				
								fileURL = soundgramObject.getString("audio_url");
							}catch(JSONException e){e.printStackTrace();}
							String fileName = fileURL.substring(fileURL.lastIndexOf("=")+1);
						String audioFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getString(R.string.app_name) +"/cache/"+fileName;
					       File output = new File(audioFilename);
					       if(output.exists() && output.isFile())
					        {
					        	playAudio(output.toString());
					        	Log.i("Soundgram Cache", "File exists. Played from Cache.");		        		
					        }
					        else{
					        	Log.i("Soundgram Cache", "Downloading audio file");
					        	//output.mkdir();
					        	Thread t = new Thread(){					        		
					        	@Override
					        	public void run(){
					        		 try
					     			{
					        			 String filePath ="";
											try{				
												filePath = soundgramObject.getString("audio_url");
											}catch(Exception e){e.printStackTrace();}
											String fileName = filePath.substring(filePath.lastIndexOf("=")+1);
						     				File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+getString(R.string.app_name)+"/cache/"+fileName);
						     				
						     				OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
					     				URL url = new URL(filePath);
					     				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					    				urlConnection.setRequestMethod("GET");
					    				urlConnection.setDoOutput(true);
					    				urlConnection.connect();
					     				InputStream input = urlConnection.getInputStream();

					     				int totalSize = urlConnection.getContentLength();
					     				int downloadSize = 0;
					     				
					     				byte[] buffer = new byte[4096];
					     				int bufferLen = -1;
					     				while((bufferLen = input.read(buffer)) != -1)
					     				{
					     					outputStream.write(buffer, 0, bufferLen);
					     				}
						     			outputStream.flush();
					     				outputStream.close();
					     				input.close();
						        		 Log.i("Soundgram Cache", "File downloaded");
						        		 Message msg = Message.obtain();
						        		 msg.obj = file;
						        		 playAudioFile.sendMessage(msg);
					     			}
					     			catch(MalformedURLException e)
					     			{ e.printStackTrace();}
					     			catch(IOException e)
					     			{ e.printStackTrace();}
					     			catch(Exception e)
					     			{ e.printStackTrace();}
					        	 }
						};
						t.start();
						
					  }
					      
					} 
				}); 
				soundgramLayout.addView(soundgramImageView); 
				return soundgramLayout; 

	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(refreshReceiver);
	}
	
	//to play audio file from cache - tuf77221
	public void playAudio(String filePathName)
	{
		  try {
	        	 
			  	 MediaPlayer mPlayer = new MediaPlayer();
			  	mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			  	mPlayer.setDataSource(filePathName);
			  	mPlayer.prepare();
			  	mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					
					@Override
					public void onPrepared(MediaPlayer mp) {
						// TODO Auto-generated method stub
						mp.start();
					}
				});
	            
	            //mPlayer.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	//in case to use AsyncTask - the code here - tuf77221
	public class saveFileFromURLToCache extends AsyncTask<String, Void, Boolean>
	{
		private Exception exception;
		
		@Override
		public Boolean doInBackground(String... fileStrings)
		{
			flag = false;
			try
			{
				String filePath = fileStrings[0];
				String audioFile = fileStrings[1];
				URL url = new URL(filePath);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.setDoOutput(true);
				urlConnection.connect();
				//File file = new File(Environment.getDownloadCacheDirectory(), audioFile);
				File file = new File(Environment.getExternalStorageDirectory()+"/"+getString(R.string.app_name)+"/cache", audioFile);
				if(!file.exists())
				{
					file.createNewFile();
				}
				FileOutputStream fileOut = new FileOutputStream(file);
				InputStream inputStream = urlConnection.getInputStream();
				int totalSize = urlConnection.getContentLength();
				int downloadSize = 0;
				
				byte[] buffer = new byte[1024];
				int bufferLen = 0;
				while((bufferLen = inputStream.read(buffer)) >0)
				{
					fileOut.write(buffer, 0, bufferLen);
					downloadSize += bufferLen;
				}
				if(downloadSize>0)
				{
					flag = true;
				}
				fileOut.close();
			}
			catch(MalformedURLException e)
			{ e.printStackTrace();}
			catch(IOException e)
			{ e.printStackTrace();}
			catch(Exception e)
			{ e.printStackTrace();}
			return flag;
			
		}
	}
	
}











