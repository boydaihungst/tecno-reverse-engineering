package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;
/* loaded from: classes.dex */
public class ConfigurationChangeItem extends ClientTransactionItem {
    public static final Parcelable.Creator<ConfigurationChangeItem> CREATOR = new Parcelable.Creator<ConfigurationChangeItem>() { // from class: android.app.servertransaction.ConfigurationChangeItem.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ConfigurationChangeItem createFromParcel(Parcel in) {
            return new ConfigurationChangeItem(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ConfigurationChangeItem[] newArray(int size) {
            return new ConfigurationChangeItem[size];
        }
    };
    private Configuration mConfiguration;

    @Override // android.app.servertransaction.BaseClientRequest
    public void preExecute(ClientTransactionHandler client, IBinder token) {
        client.updatePendingConfiguration(this.mConfiguration);
    }

    @Override // android.app.servertransaction.BaseClientRequest
    public void execute(ClientTransactionHandler client, IBinder token, PendingTransactionActions pendingActions) {
        client.handleConfigurationChanged(this.mConfiguration);
    }

    private ConfigurationChangeItem() {
    }

    public static ConfigurationChangeItem obtain(Configuration config) {
        ConfigurationChangeItem instance = (ConfigurationChangeItem) ObjectPool.obtain(ConfigurationChangeItem.class);
        if (instance == null) {
            instance = new ConfigurationChangeItem();
        }
        instance.mConfiguration = config;
        return instance;
    }

    @Override // android.app.servertransaction.ObjectPoolItem
    public void recycle() {
        this.mConfiguration = null;
        ObjectPool.recycle(this);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedObject(this.mConfiguration, flags);
    }

    private ConfigurationChangeItem(Parcel in) {
        this.mConfiguration = (Configuration) in.readTypedObject(Configuration.CREATOR);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigurationChangeItem other = (ConfigurationChangeItem) o;
        return Objects.equals(this.mConfiguration, other.mConfiguration);
    }

    public int hashCode() {
        return this.mConfiguration.hashCode();
    }

    public String toString() {
        return "ConfigurationChangeItem{config=" + this.mConfiguration + "}";
    }
}
