package com.cse10.icommand;

import java.lang.ref.WeakReference;

import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class IncomingHandler extends Handler {
	private WeakReference<SpeechRecognitionService> mtarget;

	IncomingHandler(SpeechRecognitionService target) {
		mtarget = new WeakReference<SpeechRecognitionService>(target);
	}

	@Override
	public void handleMessage(Message msg) {
		final SpeechRecognitionService target = mtarget.get();

		switch (msg.what) {
		case Constants.MSG_RECOGNIZER_START_LISTENING:

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				// turn off beep sound
				target.mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			}
			if (!target.mIsListening) {
				target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
				target.mIsListening = true;
				Toast.makeText(target.getApplicationContext(), "Message start Listnening", Toast.LENGTH_LONG).show();
			}
			break;

		case Constants.MSG_RECOGNIZER_CANCEL:
			target.mSpeechRecognizer.cancel();
			target.mIsListening = false;
			break;
		}

	}
}
