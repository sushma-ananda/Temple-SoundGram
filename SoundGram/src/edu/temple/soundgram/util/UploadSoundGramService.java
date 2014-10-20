package edu.temple.soundgram.util;

import java.io.File;

import edu.temple.soundgram.MainActivity;
import edu.temple.soundgram.R;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class UploadSoundGramService extends IntentService {
	
	public static final String image = "image", audio = "audio", directory = "directory", description = "descriptionKey",
			visibilityKey = "visibility";
	public static final String REFRESH_ACTION = "refreshStream";
	

	public UploadSoundGramService(){
		super("default");
	}
	
	public UploadSoundGramService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			
			boolean status = API.uploadSoundGram(this, 
					1111,
					new File(intent.getStringExtra(image)), 
					new File(intent.getStringExtra(audio)), 
					"No description");
			
			if (status) {
				createNotification(this, 
						"SoundGram Uploaded", 
						"Your soundgram has been successfully uploaded",
						MainActivity.class.getName());
			
				// Delete local copy
				deleteRecursive(new File(intent.getStringExtra(directory)));
			}
			
			//Broadcast completion to refresh streams.
			Intent refreshIntent = new Intent().setAction(REFRESH_ACTION);
			sendBroadcast(refreshIntent);
			
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static void deleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            deleteRecursive(child);
	}
	
	public static void createNotification(Context context, String title, String text, String mClass){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
                        .setContentTitle(title)
                        .setContentText(text)
                        .setLights(Color.MAGENTA, 1000, 4000)
                        .setDefaults(Notification.DEFAULT_SOUND);
        long[] pattern = {1000, 1000};
        mBuilder.setVibrate(pattern);

        Intent resultIntent = null;
        try {
        	resultIntent = new Intent(context, Class.forName(mClass)).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } catch (Exception e) {
        	e.printStackTrace();
        }

        
// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(1000, mBuilder.build());
    }
}
