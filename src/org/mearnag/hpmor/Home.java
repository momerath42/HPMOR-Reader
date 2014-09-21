package org.mearnag.hpmor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.mearnag.hpmor.Downloader.DownloaderBinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class Home extends Activity implements OnInitListener, OnUtteranceCompletedListener, OnItemClickListener {
	private static final String TAG = "Home";
	private static final String END_OF_LINE = "end of line";
	private Downloader downloader;
	private boolean mBound = false;
	private static final int MY_DATA_CHECK_CODE = 1;
    private TextToSpeech mTts;
	private HashMap<String, String> myHashAlarm = new HashMap<String,String>();
	private ArrayList<String> chapterLines;
	private boolean waitingToFinishSpeaking;
	private int chapter = 0;
	private int line = 0;
	private boolean pause = false;
	
	private ListView chapterLV;
	private ArrayAdapter<String> chapterAdapter;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
        public void onServiceConnected(ComponentName className,IBinder service) {
            DownloaderBinder binder = (DownloaderBinder) service;
            downloader = binder.getService();
            mBound = true;
            
    		startTTS();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		chapter = savedInstanceState.getInt("chapter");
		line = savedInstanceState.getInt("line");
		super.onRestoreInstanceState(savedInstanceState);
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("chapter", chapter);
		outState.putInt("line", line);
		super.onSaveInstanceState(outState);
	}
	@Override
	protected void onStart() {
		super.onStart();
		Log.v(TAG,"onStart");
		if (!mBound) {
			Intent intent = new Intent(this, Downloader.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
		if (chapter > 0) {
			Log.v(TAG,"onStart doesn't work like I thought");
		} else {
			SharedPreferences settings = getPreferences(0); // MODE_PRIVATE
			chapter = settings.getInt("chapter", 0);
			line = settings.getInt("line", 0);
		}
	}
	@Override
	protected void onStop() {
		super.onStop();
		if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
	}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        chapterLV = (ListView)findViewById(R.id.contents);
        Log.v(TAG,"onCreate");
    }
	@Override
	protected void onDestroy() {
		mTts.shutdown();
		SharedPreferences settings = getPreferences(0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("chapter", chapter);
		editor.putInt("line", line);
		editor.commit();
		super.onDestroy();
	}

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == MY_DATA_CHECK_CODE) {
    		if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
    			// success, create the TTS instance
    			mTts = new TextToSpeech(this, this);
    		} else {
    			// missing data, install it
    			Intent installIntent = new Intent();
    			installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
    			startActivity(installIntent);
    		}
    	}
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.skip_to_chapter:
				Log.v(TAG,"Skip to Chapter");
				final EditText input = new EditText(this);
				input.setInputType(InputType.TYPE_CLASS_NUMBER);
				
				new AlertDialog.Builder(this)
			    .setTitle("Skip to Chapter")
			    .setView(input)
			    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int whichButton) {
			        	startChapter(Integer.parseInt(input.getText().toString()));
			        	speakLine();
			        }
			    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int whichButton) {
			            // Do nothing.
			        }
			    }).show();
				return true;
			case R.id.pause:
				pause = !pause;
				if (pause == false) {
					mTts.speak(chapterLine(line), TextToSpeech.QUEUE_ADD, myHashAlarm);
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		Log.v(TAG,"onItemClick("+arg0+","+arg1+","+position+","+arg3+")");
		line = position - 1;
	}
	
	private void startTTS() {
		myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, END_OF_LINE);

        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
	}
	@Override
	public void onInit(int arg0) {
		Log.v(TAG,"onInit");
		mTts.setLanguage(Locale.US);
		mTts.setOnUtteranceCompletedListener(this);
		String speech;
		if (chapter == 0) {
			mTts.speak("Initializing!", TextToSpeech.QUEUE_FLUSH, null);
			// temp kludge
			chapter = 2;
			startNextChapter();
		}
		if (mBound) {
			if (chapterLines == null) {
				int savedLine = line;
				startChapter(chapter);
				line = savedLine;
			}
			speech = chapterLine(line);
		} else {
			Log.v(TAG,"not bound to downloader");
			speech = "ERROR: not bound to downloader";
		}
		mTts.speak(speech, TextToSpeech.QUEUE_ADD, myHashAlarm);
	}	
	private void startNextChapter() {
		chapter = chapter + 1;
		startChapter(chapter);
	}
	private void startChapter(int chapterid) {
		chapter = chapterid;
		line = 1;
		loadChapter(chapter);
		// find the real beginning
		while(chapterLines.size() > line && !chapterLines.get(line).contains("<div id=\"chapter-title\"")) {
			line = line + 1;
		}
	}
	private void speakLine() {
		if (waitingToFinishSpeaking) {
			Log.v(TAG,"Race detected");
		} else {
			waitingToFinishSpeaking = true;
			mTts.speak(chapterLine(line), TextToSpeech.QUEUE_ADD, myHashAlarm);
			runOnUiThread(new Runnable() {
				public void run() {
					chapterLV.setSelection(line);
				}
			});
		}
	}
	private void loadChapter(int chapterid) {
		Log.v(TAG,"loadChapter("+chapterid+")");
		boolean result = downloader.downloadFile(chapterURL(chapterid), chapterFilename(chapterid));
		Log.v(TAG,"loadChapter("+chapterid+") downloadFile returned:"+result);
		chapterLines = downloader.readLinesFromFile(chapterFilename(chapterid));
		Log.v(TAG,"loadChapter("+chapterid+") chapterLines size:"+chapterLines.size());
		final Home activity = this;
		final int newChapterId = chapterid;
		runOnUiThread(new Runnable() {
        	public void run() {
				chapterAdapter = new ArrayAdapter<String>(activity,R.layout.row_identity,chapterLines);
				Log.v(TAG,"loadChapter("+newChapterId+") adapter:"+chapterAdapter);
				chapterLV.setAdapter(chapterAdapter);
				chapterLV.setOnItemClickListener(activity);
			}
		});
		
	}
	private String chapterFilename(int chapterid) {
		return "chapter-"+chapterid+".txt";
	}
	private String chapterURL(int chapterid) {
		return "http://www.elsewhere.org/rationality/chapter/"+chapterid;
		//return "http://www.fanfiction.net/s/5782108/"+chapterid+"/Harry_Potter_and_the_Methods_of_Rationality";
	}
	private String chapterLine(int linenum) {
		if (chapterLines.size() < linenum) {
			Log.e(TAG,"chapterLine("+linenum+") size:"+chapterLines.size()+" chapter:"+chapter);
			return "Error";
		} else {
			String l = chapterLines.get(linenum);
			return android.text.Html.fromHtml(l).toString();
		}
	}
	@Override
	public void onUtteranceCompleted(String utteranceId) {
		//Log.v(TAG,"onUtteranceCompleted("+utteranceId+")");
		waitingToFinishSpeaking = false;
		if (utteranceId.equals(END_OF_LINE)) {
			//Log.v(TAG,"onUtteranceCompleted EOL!");
			if (pause) {
				Log.v(TAG,"paused!");
			} else {
				line = line + 1;
				if (chapterLines.size() < line || chapterLines.get(line).contains("div id=\"nav-bottom\"")) {
					mTts.speak("End of Chapter "+chapter, TextToSpeech.QUEUE_ADD, null);
					startNextChapter();
				}
				speakLine();
			}
		}
	}
}