package com.android.server.hdmi;

import com.android.internal.util.FrameworkStatsLog;
/* loaded from: classes.dex */
public class HdmiCecAtomWriter {
    private static final int ERROR_CODE_UNKNOWN = -1;
    protected static final int FEATURE_ABORT_OPCODE_UNKNOWN = 256;

    public void messageReported(HdmiCecMessage message, int direction, int callingUid, int errorCode) {
        MessageReportedGenericArgs genericArgs = createMessageReportedGenericArgs(message, direction, errorCode, callingUid);
        MessageReportedSpecialArgs specialArgs = createMessageReportedSpecialArgs(message);
        messageReportedBase(genericArgs, specialArgs);
    }

    public void messageReported(HdmiCecMessage message, int direction, int callingUid) {
        messageReported(message, direction, callingUid, -1);
    }

    private MessageReportedGenericArgs createMessageReportedGenericArgs(HdmiCecMessage message, int direction, int errorCode, int callingUid) {
        int sendMessageResult;
        if (errorCode == -1) {
            sendMessageResult = 0;
        } else {
            sendMessageResult = errorCode + 10;
        }
        return new MessageReportedGenericArgs(callingUid, direction, message.getSource(), message.getDestination(), message.getOpcode(), sendMessageResult);
    }

    private MessageReportedSpecialArgs createMessageReportedSpecialArgs(HdmiCecMessage message) {
        switch (message.getOpcode()) {
            case 0:
                return createFeatureAbortSpecialArgs(message);
            case 68:
                return createUserControlPressedSpecialArgs(message);
            default:
                return new MessageReportedSpecialArgs();
        }
    }

    private MessageReportedSpecialArgs createUserControlPressedSpecialArgs(HdmiCecMessage message) {
        MessageReportedSpecialArgs specialArgs = new MessageReportedSpecialArgs();
        if (message.getParams().length > 0) {
            int keycode = message.getParams()[0];
            if (keycode >= 30 && keycode <= 41) {
                specialArgs.mUserControlPressedCommand = 2;
            } else {
                specialArgs.mUserControlPressedCommand = keycode + 256;
            }
        }
        return specialArgs;
    }

    private MessageReportedSpecialArgs createFeatureAbortSpecialArgs(HdmiCecMessage message) {
        MessageReportedSpecialArgs specialArgs = new MessageReportedSpecialArgs();
        if (message.getParams().length > 0) {
            specialArgs.mFeatureAbortOpcode = message.getParams()[0] & 255;
            if (message.getParams().length > 1) {
                specialArgs.mFeatureAbortReason = message.getParams()[1] + 10;
            }
        }
        return specialArgs;
    }

    private void messageReportedBase(MessageReportedGenericArgs genericArgs, MessageReportedSpecialArgs specialArgs) {
        writeHdmiCecMessageReportedAtom(genericArgs.mUid, genericArgs.mDirection, genericArgs.mInitiatorLogicalAddress, genericArgs.mDestinationLogicalAddress, genericArgs.mOpcode, genericArgs.mSendMessageResult, specialArgs.mUserControlPressedCommand, specialArgs.mFeatureAbortOpcode, specialArgs.mFeatureAbortReason);
    }

    protected void writeHdmiCecMessageReportedAtom(int uid, int direction, int initiatorLogicalAddress, int destinationLogicalAddress, int opcode, int sendMessageResult, int userControlPressedCommand, int featureAbortOpcode, int featureAbortReason) {
        FrameworkStatsLog.write(310, uid, direction, initiatorLogicalAddress, destinationLogicalAddress, opcode, sendMessageResult, userControlPressedCommand, featureAbortOpcode, featureAbortReason);
    }

    public void activeSourceChanged(int logicalAddress, int physicalAddress, int relationshipToActiveSource) {
        FrameworkStatsLog.write(309, logicalAddress, physicalAddress, relationshipToActiveSource);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MessageReportedGenericArgs {
        final int mDestinationLogicalAddress;
        final int mDirection;
        final int mInitiatorLogicalAddress;
        final int mOpcode;
        final int mSendMessageResult;
        final int mUid;

        MessageReportedGenericArgs(int uid, int direction, int initiatorLogicalAddress, int destinationLogicalAddress, int opcode, int sendMessageResult) {
            this.mUid = uid;
            this.mDirection = direction;
            this.mInitiatorLogicalAddress = initiatorLogicalAddress;
            this.mDestinationLogicalAddress = destinationLogicalAddress;
            this.mOpcode = opcode;
            this.mSendMessageResult = sendMessageResult;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MessageReportedSpecialArgs {
        int mFeatureAbortOpcode;
        int mFeatureAbortReason;
        int mUserControlPressedCommand;

        private MessageReportedSpecialArgs() {
            this.mUserControlPressedCommand = 0;
            this.mFeatureAbortOpcode = 256;
            this.mFeatureAbortReason = 0;
        }
    }
}
