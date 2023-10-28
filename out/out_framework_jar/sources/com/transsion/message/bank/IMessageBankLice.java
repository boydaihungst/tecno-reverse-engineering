package com.transsion.message.bank;

import android.content.Context;
import android.widget.TextView;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes4.dex */
public interface IMessageBankLice {
    public static final LiceInfo<IMessageBankLice> sLiceInfo = new LiceInfo<>("com.transsion.message.bank.v1.MessageBankLice", IMessageBankLice.class, new Supplier() { // from class: com.transsion.message.bank.IMessageBankLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return IMessageBankLice.lambda$static$0();
        }
    });

    static /* synthetic */ IMessageBankLice lambda$static$0() {
        return new IMessageBankLice() { // from class: com.transsion.message.bank.IMessageBankLice.1
        };
    }

    static IMessageBankLice Instance() {
        return sLiceInfo.getImpl();
    }

    default void onTextSelected(Context context, String text) {
    }

    default void onTextSelectedChanged(TextView textView, String reason) {
    }

    default boolean hasSelectedText(Context contet) {
        return false;
    }
}
