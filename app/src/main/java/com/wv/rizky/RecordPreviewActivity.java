package com.wv.rizky;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * Created by hamdhanywijaya@gmail.com on 5/16/17.
 */

public class RecordPreviewActivity extends BaseActivity {

    private TextureVideoView mVideoView;
    private TextView imgClose, finishText;
    private ProgressBar progressBar;
    String html = "", stat_id, vidLenght, coin;
    int lastPosition = 0;

    @Override
    public void initView() {
        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();
        html = bundle.getString("url");
        stat_id = bundle.getString("stat_id");
        vidLenght = bundle.getString("vid_length");
        if(html == null || stat_id == null)
            finish();
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        imgClose = (TextView) findViewById(R.id.discard);
        imgClose.setOnClickListener(closeListener);
        finishText = (TextView) findViewById(R.id.finish);
        finishText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mVideoView = (TextureVideoView) findViewById(R.id.vid);
        mVideoView.setScaleType(TextureVideoView.ScaleType.CENTER_CROP);
        mVideoView.setDataSource(html);
        mVideoView.setLooping(true);
        mVideoView.setListener(new TextureVideoView.MediaPlayerListener() {
            @Override
            public void onVideoPrepared() {
                progressBar.setVisibility(View.GONE);
                mVideoView.seekTo(lastPosition);
            }

            @Override
            public void onVideoEnd() {

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        lastPosition = mVideoView.getCurrentPosition();
        mVideoView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.stop();
        mVideoView.release();
    }

    @Override
    public void setUICallbacks() {

    }

    @Override
    public int getLayout() {
        return R.layout.activity_record_preview;
    }

    @Override
    public void updateUI() {

    }

    private View.OnClickListener closeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

}
