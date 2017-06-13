package com.wv.rizky;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: CameraFragment.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.wv.videoprocessing.NewExportPreset960x540Strategy;
import com.wv.videorecorder.CameraGLView;
import com.wv.videorecorder.encoder.MediaAudioEncoder;
import com.wv.videorecorder.encoder.MediaEncoder;
import com.wv.videorecorder.encoder.MediaMuxerWrapper;
import com.wv.videorecorder.encoder.MediaVideoEncoder;

import net.ypresto.androidtranscoder.MediaTranscoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CameraFragment extends Fragment {
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "CameraFragment";

	/**
	 * for camera preview display
	 */
	private CameraGLView mCameraView;
	/**
	 * button for start/stop recording
	 */
	private ImageView mRecordButton;
	/**
	 * muxer for audio/video recording
	 */
	private ImageView switchCam;
	private MediaMuxerWrapper mMuxer;
	private Chronometer timer;
	private ArrayList<String> videoList = new ArrayList<>();
	private TextView retakeText ,doneText;
	private ProgressDialog pd;
	private DisplayMetrics metrics;
	private Context mContext;

	public CameraFragment() {
		// need default constructor
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mContext = getActivity();
		final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		mCameraView = (CameraGLView)rootView.findViewById(R.id.cameraView);
		metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mCameraView.setVideoSize(metrics.widthPixels, metrics.heightPixels);
		mCameraView.setOnClickListener(mOnClickListener);
		mRecordButton = (ImageView) rootView.findViewById(R.id.camera_control);
//		mRecordButton.setOnClickListener(mOnClickListener);
		mRecordButton.setOnTouchListener(recordButtonOnTouch);
		switchCam = (ImageView) rootView.findViewById(R.id.cam_switch_button);
		switchCam.setOnClickListener(switchCamOnClick);
		timer = (Chronometer) rootView.findViewById(R.id.timer);
		doneText = (TextView) rootView.findViewById(R.id.done);
		doneText.setOnClickListener(doneOnClick);
		retakeText = (TextView) rootView.findViewById(R.id.retake);
		retakeText.setOnClickListener(retakeOnClick);
		pd = new ProgressDialog(getActivity());
		pd.setCancelable(false);
		pd.setMessage("Please wait....");
		pd.setProgressStyle(android.R.style.Widget_ProgressBar_Small);

		return rootView;
	}

	private boolean isRecordButtonPressed;

	private ImageView.OnTouchListener recordButtonOnTouch = new ImageView.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					isRecordButtonPressed = false;
					handler.postDelayed(mLongPressed, 600);
					return true;

				case MotionEvent.ACTION_UP:
					handler.removeCallbacks(mLongPressed);
					if (isRecordButtonPressed){
						timer.stop();
						stopRecording();
						mRecordButton.setImageResource(R.drawable.record_button);
					}
					isRecordButtonPressed = false;
					return true;
			}
			return false;
		}
	};

	final Handler handler = new Handler();
	Runnable mLongPressed = new Runnable() {
		public void run() {
			isRecordButtonPressed = true;
			mRecordButton.setImageResource(R.drawable.record_button_pressed);
			startRecording();
			timer.start();
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		if (DEBUG) Log.v(TAG, "onResume:");
		mCameraView.onResume();
	}

	@Override
	public void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		stopRecording();
		mCameraView.onPause();
		super.onPause();
	}

	/**
	 * method when touch record button
	 */
	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			switch (view.getId()) {
			case R.id.camera_control:
				if (mMuxer == null)
					startRecording();
				else
					stopRecording();
				break;
			}
		}
	};


	/**
	 * start resorcing
	 * This is a sample project and call this on UI thread to avoid being complicated
	 * but basically this should be called on private thread because prepareing
	 * of encoder is heavy work
	 */
	private void startRecording() {
		if (DEBUG) Log.v(TAG, "startRecording:");
		try {
			mRecordButton.setImageDrawable(getResources().getDrawable(R.drawable.record_button_pressed));
			mMuxer = new MediaMuxerWrapper(".mp4");	// if you record audio only, ".m4a" is also OK.
			if (true) {
				// for video capturing
				new MediaVideoEncoder(mMuxer, mMediaEncoderListener, metrics.widthPixels, metrics.heightPixels);
			}
			if (true) {
				// for audio capturing
				new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
			}
			mMuxer.prepare();
			mMuxer.startRecording();
		} catch (final IOException e) {
			mRecordButton.setColorFilter(0);
			Log.e(TAG, "startCapture:", e);
		}
	}

	/**
	 * request stop recording
	 */
	private void stopRecording() {
		if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
		mRecordButton.setImageDrawable(getResources().getDrawable(R.drawable.record_button));
		if (mMuxer != null) {
			mMuxer.stopRecording();
			mMuxer.getOutputPath();
			videoList.add(mMuxer.getOutputPath());
			mMuxer = null;
			// you should not wait here
		}
	}

	/**
	 * callback methods from encoder
	 */
	private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
		@Override
		public void onPrepared(final MediaEncoder encoder) {
			if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
			if (encoder instanceof MediaVideoEncoder)
				mCameraView.setVideoEncoder((MediaVideoEncoder)encoder);
		}

		@Override
		public void onStopped(final MediaEncoder encoder) {
			if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
			if (encoder instanceof MediaVideoEncoder)
				mCameraView.setVideoEncoder(null);
		}
	};

	private OnClickListener switchCamOnClick = new OnClickListener() {
		@Override
		public void onClick(View view) {
			mCameraView.switchCamera();
		}
	};

	private OnClickListener retakeOnClick = new OnClickListener() {
		@Override
		public void onClick(View view) {
			for (String path : videoList){
				File file = new File(path);
				boolean deleted = file.delete();
			}
			videoList.clear();
			timer.reset();
		}
	};

	private OnClickListener doneOnClick = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if(pd != null)
				pd.show();
			new ResampleTask().execute(videoList);
		}
	};

	class ResampleTask extends AsyncTask<List<String>, Void, String> {

		@Override
		protected void onPostExecute(String videoAudioData ) {
			compressVideo(videoAudioData);
		}

		@Override
		protected String doInBackground(List<String>... lists) {
			List<String> videoAudioData = lists[0];
			if (videoAudioData.size() > 1){
				try {
					List<Movie> movieList = new ArrayList<>();
					for (String path : videoAudioData){
						movieList.add(MovieCreator.build(path));
					}
					List<Track> videoTracks = new LinkedList<Track>();
					List<Track> audioTracks = new LinkedList<Track>();

					for (Movie m : movieList) {
						for (Track t : m.getTracks()) {
							if (t.getHandler().equals("soun")) {
								audioTracks.add(t);
							}
							if (t.getHandler().equals("vide")) {
								videoTracks.add(t);
							}
						}
					}

					Movie result = new Movie();

					if (audioTracks.size() > 0) {
						result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
					}
					if (videoTracks.size() > 0) {
						result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
					}

					String output = mContext.getExternalFilesDir(null).getAbsolutePath() + "/" + "output.mp4";
					Container out = new DefaultMp4Builder().build(result);
					FileOutputStream fos = new FileOutputStream(new File(output));
					out.writeContainer(fos.getChannel());
					fos.close();
					return output;
				} catch (IOException e){

				}
			}
			return videoAudioData.get(0);
		}
	}

	public void compressVideo(String videoInput){
		final String videoOutput = videoInput.replace(".mp4", "compressed.mp4");
		MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
			@Override
			public void onTranscodeProgress(double progress) {
			}

			@Override
			public void onTranscodeCompleted() {
				previewVideo(videoOutput);
			}

			@Override
			public void onTranscodeCanceled() {

			}

			@Override
			public void onTranscodeFailed(Exception exception) {
			}
		};
		try {
			MediaTranscoder.getInstance().transcodeVideo(videoInput, videoOutput,
					new NewExportPreset960x540Strategy(), listener);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void previewVideo(String url){
		pd.dismiss();
		Intent intent = new Intent(mContext, RecordPreviewActivity.class);
		intent.putExtra("url", url);
		intent.putExtra("stat_id", "100");
		intent.putExtra("vid_length", String.valueOf(timer.getTotalTime()));
		startActivity(intent);
	}
}
