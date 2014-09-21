package org.mearnag.hpmor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class Downloader extends Service {

    private static final String TAG = "Downloader";
	private final IBinder mBinder = new DownloaderBinder();

    public class DownloaderBinder extends Binder {
        Downloader getService() {
            return Downloader.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    public ArrayList<String> readLinesFromFile(final String filename) {
		ArrayList<String> lines = new ArrayList<String>();
		try {
			InputStream instream = openFileInput(filename);
			if (instream != null) {
				InputStreamReader inputreader = new InputStreamReader(instream);
				BufferedReader buffreader = new BufferedReader(inputreader);
				String line;
				while ((line = buffreader.readLine()) != null) {
					lines.add(line);
				}
			}
			instream.close();
		} catch (java.io.FileNotFoundException e) {
			Log.e(TAG,"file not found:"+filename);
		} catch (IOException e) {
			Log.e(TAG,"IOException:"+e);
		}
    	return lines;
    }
    
    public Boolean downloadFile(final String url, String outfilename) {
        Log.i(TAG,"DownloadFile: " + url +" , "+ outfilename);

        File outfile = new File(outfilename);
        long existingSize = outfile.length();

        final DefaultHttpClient client = new DefaultHttpClient();
        final HttpGet getRequest = new HttpGet(url);
        if (existingSize > 0)
            getRequest.addHeader("Range","bytes="+existingSize+"-");
        try {
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 30000);
            HttpConnectionParams.setSoTimeout(client.getParams(), 30000);
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK && statusCode != 206) { 
                Log.e(TAG,"Error " + statusCode + " while retrieving file from from " + url);
                return false;
            }
            Header[] headers = response.getHeaders("Content-Length");
            //int expectedsize = Integer.valueOf(headers[0].getValue());
            
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    inputStream = entity.getContent(); 
                    outputStream = openFileOutput(outfilename, 0);//new FileOutputStream(outfilename);
                    byte[] rb = new byte[8196]; //2048];
                    int numbytesread;
                    int bytesreadsofar = 0;

                    while(true) {
                        numbytesread = inputStream.read(rb, 0, 8196); //2048);
                        bytesreadsofar += numbytesread;
                        if(numbytesread == -1) {
                            break;
                        }
                        outputStream.write(rb, 0, numbytesread);
                    }
                } catch (final Exception e) {
                    // Could provide a more explicit error message for IOException, IllegalStateException or FileNotFound
                    getRequest.abort();
                    Log.e(TAG,"Error while retrieving file from " + url + " exception: " + e.toString());
                    return false;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();  
                    }
                    if (outputStream != null) {
                        outputStream.close();  
                    }
                    entity.consumeContent();
                }
            }
        } catch (java.net.UnknownHostException e) {
            getRequest.abort();
            Log.e(TAG,"Error while retrieving file from " + url + " Unknown Host");
            return false;
        } catch (final Exception e) {
            // Could provide a more explicit error message for IOException or IllegalStateException
            getRequest.abort();
            Log.e(TAG,"Error retrieving file from " + url + " exception: " + e.toString());
            return false;
        } finally {
            if (client != null) {
                //client.close();
            }
        }
        return true;
    }
}
