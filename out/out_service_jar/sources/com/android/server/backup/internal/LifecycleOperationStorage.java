package com.android.server.backup.internal;

import android.util.Slog;
import android.util.SparseArray;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.OperationStorage;
import com.google.android.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
/* loaded from: classes.dex */
public class LifecycleOperationStorage implements OperationStorage {
    private static final String TAG = "LifecycleOperationStorage";
    private final int mUserId;
    private final Object mOperationsLock = new Object();
    private final SparseArray<Operation> mOperations = new SparseArray<>();
    private final Map<String, Set<Integer>> mOpTokensByPackage = new HashMap();

    public LifecycleOperationStorage(int userId) {
        this.mUserId = userId;
    }

    @Override // com.android.server.backup.OperationStorage
    public void registerOperation(int token, int initialState, BackupRestoreTask task, int type) {
        registerOperationForPackages(token, initialState, Sets.newHashSet(), task, type);
    }

    @Override // com.android.server.backup.OperationStorage
    public void registerOperationForPackages(int token, int initialState, Set<String> packageNames, BackupRestoreTask task, int type) {
        synchronized (this.mOperationsLock) {
            this.mOperations.put(token, new Operation(initialState, task, type));
            for (String packageName : packageNames) {
                Set<Integer> tokens = this.mOpTokensByPackage.get(packageName);
                if (tokens == null) {
                    tokens = new HashSet();
                }
                tokens.add(Integer.valueOf(token));
                this.mOpTokensByPackage.put(packageName, tokens);
            }
        }
    }

    @Override // com.android.server.backup.OperationStorage
    public void removeOperation(int token) {
        synchronized (this.mOperationsLock) {
            this.mOperations.remove(token);
            Set<String> packagesWithTokens = this.mOpTokensByPackage.keySet();
            for (String packageName : packagesWithTokens) {
                Set<Integer> tokens = this.mOpTokensByPackage.get(packageName);
                if (tokens != null) {
                    tokens.remove(Integer.valueOf(token));
                    this.mOpTokensByPackage.put(packageName, tokens);
                }
            }
        }
    }

    @Override // com.android.server.backup.OperationStorage
    public int numOperations() {
        int size;
        synchronized (this.mOperationsLock) {
            size = this.mOperations.size();
        }
        return size;
    }

    @Override // com.android.server.backup.OperationStorage
    public boolean isBackupOperationInProgress() {
        synchronized (this.mOperationsLock) {
            for (int i = 0; i < this.mOperations.size(); i++) {
                Operation op = this.mOperations.valueAt(i);
                if (op.type == 2 && op.state == 0) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.backup.OperationStorage
    public Set<Integer> operationTokensForPackage(String packageName) {
        Set<Integer> result;
        synchronized (this.mOperationsLock) {
            Collection<? extends Integer> tokens = (Set) this.mOpTokensByPackage.get(packageName);
            result = Sets.newHashSet();
            if (tokens != null) {
                result.addAll(tokens);
            }
        }
        return result;
    }

    @Override // com.android.server.backup.OperationStorage
    public Set<Integer> operationTokensForOpType(int type) {
        Set<Integer> tokens = Sets.newHashSet();
        synchronized (this.mOperationsLock) {
            for (int i = 0; i < this.mOperations.size(); i++) {
                Operation op = this.mOperations.valueAt(i);
                int token = this.mOperations.keyAt(i);
                if (op.type == type) {
                    tokens.add(Integer.valueOf(token));
                }
            }
        }
        return tokens;
    }

    @Override // com.android.server.backup.OperationStorage
    public Set<Integer> operationTokensForOpState(int state) {
        Set<Integer> tokens = Sets.newHashSet();
        synchronized (this.mOperationsLock) {
            for (int i = 0; i < this.mOperations.size(); i++) {
                Operation op = this.mOperations.valueAt(i);
                int token = this.mOperations.keyAt(i);
                if (op.state == state) {
                    tokens.add(Integer.valueOf(token));
                }
            }
        }
        return tokens;
    }

    public boolean waitUntilOperationComplete(int token, IntConsumer callback) {
        Operation op;
        int finalState = 0;
        synchronized (this.mOperationsLock) {
            while (true) {
                op = this.mOperations.get(token);
                if (op == null) {
                    break;
                } else if (op.state == 0) {
                    try {
                        this.mOperationsLock.wait();
                    } catch (InterruptedException e) {
                        Slog.w(TAG, "Waiting on mOperationsLock: ", e);
                    }
                } else {
                    finalState = op.state;
                    break;
                }
            }
        }
        removeOperation(token);
        if (op != null) {
            callback.accept(op.type);
        }
        return finalState == 1;
    }

    public void onOperationComplete(int token, long result, Consumer<BackupRestoreTask> callback) {
        Operation op;
        synchronized (this.mOperationsLock) {
            op = this.mOperations.get(token);
            if (op != null) {
                if (op.state == -1) {
                    op = null;
                    this.mOperations.remove(token);
                } else if (op.state == 1) {
                    Slog.w(TAG, "[UserID:" + this.mUserId + "] Received duplicate ack for token=" + Integer.toHexString(token));
                    op = null;
                    this.mOperations.remove(token);
                } else if (op.state == 0) {
                    op.state = 1;
                }
            }
            this.mOperationsLock.notifyAll();
        }
        if (op != null && op.callback != null) {
            callback.accept(op.callback);
        }
    }

    public void cancelOperation(int token, boolean cancelAll, IntConsumer operationTimedOutCallback) {
        Operation op;
        synchronized (this.mOperationsLock) {
            op = this.mOperations.get(token);
            int state = op != null ? op.state : -1;
            if (state == 1) {
                Slog.w(TAG, "[UserID:" + this.mUserId + "] Operation already got an ack.Should have been removed from mCurrentOperations.");
                op = null;
                this.mOperations.delete(token);
            } else if (state == 0) {
                Slog.v(TAG, "[UserID:" + this.mUserId + "] Cancel: token=" + Integer.toHexString(token));
                op.state = -1;
                operationTimedOutCallback.accept(op.type);
            }
            this.mOperationsLock.notifyAll();
        }
        if (op != null && op.callback != null) {
            op.callback.handleCancel(cancelAll);
        }
    }
}
