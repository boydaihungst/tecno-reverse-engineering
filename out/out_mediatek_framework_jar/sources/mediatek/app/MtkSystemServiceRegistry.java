package mediatek.app;

import android.app.SystemServiceRegistry;
import android.content.Context;
import android.os.Looper;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Log;
import com.mediatek.search.SearchEngineManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
/* loaded from: classes.dex */
public final class MtkSystemServiceRegistry {
    private static final String TAG = "MtkSystemServiceRegistry";
    private static ArrayMap<String, SystemServiceRegistry.ServiceFetcher<?>> sSystemServiceFetchers;
    private static ArrayMap<Class<?>, String> sSystemServiceNames;

    private MtkSystemServiceRegistry() {
    }

    public static void registerAllService() {
        Log.i(TAG, "registerAllService start");
        Log.i(TAG, "Comment out registerService");
        registerService(SearchEngineManager.SEARCH_ENGINE_SERVICE, SearchEngineManager.class, new SystemServiceRegistry.StaticServiceFetcher<SearchEngineManager>() { // from class: mediatek.app.MtkSystemServiceRegistry.1
            /* JADX DEBUG: Method merged with bridge method */
            /* renamed from: createService */
            public SearchEngineManager m68createService() {
                return new SearchEngineManager();
            }
        });
        registerFmService();
        registerOmadmService();
    }

    public static void setMtkSystemServiceName(ArrayMap<Class<?>, String> names, ArrayMap<String, SystemServiceRegistry.ServiceFetcher<?>> fetchers) {
        Log.i(TAG, "setMtkSystemServiceName start names" + names + ",fetchers" + fetchers);
        sSystemServiceNames = names;
        sSystemServiceFetchers = fetchers;
    }

    private static <T> void registerService(String serviceName, Class<T> serviceClass, SystemServiceRegistry.ServiceFetcher<T> serviceFetcher) {
        sSystemServiceNames.put(serviceClass, serviceName);
        sSystemServiceFetchers.put(serviceName, serviceFetcher);
    }

    public static void registerFmService() {
        final Constructor constructor;
        try {
            Class<?> clazz = Class.forName("com.mediatek.fmradio.FmRadioPackageManager");
            if (clazz != null) {
                Method method = clazz.getMethod("getPackageName", null);
                Object object = method.invoke(null, new Object[0]);
                String clazzName = (String) object;
                Class<?> clazz2 = Class.forName(clazzName);
                if (clazz2 != null && (constructor = clazz2.getConstructor(Context.class, Looper.class)) != null) {
                    registerService("fm_radio_service", Optional.class, new SystemServiceRegistry.StaticServiceFetcher<Optional>() { // from class: mediatek.app.MtkSystemServiceRegistry.2
                        /* JADX DEBUG: Method merged with bridge method */
                        public Optional createService() throws ServiceManager.ServiceNotFoundException {
                            Optional optObj = Optional.empty();
                            try {
                                Object obj = constructor.newInstance(new Object[0]);
                                Optional optObj2 = Optional.of(obj);
                                return optObj2;
                            } catch (Exception e) {
                                Log.e(MtkSystemServiceRegistry.TAG, "Exception while creating FmRadioManager object");
                                return optObj;
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while getting FmRadioPackageManager class");
        }
    }

    public static void registerOmadmService() {
        final Constructor constructor;
        try {
            Class<?> clazz = Class.forName("com.mediatek.common.omadm.OmadmManager");
            if (clazz != null && (constructor = clazz.getConstructor(Context.class)) != null) {
                registerService("omadm_service", Optional.class, new SystemServiceRegistry.StaticServiceFetcher<Optional>() { // from class: mediatek.app.MtkSystemServiceRegistry.3
                    /* JADX DEBUG: Method merged with bridge method */
                    public Optional createService() throws ServiceManager.ServiceNotFoundException {
                        Optional optObj = Optional.empty();
                        try {
                            Object obj = constructor.newInstance(new Object[0]);
                            Optional optObj2 = Optional.of(obj);
                            return optObj2;
                        } catch (Exception e) {
                            Log.e(MtkSystemServiceRegistry.TAG, "Exception while creating OmadmManager object");
                            return optObj;
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while getting OmadmManager class");
        }
    }
}
