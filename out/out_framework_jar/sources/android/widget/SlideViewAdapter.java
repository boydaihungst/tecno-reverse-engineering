package android.widget;

import android.app.IServiceConnection;
import android.app.job.JobInfo;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.internal.widget.IRemoteViewsFactory;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.Executor;
/* loaded from: classes4.dex */
public class SlideViewAdapter extends BaseAdapter implements Handler.Callback {
    private static final int DEFAULT_CACHE_SIZE = 40;
    static final String EXTRA_REMOTEADAPTER_APPWIDGET_ID = "remoteAdapterAppWidgetId";
    static final String EXTRA_REMOTEADAPTER_ON_LIGHT_BACKGROUND = "remoteAdapterOnLightBackground";
    public static final String EXTRA_SHARED_ELEMENT_BOUNDS = "android.widget.extra.SHARED_ELEMENT_BOUNDS";
    private static final int MSG_MAIN_HANDLER_COMMIT_METADATA = 1;
    private static final int MSG_MAIN_HANDLER_REMOTE_ADAPTER_CONNECTED = 3;
    private static final int MSG_MAIN_HANDLER_REMOTE_ADAPTER_DISCONNECTED = 4;
    private static final int MSG_MAIN_HANDLER_SUPER_NOTIFY_DATA_SET_CHANGED = 2;
    static final int MSG_NOTIFY_DATA_SET_CHANGED = 2;
    static final int MSG_PRE_LOAD_NEXT_ITEM = 3;
    static final int MSG_REQUEST_BIND = 1;
    static final int MSG_UNBIND_SERVICE = 4;
    static final String TAG = "SlideViewAdapter";
    private static final int UNBIND_SERVICE_DELAY = 30000;
    private final int mAppWidgetId;
    private final Executor mAsyncViewLoadExecutor;
    private final FixedSizeRemoteViewsCache mCache;
    private final RemoteAdapterConnectionCallback mCallback;
    private final Context mContext;
    private boolean mDataReady = false;
    private final Intent mIntent;
    private final Handler mMainHandler;
    private final boolean mOnLightBackground;
    private RemoteViews.InteractionHandler mRemoteViewsInteractionHandler;
    private final RemoteServiceHandler mServiceHandler;
    private int mVisibleWindowIndex;
    private final HandlerThread mWorkerThread;

    /* loaded from: classes4.dex */
    public interface RemoteAdapterConnectionCallback {
        void onDataChanged();

        boolean onRemoteAdapterConnected();

        void onRemoteAdapterDisconnected();

        void setRemoteViewsAdapter(Intent intent, boolean z);
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                this.mCache.commitTemporaryMetaData();
                return true;
            case 2:
                RemoteAdapterConnectionCallback remoteAdapterConnectionCallback = this.mCallback;
                if (remoteAdapterConnectionCallback != null) {
                    remoteAdapterConnectionCallback.onDataChanged();
                }
                superNotifyDataSetChanged();
                return true;
            case 3:
                RemoteAdapterConnectionCallback remoteAdapterConnectionCallback2 = this.mCallback;
                if (remoteAdapterConnectionCallback2 != null) {
                    remoteAdapterConnectionCallback2.onRemoteAdapterConnected();
                }
                return true;
            case 4:
                RemoteAdapterConnectionCallback remoteAdapterConnectionCallback3 = this.mCallback;
                if (remoteAdapterConnectionCallback3 != null) {
                    remoteAdapterConnectionCallback3.onRemoteAdapterDisconnected();
                }
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class RemoteServiceHandler extends Handler implements ServiceConnection {
        private final WeakReference<SlideViewAdapter> mAdapter;
        private boolean mBindRequested;
        private final Context mContext;
        private boolean mNotifyDataSetChangedPending;
        private IRemoteViewsFactory mRemoteViewsFactory;

        RemoteServiceHandler(Looper workerLooper, SlideViewAdapter adapter, Context context) {
            super(workerLooper);
            this.mNotifyDataSetChangedPending = false;
            this.mBindRequested = false;
            this.mAdapter = new WeakReference<>(adapter);
            this.mContext = context;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.mRemoteViewsFactory = IRemoteViewsFactory.Stub.asInterface(service);
            enqueueDeferredUnbindServiceMessage();
            SlideViewAdapter adapter = this.mAdapter.get();
            if (adapter == null) {
                return;
            }
            Log.d(SlideViewAdapter.TAG, "<onServiceConnected>");
            if (this.mNotifyDataSetChangedPending) {
                this.mNotifyDataSetChangedPending = false;
                Message msg = Message.obtain(this, 2);
                handleMessage(msg);
                msg.recycle();
            } else if (!sendNotifyDataSetChange(false)) {
            } else {
                adapter.updateTemporaryMetaData(this.mRemoteViewsFactory);
                adapter.mMainHandler.sendEmptyMessage(1);
                adapter.mMainHandler.sendEmptyMessage(3);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Log.d(SlideViewAdapter.TAG, "<onServiceDisconnected>");
            this.mRemoteViewsFactory = null;
            SlideViewAdapter adapter = this.mAdapter.get();
            if (adapter != null) {
                adapter.mMainHandler.sendEmptyMessage(4);
            }
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName name) {
            Log.d(SlideViewAdapter.TAG, "<onBindingDied> try to rebind service");
            unbindNow();
            SlideViewAdapter adapter = this.mAdapter.get();
            if (adapter != null) {
                adapter.requestBindService();
            }
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            SlideViewAdapter adapter = this.mAdapter.get();
            switch (msg.what) {
                case 1:
                    if (adapter == null || this.mRemoteViewsFactory != null) {
                        enqueueDeferredUnbindServiceMessage();
                    }
                    if (this.mBindRequested) {
                        return;
                    }
                    IServiceConnection sd = this.mContext.getServiceDispatcher(this, this, InputDevice.SOURCE_HDMI);
                    Intent intent = (Intent) msg.obj;
                    int appWidgetId = msg.arg1;
                    try {
                        this.mBindRequested = AppWidgetManager.getInstance(this.mContext).bindRemoteViewsService(this.mContext, appWidgetId, intent, sd, InputDevice.SOURCE_HDMI);
                        return;
                    } catch (Exception e) {
                        Log.e(SlideViewAdapter.TAG, "Failed to bind remoteViewsService: " + e.getMessage());
                        return;
                    }
                case 2:
                    enqueueDeferredUnbindServiceMessage();
                    if (adapter == null) {
                        Log.e(SlideViewAdapter.TAG, "MSG_NOTIFY_DATA_SET_CHANGED adapter == null");
                        return;
                    } else if (this.mRemoteViewsFactory == null) {
                        Log.e(SlideViewAdapter.TAG, "MSG_NOTIFY_DATA_SET_CHANGED mRemoteViewsFactory == null");
                        this.mNotifyDataSetChangedPending = true;
                        adapter.requestBindService();
                        return;
                    } else if (!sendNotifyDataSetChange(true)) {
                        return;
                    } else {
                        synchronized (adapter.mCache) {
                            adapter.mCache.reset();
                        }
                        Log.d(SlideViewAdapter.TAG, "MSG_NOTIFY_DATA_SET_CHANGED");
                        adapter.updateTemporaryMetaData(this.mRemoteViewsFactory);
                        adapter.mMainHandler.sendEmptyMessage(1);
                        adapter.mMainHandler.sendEmptyMessage(2);
                        return;
                    }
                case 3:
                    if (adapter == null) {
                        Log.e(SlideViewAdapter.TAG, "MSG_PRE_LOAD_NEXT_ITEM adapter == null");
                        return;
                    } else if (this.mRemoteViewsFactory == null) {
                        Log.e(SlideViewAdapter.TAG, "MSG_PRE_LOAD_NEXT_ITEM mRemoteViewsFactory == null");
                        return;
                    } else {
                        removeMessages(4);
                        int nextPosition = ((Integer) msg.obj).intValue();
                        int position = getNextPosition(nextPosition);
                        Log.d(SlideViewAdapter.TAG, "MSG_PRE_LOAD_NEXT_ITEM nextPosition: " + position);
                        if (position > -1) {
                            adapter.updateRemoteViews(this.mRemoteViewsFactory, position, true);
                            return;
                        } else {
                            enqueueDeferredUnbindServiceMessage();
                            return;
                        }
                    }
                case 4:
                    Log.d(SlideViewAdapter.TAG, "MSG_UNBIND_SERVICE unbindNow mBindRequested: " + this.mBindRequested);
                    unbindNow();
                    return;
                default:
                    return;
            }
        }

        protected void unbindNow() {
            if (this.mBindRequested) {
                this.mBindRequested = false;
                this.mContext.unbindService(this);
            }
            this.mRemoteViewsFactory = null;
        }

        private int getNextPosition(int nextPosition) {
            int count;
            SlideViewAdapter adapter = this.mAdapter.get();
            if (adapter == null || nextPosition < 0 || (count = adapter.mCache.mTemporaryMetaData.count) <= 0) {
                return -1;
            }
            if (nextPosition >= count) {
                return nextPosition % count;
            }
            return nextPosition;
        }

        private boolean sendNotifyDataSetChange(boolean always) {
            if (!always) {
                try {
                    if (this.mRemoteViewsFactory.isCreated()) {
                        return true;
                    }
                } catch (RemoteException | RuntimeException e) {
                    Log.e(SlideViewAdapter.TAG, "Error in updateNotifyDataSetChanged(): " + e.getMessage());
                    return false;
                }
            }
            this.mRemoteViewsFactory.onDataSetChanged();
            return true;
        }

        private void enqueueDeferredUnbindServiceMessage() {
            removeMessages(4);
            sendEmptyMessageDelayed(4, JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
        }
    }

    public SlideViewAdapter(Context context, Intent intent, boolean useAsyncLoader, RemoteAdapterConnectionCallback callback) {
        this.mContext = context;
        this.mIntent = intent;
        this.mAppWidgetId = intent.getIntExtra(EXTRA_REMOTEADAPTER_APPWIDGET_ID, -1);
        this.mOnLightBackground = intent.getBooleanExtra(EXTRA_REMOTEADAPTER_ON_LIGHT_BACKGROUND, false);
        intent.removeExtra(EXTRA_REMOTEADAPTER_APPWIDGET_ID);
        intent.removeExtra(EXTRA_REMOTEADAPTER_ON_LIGHT_BACKGROUND);
        HandlerThread handlerThread = new HandlerThread("RemoteViewsCache-loader");
        this.mWorkerThread = handlerThread;
        handlerThread.start();
        this.mMainHandler = new Handler(Looper.myLooper(), this);
        this.mServiceHandler = new RemoteServiceHandler(handlerThread.getLooper(), this, context.getApplicationContext());
        this.mAsyncViewLoadExecutor = useAsyncLoader ? new HandlerThreadExecutor(handlerThread) : null;
        this.mCallback = callback;
        this.mCache = new FixedSizeRemoteViewsCache(40);
        if (!this.mDataReady) {
            requestBindService();
        }
    }

    protected void finalize() throws Throwable {
        try {
            this.mServiceHandler.unbindNow();
            this.mWorkerThread.quit();
        } finally {
            super.finalize();
        }
    }

    @Override // android.widget.Adapter
    public int getCount() {
        int i;
        RemoteViewsMetaData metaData = this.mCache.getMetaData();
        synchronized (metaData) {
            i = metaData.count;
        }
        return i;
    }

    @Override // android.widget.Adapter
    public Object getItem(int position) {
        return null;
    }

    @Override // android.widget.Adapter
    public long getItemId(int position) {
        synchronized (this.mCache) {
            if (this.mCache.containsMetaDataAt(position)) {
                return this.mCache.getMetaDataAt(position).itemId;
            }
            return 0L;
        }
    }

    public void setVisibleHint(int showingIndex) {
        this.mVisibleWindowIndex = showingIndex;
    }

    @Override // android.widget.Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        RemoteViews remoteViews = this.mCache.getRemoteViewsAt(position);
        if (remoteViews != null) {
            RemoteViewsFrameLayout layout = new RemoteViewsFrameLayout(parent.getContext(), this.mCache);
            layout.setExecutor(this.mAsyncViewLoadExecutor);
            layout.setOnLightBackground(this.mOnLightBackground);
            layout.onRemoteViewsLoaded(remoteViews, this.mRemoteViewsInteractionHandler, false);
            int nextPosition = getNextPosition(position + 1);
            Log.d(TAG, "<getView> current position: " + position + ", preload next: " + nextPosition);
            if (nextPosition != position) {
                Message.obtain(this.mServiceHandler, 3, Integer.valueOf(nextPosition)).sendToTarget();
            }
            return layout;
        }
        this.mCache.queueRequestedPositionToLoad(position);
        Message.obtain(this.mServiceHandler, 3, Integer.valueOf(position)).sendToTarget();
        Log.e(TAG, "<getView> no cache of next remoteView position: " + position);
        return null;
    }

    private int getNextPosition(int nextPosition) {
        int count;
        if (nextPosition < 0 || (count = this.mCache.mTemporaryMetaData.count) <= 0) {
            return -1;
        }
        if (nextPosition >= count) {
            return nextPosition % count;
        }
        return nextPosition;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Intent createRemoteViewsServiceIntent() {
        return this.mIntent;
    }

    public boolean isDataReady() {
        return this.mDataReady;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestBindService() {
        this.mServiceHandler.removeMessages(4);
        Message.obtain(this.mServiceHandler, 1, this.mAppWidgetId, 0, this.mIntent).sendToTarget();
    }

    private int[] getVisibleWindow() {
        int i = this.mVisibleWindowIndex;
        if (i < 0) {
            return new int[0];
        }
        int[] window = {i};
        return window;
    }

    @Override // android.widget.BaseAdapter
    public void notifyDataSetChanged() {
        this.mServiceHandler.removeMessages(4);
        this.mServiceHandler.sendEmptyMessage(2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void superNotifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTemporaryMetaData(IRemoteViewsFactory factory) {
        try {
            Log.i(TAG, "updateTemporaryMetaData");
            boolean hasStableIds = factory.hasStableIds();
            int viewTypeCount = factory.getViewTypeCount();
            int count = factory.getCount();
            RemoteViewsMetaData tmpMetaData = this.mCache.getTemporaryMetaData();
            synchronized (tmpMetaData) {
                tmpMetaData.hasStableIds = hasStableIds;
                tmpMetaData.viewTypeCount = viewTypeCount;
                tmpMetaData.count = count;
            }
            if (count > 0) {
                updateRemoteViews(factory, 0, false);
            }
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Error in updateMetaData: " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRemoteViews(IRemoteViewsFactory factory, int position, boolean notifyWhenLoaded) {
        try {
            Log.d(TAG, "<updateRemoteViews> load remoteView position: " + position);
            RemoteViews remoteViews = factory.getViewAt(position);
            long itemId = factory.getItemId(position);
            if (remoteViews == null) {
                Log.e(TAG, "<updateRemoteViews> no remoteView");
                return;
            }
            synchronized (this.mCache) {
                int[] visibleWindow = getVisibleWindow();
                this.mCache.insert(position, remoteViews, itemId, visibleWindow);
            }
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "<updateRemoteViews> exception: (" + position + "): " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class RemoteViewsIndexMetaData {
        long itemId;
        int typeId;

        public RemoteViewsIndexMetaData(RemoteViews v, long itemId) {
            set(v, itemId);
        }

        public void set(RemoteViews v, long id) {
            this.itemId = id;
            if (v != null) {
                this.typeId = v.getLayoutId();
            } else {
                this.typeId = 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class FixedSizeRemoteViewsCache {
        private static final int sMaxMemoryLimitInBytes = 2097152;
        private final int mMaxCount;
        private final SparseArray<RemoteViewsIndexMetaData> mIndexMetaData = new SparseArray<>();
        private final SparseArray<RemoteViews> mIndexRemoteViews = new SparseArray<>();
        private final RemoteViewsMetaData mMetaData = new RemoteViewsMetaData();
        private final RemoteViewsMetaData mTemporaryMetaData = new RemoteViewsMetaData();
        private int mLastRequestedIndex = -1;

        public FixedSizeRemoteViewsCache(int maxCacheSize) {
            this.mMaxCount = maxCacheSize;
        }

        public void insert(int position, RemoteViews v, long itemId, int[] visibleWindow) {
            int trimIndex;
            if (this.mIndexRemoteViews.size() >= this.mMaxCount) {
                this.mIndexRemoteViews.remove(getFarthestPositionFrom(position, visibleWindow));
            }
            int pruneFromPosition = this.mLastRequestedIndex;
            if (pruneFromPosition <= -1) {
                pruneFromPosition = position;
            }
            int memoryUsage = getRemoteViewsBitmapMemoryUsage();
            while (memoryUsage >= 2097152 && (trimIndex = getFarthestPositionFrom(pruneFromPosition, visibleWindow)) >= 0) {
                Log.e(SlideViewAdapter.TAG, "cache go beyond the available memory size ,now " + memoryUsage);
                this.mIndexRemoteViews.remove(trimIndex);
                memoryUsage = getRemoteViewsBitmapMemoryUsage();
            }
            RemoteViewsIndexMetaData metaData = this.mIndexMetaData.get(position);
            if (metaData != null) {
                metaData.set(v, itemId);
            } else {
                this.mIndexMetaData.put(position, new RemoteViewsIndexMetaData(v, itemId));
            }
            this.mIndexRemoteViews.put(position, v);
        }

        public void queueRequestedPositionToLoad(int position) {
            this.mLastRequestedIndex = position;
        }

        public RemoteViews getRemoteViewsAt(int position) {
            return this.mIndexRemoteViews.get(position);
        }

        public RemoteViewsMetaData getMetaData() {
            return this.mMetaData;
        }

        public RemoteViewsMetaData getTemporaryMetaData() {
            return this.mTemporaryMetaData;
        }

        public RemoteViewsIndexMetaData getMetaDataAt(int position) {
            return this.mIndexMetaData.get(position);
        }

        public void commitTemporaryMetaData() {
            synchronized (this.mTemporaryMetaData) {
                synchronized (this.mMetaData) {
                    this.mMetaData.set(this.mTemporaryMetaData);
                }
            }
        }

        private int getRemoteViewsBitmapMemoryUsage() {
            int mem = 0;
            for (int i = this.mIndexRemoteViews.size() - 1; i >= 0; i--) {
                RemoteViews v = this.mIndexRemoteViews.valueAt(i);
                if (v != null) {
                    mem += v.estimateMemoryUsage();
                }
            }
            return mem;
        }

        public boolean containsMetaDataAt(int position) {
            return this.mIndexMetaData.indexOfKey(position) >= 0;
        }

        private int getFarthestPositionFrom(int pos, int[] visibleWindow) {
            int maxDist = 0;
            int maxDistIndex = -1;
            int maxDistNotVisible = 0;
            int maxDistIndexNotVisible = -1;
            for (int i = this.mIndexRemoteViews.size() - 1; i >= 0; i--) {
                int index = this.mIndexRemoteViews.keyAt(i);
                int dist = Math.abs(index - pos);
                if (dist > maxDistNotVisible && Arrays.binarySearch(visibleWindow, index) < 0) {
                    maxDistIndexNotVisible = index;
                    maxDistNotVisible = dist;
                }
                if (dist >= maxDist) {
                    maxDistIndex = index;
                    maxDist = dist;
                }
            }
            if (maxDistIndexNotVisible > -1) {
                return maxDistIndexNotVisible;
            }
            return maxDistIndex;
        }

        public void reset() {
            this.mIndexMetaData.clear();
            this.mIndexRemoteViews.clear();
            this.mLastRequestedIndex = -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class RemoteViewsMetaData {
        int count;
        boolean hasStableIds;
        private final SparseIntArray mTypeIdIndexMap = new SparseIntArray();
        int viewTypeCount;

        public RemoteViewsMetaData() {
            reset();
        }

        public void set(RemoteViewsMetaData d) {
            synchronized (d) {
                this.count = d.count;
                this.viewTypeCount = d.viewTypeCount;
                this.hasStableIds = d.hasStableIds;
            }
        }

        public void reset() {
            this.count = 0;
            this.viewTypeCount = 1;
            this.hasStableIds = true;
            this.mTypeIdIndexMap.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class RemoteViewsFrameLayout extends AppWidgetHostView {
        public int cacheIndex;
        private final FixedSizeRemoteViewsCache mCache;

        public RemoteViewsFrameLayout(Context context, FixedSizeRemoteViewsCache cache) {
            super(context);
            this.cacheIndex = -1;
            this.mCache = cache;
        }

        public void onRemoteViewsLoaded(RemoteViews view, RemoteViews.InteractionHandler handler, boolean forceApplyAsync) {
            setInteractionHandler(handler);
            applyRemoteViews(view, forceApplyAsync);
        }

        @Override // android.appwidget.AppWidgetHostView
        protected View getDefaultView() {
            return null;
        }

        protected Context getRemoteContext() {
            return null;
        }

        @Override // android.appwidget.AppWidgetHostView
        protected View getErrorView() {
            return getDefaultView();
        }
    }

    /* loaded from: classes4.dex */
    private static class HandlerThreadExecutor implements Executor {
        private final HandlerThread mThread;

        HandlerThreadExecutor(HandlerThread thread) {
            this.mThread = thread;
        }

        @Override // java.util.concurrent.Executor
        public void execute(Runnable runnable) {
            if (Thread.currentThread().getId() == this.mThread.getId()) {
                runnable.run();
            } else {
                new Handler(this.mThread.getLooper()).post(runnable);
            }
        }
    }

    public void setRemoteViewsInteractionHandler(RemoteViews.InteractionHandler handler) {
        this.mRemoteViewsInteractionHandler = handler;
    }
}
