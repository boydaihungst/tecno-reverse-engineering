package android.telecom;

import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import com.android.internal.telecom.IInCallAdapter;
import java.util.List;
/* loaded from: classes3.dex */
public final class InCallAdapter {
    private final IInCallAdapter mAdapter;

    public InCallAdapter(IInCallAdapter adapter) {
        this.mAdapter = adapter;
    }

    public void answerCall(String callId, int videoState) {
        try {
            this.mAdapter.answerCall(callId, videoState);
        } catch (RemoteException e) {
        }
    }

    public void deflectCall(String callId, Uri address) {
        try {
            this.mAdapter.deflectCall(callId, address);
        } catch (RemoteException e) {
        }
    }

    public void rejectCall(String callId, boolean rejectWithMessage, String textMessage) {
        try {
            this.mAdapter.rejectCall(callId, rejectWithMessage, textMessage);
        } catch (RemoteException e) {
        }
    }

    public void rejectCall(String callId, int rejectReason) {
        try {
            this.mAdapter.rejectCallWithReason(callId, rejectReason);
        } catch (RemoteException e) {
        }
    }

    public void transferCall(String callId, Uri targetNumber, boolean isConfirmationRequired) {
        try {
            this.mAdapter.transferCall(callId, targetNumber, isConfirmationRequired);
        } catch (RemoteException e) {
        }
    }

    public void transferCall(String callId, String otherCallId) {
        try {
            this.mAdapter.consultativeTransfer(callId, otherCallId);
        } catch (RemoteException e) {
        }
    }

    public void disconnectCall(String callId) {
        try {
            this.mAdapter.disconnectCall(callId);
        } catch (RemoteException e) {
        }
    }

    public void holdCall(String callId) {
        try {
            this.mAdapter.holdCall(callId);
        } catch (RemoteException e) {
        }
    }

    public void unholdCall(String callId) {
        try {
            this.mAdapter.unholdCall(callId);
        } catch (RemoteException e) {
        }
    }

    public void mute(boolean shouldMute) {
        try {
            this.mAdapter.mute(shouldMute);
        } catch (RemoteException e) {
        }
    }

    public void setAudioRoute(int route) {
        try {
            this.mAdapter.setAudioRoute(route, null);
        } catch (RemoteException e) {
        }
    }

    public void enterBackgroundAudioProcessing(String callId) {
        try {
            this.mAdapter.enterBackgroundAudioProcessing(callId);
        } catch (RemoteException e) {
        }
    }

    public void exitBackgroundAudioProcessing(String callId, boolean shouldRing) {
        try {
            this.mAdapter.exitBackgroundAudioProcessing(callId, shouldRing);
        } catch (RemoteException e) {
        }
    }

    public void requestBluetoothAudio(String bluetoothAddress) {
        try {
            this.mAdapter.setAudioRoute(2, bluetoothAddress);
        } catch (RemoteException e) {
        }
    }

    public void playDtmfTone(String callId, char digit) {
        try {
            this.mAdapter.playDtmfTone(callId, digit);
        } catch (RemoteException e) {
        }
    }

    public void stopDtmfTone(String callId) {
        try {
            this.mAdapter.stopDtmfTone(callId);
        } catch (RemoteException e) {
        }
    }

    public void postDialContinue(String callId, boolean proceed) {
        try {
            this.mAdapter.postDialContinue(callId, proceed);
        } catch (RemoteException e) {
        }
    }

    public void phoneAccountSelected(String callId, PhoneAccountHandle accountHandle, boolean setDefault) {
        try {
            this.mAdapter.phoneAccountSelected(callId, accountHandle, setDefault);
        } catch (RemoteException e) {
        }
    }

    public void conference(String callId, String otherCallId) {
        try {
            this.mAdapter.conference(callId, otherCallId);
        } catch (RemoteException e) {
        }
    }

    public void addConferenceParticipants(String callId, List<Uri> participants) {
        try {
            this.mAdapter.addConferenceParticipants(callId, participants);
        } catch (RemoteException e) {
        }
    }

    public void splitFromConference(String callId) {
        try {
            this.mAdapter.splitFromConference(callId);
        } catch (RemoteException e) {
        }
    }

    public void mergeConference(String callId) {
        try {
            this.mAdapter.mergeConference(callId);
        } catch (RemoteException e) {
        }
    }

    public void swapConference(String callId) {
        try {
            this.mAdapter.swapConference(callId);
        } catch (RemoteException e) {
        }
    }

    public void pullExternalCall(String callId) {
        try {
            this.mAdapter.pullExternalCall(callId);
        } catch (RemoteException e) {
        }
    }

    public void sendCallEvent(String callId, String event, int targetSdkVer, Bundle extras) {
        try {
            this.mAdapter.sendCallEvent(callId, event, targetSdkVer, extras);
        } catch (RemoteException e) {
        }
    }

    public void putExtras(String callId, Bundle extras) {
        try {
            this.mAdapter.putExtras(callId, extras);
        } catch (RemoteException e) {
        }
    }

    public void putExtra(String callId, String key, boolean value) {
        try {
            Bundle bundle = new Bundle();
            bundle.putBoolean(key, value);
            this.mAdapter.putExtras(callId, bundle);
        } catch (RemoteException e) {
        }
    }

    public void putExtra(String callId, String key, int value) {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt(key, value);
            this.mAdapter.putExtras(callId, bundle);
        } catch (RemoteException e) {
        }
    }

    public void putExtra(String callId, String key, String value) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(key, value);
            this.mAdapter.putExtras(callId, bundle);
        } catch (RemoteException e) {
        }
    }

    public void removeExtras(String callId, List<String> keys) {
        try {
            this.mAdapter.removeExtras(callId, keys);
        } catch (RemoteException e) {
        }
    }

    public void turnProximitySensorOn() {
        try {
            this.mAdapter.turnOnProximitySensor();
        } catch (RemoteException e) {
        }
    }

    public void turnProximitySensorOff(boolean screenOnImmediately) {
        try {
            this.mAdapter.turnOffProximitySensor(screenOnImmediately);
        } catch (RemoteException e) {
        }
    }

    public void sendRttRequest(String callId) {
        try {
            this.mAdapter.sendRttRequest(callId);
        } catch (RemoteException e) {
        }
    }

    public void respondToRttRequest(String callId, int id, boolean accept) {
        try {
            this.mAdapter.respondToRttRequest(callId, id, accept);
        } catch (RemoteException e) {
        }
    }

    public void stopRtt(String callId) {
        try {
            this.mAdapter.stopRtt(callId);
        } catch (RemoteException e) {
        }
    }

    public void setRttMode(String callId, int mode) {
        try {
            this.mAdapter.setRttMode(callId, mode);
        } catch (RemoteException e) {
        }
    }

    public void handoverTo(String callId, PhoneAccountHandle destAcct, int videoState, Bundle extras) {
        try {
            this.mAdapter.handoverTo(callId, destAcct, videoState, extras);
        } catch (RemoteException e) {
        }
    }

    public void doTranAction(Bundle params) {
        try {
            this.mAdapter.doTranAction(params);
        } catch (RemoteException e) {
        }
    }
}
