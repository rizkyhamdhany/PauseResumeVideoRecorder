package com.wv.videoprocessing;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import net.ypresto.androidtranscoder.format.MediaFormatStrategy;
import net.ypresto.androidtranscoder.format.OutputFormatUnavailableException;

/**
 * Created by hamdhanywijaya@gmail.com on 5/22/17.
 */

public class NewExportPreset960x540Strategy implements MediaFormatStrategy {
    private static final int LONGER_LENGTH_960x540 = 960;
    private static final String TAG = "ExportPreset960x540Strategy";

    @Override
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        // TODO: detect non-baseline profile and throw exception
        int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        MediaFormat outputFormat = getExportPreset960x540(width, height);
        if (outputFormat == null) {
            outputFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1500000);
            outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 40);
            outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            return outputFormat;
        }
        int outWidth = outputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int outHeight = outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        Log.d(TAG, String.format("inputFormat: %dx%d => outputFormat: %dx%d", width, height, outWidth, outHeight));
        return outputFormat;
    }

    @Override
    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        // TODO
        return null;
    }

    public MediaFormat getExportPreset960x540(int originalWidth, int originalHeight) {
        int longerLength = Math.max(originalWidth, originalHeight);
        int shorterLength = Math.min(originalWidth, originalHeight);

        if (longerLength <= LONGER_LENGTH_960x540) return null; // don't upscale

        int residue = LONGER_LENGTH_960x540 * shorterLength % longerLength;
        if (residue != 0) {
            double ambiguousShorter = (double) LONGER_LENGTH_960x540 * shorterLength / longerLength;
            throw new OutputFormatUnavailableException(String.format(
                    "Could not fit to integer, original: (%d, %d), scaled: (%d, %f)",
                    longerLength, shorterLength, LONGER_LENGTH_960x540, ambiguousShorter));
        }

        int scaledShorter = LONGER_LENGTH_960x540 * shorterLength / longerLength;
        int width = originalWidth, height = originalHeight;
        if (originalWidth >= originalHeight) {
            width = LONGER_LENGTH_960x540;
            height = scaledShorter;
        } else {
            width = scaledShorter;
            height = LONGER_LENGTH_960x540;
        }

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1500000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 40);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        return format;
    }
}
