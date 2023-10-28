package android.hardware.camera2.impl;

import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.extension.CaptureBundle;
import android.hardware.camera2.extension.ICaptureProcessorImpl;
import android.hardware.camera2.extension.IProcessResultImpl;
import android.hardware.camera2.extension.Size;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
/* loaded from: classes.dex */
public class CameraExtensionJpegProcessor implements ICaptureProcessorImpl {
    private static final int JPEG_QUEUE_SIZE = 1;
    public static final String TAG = "CameraExtensionJpeg";
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final ICaptureProcessorImpl mProcessor;
    private ImageReader mYuvReader = null;
    private Size mResolution = null;
    private int mFormat = -1;
    private Surface mOutputSurface = null;
    private ImageWriter mOutputWriter = null;
    private ConcurrentLinkedQueue<JpegParameters> mJpegParameters = new ConcurrentLinkedQueue<>();

    /* JADX INFO: Access modifiers changed from: private */
    public static native int compressJpegFromYUV420pNative(int i, int i2, ByteBuffer byteBuffer, int i3, int i4, ByteBuffer byteBuffer2, int i5, int i6, ByteBuffer byteBuffer3, int i7, int i8, ByteBuffer byteBuffer4, int i9, int i10, int i11, int i12, int i13, int i14, int i15);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class JpegParameters {
        public int mQuality;
        public int mRotation;
        public HashSet<Long> mTimeStamps;

        private JpegParameters() {
            this.mTimeStamps = new HashSet<>();
            this.mRotation = 0;
            this.mQuality = 100;
        }
    }

    public CameraExtensionJpegProcessor(ICaptureProcessorImpl processor) {
        this.mProcessor = processor;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
    }

    public void close() {
        this.mHandlerThread.quitSafely();
        ImageWriter imageWriter = this.mOutputWriter;
        if (imageWriter != null) {
            imageWriter.close();
            this.mOutputWriter = null;
        }
        ImageReader imageReader = this.mYuvReader;
        if (imageReader != null) {
            imageReader.close();
            this.mYuvReader = null;
        }
    }

    private static JpegParameters getJpegParameters(List<CaptureBundle> captureBundles) {
        JpegParameters ret = new JpegParameters();
        if (!captureBundles.isEmpty()) {
            Byte jpegQuality = (Byte) captureBundles.get(0).captureResult.get(CaptureResult.JPEG_QUALITY);
            if (jpegQuality == null) {
                Log.w(TAG, "No jpeg quality set, using default: 100");
            } else {
                ret.mQuality = jpegQuality.byteValue();
            }
            Integer orientation = (Integer) captureBundles.get(0).captureResult.get(CaptureResult.JPEG_ORIENTATION);
            if (orientation == null) {
                Log.w(TAG, "No jpeg rotation set, using default: 0");
            } else {
                ret.mRotation = (360 - (orientation.intValue() % 360)) / 90;
            }
            for (CaptureBundle bundle : captureBundles) {
                Long timeStamp = (Long) bundle.captureResult.get(CaptureResult.SENSOR_TIMESTAMP);
                if (timeStamp == null) {
                    Log.e(TAG, "Capture bundle without valid sensor timestamp!");
                } else {
                    ret.mTimeStamps.add(timeStamp);
                }
            }
        }
        return ret;
    }

    @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
    public void process(List<CaptureBundle> captureBundle, IProcessResultImpl captureCallback) throws RemoteException {
        JpegParameters jpegParams = getJpegParameters(captureBundle);
        try {
            this.mJpegParameters.add(jpegParams);
            this.mProcessor.process(captureBundle, captureCallback);
        } catch (Exception e) {
            this.mJpegParameters.remove(jpegParams);
            throw e;
        }
    }

    @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
    public void onOutputSurface(Surface surface, int format) throws RemoteException {
        if (format != 256) {
            Log.e(TAG, "Unsupported output format: " + format);
            return;
        }
        this.mOutputSurface = surface;
        initializePipeline();
    }

    @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
    public void onResolutionUpdate(Size size) throws RemoteException {
        this.mResolution = size;
        initializePipeline();
    }

    @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
    public void onImageFormatUpdate(int format) throws RemoteException {
        if (format != 35) {
            Log.e(TAG, "Unsupported input format: " + format);
            return;
        }
        this.mFormat = format;
        initializePipeline();
    }

    private void initializePipeline() throws RemoteException {
        Surface surface;
        Size size;
        if (this.mFormat != -1 && (surface = this.mOutputSurface) != null && (size = this.mResolution) != null && this.mYuvReader == null) {
            this.mOutputWriter = ImageWriter.newInstance(surface, 1, 256, size.width * this.mResolution.height, 1);
            ImageReader newInstance = ImageReader.newInstance(this.mResolution.width, this.mResolution.height, this.mFormat, 1);
            this.mYuvReader = newInstance;
            newInstance.setOnImageAvailableListener(new YuvCallback(), this.mHandler);
            this.mProcessor.onOutputSurface(this.mYuvReader.getSurface(), this.mFormat);
            this.mProcessor.onResolutionUpdate(this.mResolution);
            this.mProcessor.onImageFormatUpdate(this.mFormat);
        }
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        throw new UnsupportedOperationException("Binder IPC not supported!");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class YuvCallback implements ImageReader.OnImageAvailableListener {
        private YuvCallback() {
        }

        @Override // android.media.ImageReader.OnImageAvailableListener
        public void onImageAvailable(ImageReader reader) {
            JpegParameters jpegParams;
            Image yuvImage = null;
            Image jpegImage = null;
            try {
                yuvImage = CameraExtensionJpegProcessor.this.mYuvReader.acquireNextImage();
                jpegImage = CameraExtensionJpegProcessor.this.mOutputWriter.dequeueInputImage();
                ByteBuffer jpegBuffer = jpegImage.getPlanes()[0].getBuffer();
                jpegBuffer.clear();
                int jpegCapacity = jpegImage.getWidth();
                Image.Plane lumaPlane = yuvImage.getPlanes()[0];
                Image.Plane crPlane = yuvImage.getPlanes()[1];
                Image.Plane cbPlane = yuvImage.getPlanes()[2];
                Iterator<JpegParameters> jpegIter = CameraExtensionJpegProcessor.this.mJpegParameters.iterator();
                JpegParameters jpegParams2 = null;
                while (true) {
                    if (!jpegIter.hasNext()) {
                        break;
                    }
                    JpegParameters currentParams = jpegIter.next();
                    if (currentParams.mTimeStamps.contains(Long.valueOf(yuvImage.getTimestamp()))) {
                        jpegParams2 = currentParams;
                        jpegIter.remove();
                        break;
                    }
                }
                if (jpegParams2 != null) {
                    jpegParams = jpegParams2;
                } else if (CameraExtensionJpegProcessor.this.mJpegParameters.isEmpty()) {
                    Log.w(CameraExtensionJpegProcessor.TAG, "Empty jpeg settings queue! Using default jpeg orientation and quality!");
                    JpegParameters jpegParams3 = new JpegParameters();
                    jpegParams3.mRotation = 0;
                    jpegParams3.mQuality = 100;
                    jpegParams = jpegParams3;
                } else {
                    Log.w(CameraExtensionJpegProcessor.TAG, "No jpeg settings found with matching timestamp for current processed input!");
                    Log.w(CameraExtensionJpegProcessor.TAG, "Using values from the top of the queue!");
                    JpegParameters jpegParams4 = (JpegParameters) CameraExtensionJpegProcessor.this.mJpegParameters.poll();
                    jpegParams = jpegParams4;
                }
                CameraExtensionJpegProcessor.compressJpegFromYUV420pNative(yuvImage.getWidth(), yuvImage.getHeight(), lumaPlane.getBuffer(), lumaPlane.getPixelStride(), lumaPlane.getRowStride(), crPlane.getBuffer(), crPlane.getPixelStride(), crPlane.getRowStride(), cbPlane.getBuffer(), cbPlane.getPixelStride(), cbPlane.getRowStride(), jpegBuffer, jpegCapacity, jpegParams.mQuality, 0, 0, yuvImage.getWidth(), yuvImage.getHeight(), jpegParams.mRotation);
                jpegImage.setTimestamp(yuvImage.getTimestamp());
                yuvImage.close();
                try {
                    try {
                        CameraExtensionJpegProcessor.this.mOutputWriter.queueInputImage(jpegImage);
                    } catch (IllegalStateException e) {
                        Log.e(CameraExtensionJpegProcessor.TAG, "Failed to queue encoded result!");
                    }
                } finally {
                    jpegImage.close();
                }
            } catch (IllegalStateException e2) {
                if (yuvImage != null) {
                    yuvImage.close();
                }
                if (jpegImage != null) {
                }
                Log.e(CameraExtensionJpegProcessor.TAG, "Failed to acquire processed yuv image or jpeg image!");
            }
        }
    }
}
