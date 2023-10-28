package com.android.server.pm;

import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.UserHandle;
import android.util.ArrayMap;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.component.ParsedComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class PackageProperty {
    private ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> mActivityProperties;
    private ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> mApplicationProperties;
    private ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> mProviderProperties;
    private ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> mReceiverProperties;
    private ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> mServiceProperties;

    public PackageManager.Property getProperty(String propertyName, String packageName, String className) {
        if (className == null) {
            return getApplicationProperty(propertyName, packageName);
        }
        return getComponentProperty(propertyName, packageName, className);
    }

    public List<PackageManager.Property> queryProperty(String propertyName, int componentType, Predicate<String> filter) {
        ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> propertyMap;
        ArrayMap<String, ArrayList<PackageManager.Property>> packagePropertyMap;
        if (componentType == 5) {
            propertyMap = this.mApplicationProperties;
        } else if (componentType == 1) {
            propertyMap = this.mActivityProperties;
        } else if (componentType == 4) {
            propertyMap = this.mProviderProperties;
        } else if (componentType == 2) {
            propertyMap = this.mReceiverProperties;
        } else if (componentType == 3) {
            propertyMap = this.mServiceProperties;
        } else {
            propertyMap = null;
        }
        if (propertyMap == null || (packagePropertyMap = propertyMap.get(propertyName)) == null) {
            return null;
        }
        Binder.getCallingUid();
        UserHandle.getCallingUserId();
        int mapSize = packagePropertyMap.size();
        List<PackageManager.Property> result = new ArrayList<>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            String packageName = packagePropertyMap.keyAt(i);
            if (!filter.test(packageName)) {
                result.addAll(packagePropertyMap.valueAt(i));
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addAllProperties(AndroidPackage pkg) {
        this.mApplicationProperties = addProperties(pkg.getProperties(), this.mApplicationProperties);
        this.mActivityProperties = addComponentProperties(pkg.getActivities(), this.mActivityProperties);
        this.mProviderProperties = addComponentProperties(pkg.getProviders(), this.mProviderProperties);
        this.mReceiverProperties = addComponentProperties(pkg.getReceivers(), this.mReceiverProperties);
        this.mServiceProperties = addComponentProperties(pkg.getServices(), this.mServiceProperties);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllProperties(AndroidPackage pkg) {
        this.mApplicationProperties = removeProperties(pkg.getProperties(), this.mApplicationProperties);
        this.mActivityProperties = removeComponentProperties(pkg.getActivities(), this.mActivityProperties);
        this.mProviderProperties = removeComponentProperties(pkg.getProviders(), this.mProviderProperties);
        this.mReceiverProperties = removeComponentProperties(pkg.getReceivers(), this.mReceiverProperties);
        this.mServiceProperties = removeComponentProperties(pkg.getServices(), this.mServiceProperties);
    }

    private static <T extends ParsedComponent> ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> addComponentProperties(List<T> components, ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> propertyCollection) {
        ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> returnCollection = propertyCollection;
        int componentsSize = components.size();
        for (int i = 0; i < componentsSize; i++) {
            Map<String, PackageManager.Property> properties = components.get(i).getProperties();
            if (properties.size() != 0) {
                returnCollection = addProperties(properties, returnCollection);
            }
        }
        return returnCollection;
    }

    private static ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> addProperties(Map<String, PackageManager.Property> properties, ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> propertyCollection) {
        if (properties.size() == 0) {
            return propertyCollection;
        }
        ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> returnCollection = propertyCollection == null ? new ArrayMap<>(10) : propertyCollection;
        for (PackageManager.Property property : properties.values()) {
            String propertyName = property.getName();
            String packageName = property.getPackageName();
            ArrayMap<String, ArrayList<PackageManager.Property>> propertyMap = returnCollection.get(propertyName);
            if (propertyMap == null) {
                propertyMap = new ArrayMap<>();
                returnCollection.put(propertyName, propertyMap);
            }
            ArrayList<PackageManager.Property> packageProperties = propertyMap.get(packageName);
            if (packageProperties == null) {
                packageProperties = new ArrayList<>(properties.size());
                propertyMap.put(packageName, packageProperties);
            }
            packageProperties.add(property);
        }
        return returnCollection;
    }

    private static <T extends ParsedComponent> ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> removeComponentProperties(List<T> components, ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> propertyCollection) {
        ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> returnCollection = propertyCollection;
        int componentsSize = components.size();
        for (int i = 0; returnCollection != null && i < componentsSize; i++) {
            Map<String, PackageManager.Property> properties = components.get(i).getProperties();
            if (properties.size() != 0) {
                returnCollection = removeProperties(properties, returnCollection);
            }
        }
        return returnCollection;
    }

    private static ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> removeProperties(Map<String, PackageManager.Property> properties, ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> propertyCollection) {
        ArrayList<PackageManager.Property> packageProperties;
        if (propertyCollection == null) {
            return null;
        }
        for (PackageManager.Property property : properties.values()) {
            String propertyName = property.getName();
            String packageName = property.getPackageName();
            ArrayMap<String, ArrayList<PackageManager.Property>> propertyMap = propertyCollection.get(propertyName);
            if (propertyMap != null && (packageProperties = propertyMap.get(packageName)) != null) {
                packageProperties.remove(property);
                if (packageProperties.size() == 0) {
                    propertyMap.remove(packageName);
                }
                if (propertyMap.size() == 0) {
                    propertyCollection.remove(propertyName);
                }
            }
        }
        if (propertyCollection.size() == 0) {
            return null;
        }
        return propertyCollection;
    }

    private static PackageManager.Property getProperty(String propertyName, String packageName, String className, ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> propertyMap) {
        List<PackageManager.Property> propertyList;
        ArrayMap<String, ArrayList<PackageManager.Property>> packagePropertyMap = propertyMap.get(propertyName);
        if (packagePropertyMap == null || (propertyList = packagePropertyMap.get(packageName)) == null) {
            return null;
        }
        for (int i = propertyList.size() - 1; i >= 0; i--) {
            PackageManager.Property property = propertyList.get(i);
            if (Objects.equals(className, property.getClassName())) {
                return property;
            }
        }
        return null;
    }

    private PackageManager.Property getComponentProperty(String propertyName, String packageName, String className) {
        ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> arrayMap;
        ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> arrayMap2;
        ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> arrayMap3;
        ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> arrayMap4;
        PackageManager.Property property = null;
        if (0 == 0 && (arrayMap4 = this.mActivityProperties) != null) {
            property = getProperty(propertyName, packageName, className, arrayMap4);
        }
        if (property == null && (arrayMap3 = this.mProviderProperties) != null) {
            property = getProperty(propertyName, packageName, className, arrayMap3);
        }
        if (property == null && (arrayMap2 = this.mReceiverProperties) != null) {
            property = getProperty(propertyName, packageName, className, arrayMap2);
        }
        if (property == null && (arrayMap = this.mServiceProperties) != null) {
            return getProperty(propertyName, packageName, className, arrayMap);
        }
        return property;
    }

    private PackageManager.Property getApplicationProperty(String propertyName, String packageName) {
        List<PackageManager.Property> propertyList;
        ArrayMap<String, ArrayMap<String, ArrayList<PackageManager.Property>>> arrayMap = this.mApplicationProperties;
        ArrayMap<String, ArrayList<PackageManager.Property>> packagePropertyMap = arrayMap != null ? arrayMap.get(propertyName) : null;
        if (packagePropertyMap == null || (propertyList = packagePropertyMap.get(packageName)) == null) {
            return null;
        }
        return propertyList.get(0);
    }
}
