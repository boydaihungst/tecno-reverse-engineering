package com.android.server.audio;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.PrintWriterPrinter;
import com.android.internal.util.XmlUtils;
import com.android.server.audio.AudioEventLogger;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class SoundEffectsHelper {
    private static final String ASSET_FILE_VERSION = "1.0";
    private static final String ATTR_ASSET_FILE = "file";
    private static final String ATTR_ASSET_ID = "id";
    private static final String ATTR_GROUP_NAME = "name";
    private static final String ATTR_VERSION = "version";
    private static final int EFFECT_NOT_IN_SOUND_POOL = 0;
    private static final String GROUP_TOUCH_SOUNDS = "touch_sounds";
    private static final int MSG_LOAD_EFFECTS = 0;
    private static final int MSG_LOAD_EFFECTS_TIMEOUT = 3;
    private static final int MSG_PLAY_EFFECT = 2;
    private static final int MSG_UNLOAD_EFFECTS = 1;
    private static final int NUM_SOUNDPOOL_CHANNELS = 4;
    private static final int SOUND_EFFECTS_LOAD_TIMEOUT_MS = 15000;
    private static final String SOUND_EFFECTS_PATH = "/media/audio/ui/";
    private static final String TAG = "AS.SfxHelper";
    private static final String TAG_ASSET = "asset";
    private static final String TAG_AUDIO_ASSETS = "audio_assets";
    private static final String TAG_GROUP = "group";
    private final Context mContext;
    private final int mSfxAttenuationDb;
    private SfxHandler mSfxHandler;
    private SfxWorker mSfxWorker;
    private SoundPool mSoundPool;
    private SoundPoolLoader mSoundPoolLoader;
    private final AudioEventLogger mSfxLogger = new AudioEventLogger(26, "Sound Effects Loading");
    private final List<Resource> mResources = new ArrayList();
    private final int[] mEffects = new int[16];

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnEffectsLoadCompleteHandler {
        void run(boolean z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class Resource {
        final String mFileName;
        boolean mLoaded;
        int mSampleId = 0;

        Resource(String fileName) {
            this.mFileName = fileName;
        }

        void unload() {
            this.mSampleId = 0;
            this.mLoaded = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SoundEffectsHelper(Context context) {
        this.mContext = context;
        this.mSfxAttenuationDb = context.getResources().getInteger(17694952);
        startWorker();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadSoundEffects(OnEffectsLoadCompleteHandler onComplete) {
        sendMsg(0, 0, 0, onComplete, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unloadSoundEffects() {
        sendMsg(1, 0, 0, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void playSoundEffect(int effect, int volume) {
        sendMsg(2, effect, volume, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        if (this.mSfxHandler != null) {
            pw.println(prefix + "Message handler (watch for unhandled messages):");
            this.mSfxHandler.dump(new PrintWriterPrinter(pw), "  ");
        } else {
            pw.println(prefix + "Message handler is null");
        }
        pw.println(prefix + "Default attenuation (dB): " + this.mSfxAttenuationDb);
        this.mSfxLogger.dump(pw);
    }

    private void startWorker() {
        SfxWorker sfxWorker = new SfxWorker();
        this.mSfxWorker = sfxWorker;
        sfxWorker.start();
        synchronized (this) {
            while (this.mSfxHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.w(TAG, "Interrupted while waiting " + this.mSfxWorker.getName() + " to start");
                }
            }
        }
    }

    private void sendMsg(int msg, int arg1, int arg2, Object obj, int delayMs) {
        SfxHandler sfxHandler = this.mSfxHandler;
        sfxHandler.sendMessageDelayed(sfxHandler.obtainMessage(msg, arg1, arg2, obj), delayMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logEvent(String msg) {
        this.mSfxLogger.log(new AudioEventLogger.StringEvent(msg));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLoadSoundEffects(OnEffectsLoadCompleteHandler onComplete) {
        SoundPoolLoader soundPoolLoader = this.mSoundPoolLoader;
        if (soundPoolLoader != null) {
            soundPoolLoader.addHandler(onComplete);
        } else if (this.mSoundPool != null) {
            if (onComplete != null) {
                onComplete.run(true);
            }
        } else {
            logEvent("effects loading started");
            this.mSoundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setContentType(4).build()).build();
            loadSoundAssets();
            SoundPoolLoader soundPoolLoader2 = new SoundPoolLoader();
            this.mSoundPoolLoader = soundPoolLoader2;
            soundPoolLoader2.addHandler(new OnEffectsLoadCompleteHandler() { // from class: com.android.server.audio.SoundEffectsHelper.1
                @Override // com.android.server.audio.SoundEffectsHelper.OnEffectsLoadCompleteHandler
                public void run(boolean success) {
                    SoundEffectsHelper.this.mSoundPoolLoader = null;
                    if (!success) {
                        Log.w(SoundEffectsHelper.TAG, "onLoadSoundEffects(), Error while loading samples");
                        SoundEffectsHelper.this.onUnloadSoundEffects();
                    }
                }
            });
            this.mSoundPoolLoader.addHandler(onComplete);
            int resourcesToLoad = 0;
            for (Resource res : this.mResources) {
                String filePath = getResourceFilePath(res);
                int sampleId = this.mSoundPool.load(filePath, 0);
                if (sampleId > 0) {
                    res.mSampleId = sampleId;
                    res.mLoaded = false;
                    resourcesToLoad++;
                } else {
                    logEvent("effect " + filePath + " rejected by SoundPool");
                    Log.w(TAG, "SoundPool could not load file: " + filePath);
                }
            }
            if (resourcesToLoad > 0) {
                sendMsg(3, 0, 0, null, 15000);
                return;
            }
            logEvent("effects loading completed, no effects to load");
            this.mSoundPoolLoader.onComplete(true);
        }
    }

    void onUnloadSoundEffects() {
        if (this.mSoundPool == null) {
            return;
        }
        SoundPoolLoader soundPoolLoader = this.mSoundPoolLoader;
        if (soundPoolLoader != null) {
            soundPoolLoader.addHandler(new OnEffectsLoadCompleteHandler() { // from class: com.android.server.audio.SoundEffectsHelper.2
                @Override // com.android.server.audio.SoundEffectsHelper.OnEffectsLoadCompleteHandler
                public void run(boolean success) {
                    SoundEffectsHelper.this.onUnloadSoundEffects();
                }
            });
        }
        logEvent("effects unloading started");
        for (Resource res : this.mResources) {
            if (res.mSampleId != 0) {
                this.mSoundPool.unload(res.mSampleId);
                res.unload();
            }
        }
        this.mSoundPool.release();
        this.mSoundPool = null;
        logEvent("effects unloading completed");
    }

    void onPlaySoundEffect(int effect, int volume) {
        float volFloat;
        if (volume < 0) {
            volFloat = (float) Math.pow(10.0d, this.mSfxAttenuationDb / 20.0f);
        } else {
            float volFloat2 = volume;
            volFloat = volFloat2 / 1000.0f;
        }
        Resource res = this.mResources.get(this.mEffects[effect]);
        if (this.mSoundPool != null && res.mSampleId != 0 && res.mLoaded) {
            this.mSoundPool.play(res.mSampleId, volFloat, volFloat, 0, 0, 1.0f);
            return;
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            String filePath = getResourceFilePath(res);
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.setAudioStreamType(1);
            mediaPlayer.prepare();
            mediaPlayer.setVolume(volFloat);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { // from class: com.android.server.audio.SoundEffectsHelper.3
                @Override // android.media.MediaPlayer.OnCompletionListener
                public void onCompletion(MediaPlayer mp) {
                    SoundEffectsHelper.cleanupPlayer(mp);
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() { // from class: com.android.server.audio.SoundEffectsHelper.4
                @Override // android.media.MediaPlayer.OnErrorListener
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    SoundEffectsHelper.cleanupPlayer(mp);
                    return true;
                }
            });
            mediaPlayer.start();
        } catch (IOException ex) {
            Log.w(TAG, "MediaPlayer IOException: " + ex);
        } catch (IllegalArgumentException ex2) {
            Log.w(TAG, "MediaPlayer IllegalArgumentException: " + ex2);
        } catch (IllegalStateException ex3) {
            Log.w(TAG, "MediaPlayer IllegalStateException: " + ex3);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void cleanupPlayer(MediaPlayer mp) {
        if (mp != null) {
            try {
                mp.stop();
                mp.release();
            } catch (IllegalStateException ex) {
                Log.w(TAG, "MediaPlayer IllegalStateException: " + ex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getResourceFilePath(Resource res) {
        String filePath = Environment.getProductDirectory() + SOUND_EFFECTS_PATH + res.mFileName;
        if (!new File(filePath).isFile()) {
            return Environment.getRootDirectory() + SOUND_EFFECTS_PATH + res.mFileName;
        }
        return filePath;
    }

    private void loadSoundAssetDefaults() {
        int defaultResourceIdx = this.mResources.size();
        this.mResources.add(new Resource("Effect_Tick.ogg"));
        Arrays.fill(this.mEffects, defaultResourceIdx);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [412=5] */
    private void loadSoundAssets() {
        XmlResourceParser parser = null;
        if (this.mResources.isEmpty()) {
            loadSoundAssetDefaults();
            try {
                try {
                    try {
                        try {
                            parser = this.mContext.getResources().getXml(18284545);
                            XmlUtils.beginDocument(parser, TAG_AUDIO_ASSETS);
                            String version = parser.getAttributeValue(null, ATTR_VERSION);
                            Map<Integer, Integer> parserCounter = new HashMap<>();
                            if ("1.0".equals(version)) {
                                while (true) {
                                    XmlUtils.nextElement(parser);
                                    String element = parser.getName();
                                    if (element == null) {
                                        break;
                                    } else if (!element.equals(TAG_GROUP)) {
                                        if (!element.equals(TAG_ASSET)) {
                                            break;
                                        }
                                        String id = parser.getAttributeValue(null, ATTR_ASSET_ID);
                                        String file = parser.getAttributeValue(null, ATTR_ASSET_FILE);
                                        try {
                                            Field field = AudioManager.class.getField(id);
                                            int fx = field.getInt(null);
                                            int currentParserCount = parserCounter.getOrDefault(Integer.valueOf(fx), 0).intValue() + 1;
                                            parserCounter.put(Integer.valueOf(fx), Integer.valueOf(currentParserCount));
                                            if (currentParserCount > 1) {
                                                Log.w(TAG, "Duplicate definition for sound ID: " + id);
                                            }
                                            this.mEffects[fx] = findOrAddResourceByFileName(file);
                                        } catch (Exception e) {
                                            Log.w(TAG, "Invalid sound ID: " + id);
                                        }
                                    } else {
                                        String name = parser.getAttributeValue(null, "name");
                                        if (!GROUP_TOUCH_SOUNDS.equals(name)) {
                                            Log.w(TAG, "Unsupported group name: " + name);
                                        }
                                    }
                                }
                                boolean navigationRepeatFxParsed = allNavigationRepeatSoundsParsed(parserCounter);
                                boolean homeSoundParsed = parserCounter.getOrDefault(11, 0).intValue() > 0;
                                if (navigationRepeatFxParsed || homeSoundParsed) {
                                    AudioManager audioManager = (AudioManager) this.mContext.getSystemService(AudioManager.class);
                                    if (audioManager != null && navigationRepeatFxParsed) {
                                        audioManager.setNavigationRepeatSoundEffectsEnabled(true);
                                    }
                                    if (audioManager != null && homeSoundParsed) {
                                        audioManager.setHomeSoundEffectEnabled(true);
                                    }
                                }
                            }
                            if (parser == null) {
                                return;
                            }
                        } catch (Throwable th) {
                            if (parser != null) {
                                parser.close();
                            }
                            throw th;
                        }
                    } catch (IOException e2) {
                        Log.w(TAG, "I/O exception reading sound assets", e2);
                        if (parser == null) {
                            return;
                        }
                    }
                } catch (Resources.NotFoundException e3) {
                    Log.w(TAG, "audio assets file not found", e3);
                    if (parser == null) {
                        return;
                    }
                }
            } catch (XmlPullParserException e4) {
                Log.w(TAG, "XML parser exception reading sound assets", e4);
                if (parser == null) {
                    return;
                }
            }
            parser.close();
        }
    }

    private boolean allNavigationRepeatSoundsParsed(Map<Integer, Integer> parserCounter) {
        int numFastScrollSoundEffectsParsed = parserCounter.getOrDefault(12, 0).intValue() + parserCounter.getOrDefault(13, 0).intValue() + parserCounter.getOrDefault(14, 0).intValue() + parserCounter.getOrDefault(15, 0).intValue();
        return numFastScrollSoundEffectsParsed == 4;
    }

    private int findOrAddResourceByFileName(String fileName) {
        for (int i = 0; i < this.mResources.size(); i++) {
            if (this.mResources.get(i).mFileName.equals(fileName)) {
                return i;
            }
        }
        int result = this.mResources.size();
        this.mResources.add(new Resource(fileName));
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Resource findResourceBySampleId(int sampleId) {
        for (Resource res : this.mResources) {
            if (res.mSampleId == sampleId) {
                return res;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SfxWorker extends Thread {
        SfxWorker() {
            super("AS.SfxWorker");
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            Looper.prepare();
            synchronized (SoundEffectsHelper.this) {
                SoundEffectsHelper.this.mSfxHandler = new SfxHandler();
                SoundEffectsHelper.this.notify();
            }
            Looper.loop();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SfxHandler extends Handler {
        private SfxHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    SoundEffectsHelper.this.onLoadSoundEffects((OnEffectsLoadCompleteHandler) msg.obj);
                    return;
                case 1:
                    SoundEffectsHelper.this.onUnloadSoundEffects();
                    return;
                case 2:
                    final int effect = msg.arg1;
                    final int volume = msg.arg2;
                    SoundEffectsHelper.this.onLoadSoundEffects(new OnEffectsLoadCompleteHandler() { // from class: com.android.server.audio.SoundEffectsHelper.SfxHandler.1
                        @Override // com.android.server.audio.SoundEffectsHelper.OnEffectsLoadCompleteHandler
                        public void run(boolean success) {
                            if (success) {
                                SoundEffectsHelper.this.onPlaySoundEffect(effect, volume);
                            }
                        }
                    });
                    return;
                case 3:
                    if (SoundEffectsHelper.this.mSoundPoolLoader != null) {
                        SoundEffectsHelper.this.mSoundPoolLoader.onTimeout();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SoundPoolLoader implements SoundPool.OnLoadCompleteListener {
        private List<OnEffectsLoadCompleteHandler> mLoadCompleteHandlers = new ArrayList();

        SoundPoolLoader() {
            SoundEffectsHelper.this.mSoundPool.setOnLoadCompleteListener(this);
        }

        void addHandler(OnEffectsLoadCompleteHandler handler) {
            if (handler != null) {
                this.mLoadCompleteHandlers.add(handler);
            }
        }

        @Override // android.media.SoundPool.OnLoadCompleteListener
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            String filePath;
            if (status == 0) {
                int remainingToLoad = 0;
                for (Resource res : SoundEffectsHelper.this.mResources) {
                    if (res.mSampleId == sampleId && !res.mLoaded) {
                        SoundEffectsHelper.this.logEvent("effect " + res.mFileName + " loaded");
                        res.mLoaded = true;
                    }
                    if (res.mSampleId != 0 && !res.mLoaded) {
                        remainingToLoad++;
                    }
                }
                if (remainingToLoad == 0) {
                    onComplete(true);
                    return;
                }
                return;
            }
            Resource res2 = SoundEffectsHelper.this.findResourceBySampleId(sampleId);
            if (res2 != null) {
                filePath = SoundEffectsHelper.this.getResourceFilePath(res2);
            } else {
                filePath = "with unknown sample ID " + sampleId;
            }
            SoundEffectsHelper.this.logEvent("effect " + filePath + " loading failed, status " + status);
            Log.w(SoundEffectsHelper.TAG, "onLoadSoundEffects(), Error " + status + " while loading sample " + filePath);
            onComplete(false);
        }

        void onTimeout() {
            onComplete(false);
        }

        void onComplete(boolean success) {
            if (SoundEffectsHelper.this.mSoundPool != null) {
                SoundEffectsHelper.this.mSoundPool.setOnLoadCompleteListener(null);
            }
            for (OnEffectsLoadCompleteHandler handler : this.mLoadCompleteHandlers) {
                handler.run(success);
            }
            SoundEffectsHelper.this.logEvent("effects loading " + (success ? "completed" : "failed"));
        }
    }
}
