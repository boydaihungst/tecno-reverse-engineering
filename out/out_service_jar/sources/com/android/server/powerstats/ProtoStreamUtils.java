package com.android.server.powerstats;

import android.hardware.power.stats.Channel;
import android.hardware.power.stats.EnergyConsumer;
import android.hardware.power.stats.EnergyConsumerAttribution;
import android.hardware.power.stats.EnergyConsumerResult;
import android.hardware.power.stats.EnergyMeasurement;
import android.hardware.power.stats.PowerEntity;
import android.hardware.power.stats.State;
import android.hardware.power.stats.StateResidency;
import android.hardware.power.stats.StateResidencyResult;
import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import android.util.proto.WireTypeMismatchException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class ProtoStreamUtils {
    private static final String TAG = ProtoStreamUtils.class.getSimpleName();

    /* loaded from: classes2.dex */
    static class PowerEntityUtils {
        PowerEntityUtils() {
        }

        public static byte[] getProtoBytes(PowerEntity[] powerEntity) {
            ProtoOutputStream pos = new ProtoOutputStream();
            packProtoMessage(powerEntity, pos);
            return pos.getBytes();
        }

        public static void packProtoMessage(PowerEntity[] powerEntity, ProtoOutputStream pos) {
            if (powerEntity == null) {
                return;
            }
            for (int i = 0; i < powerEntity.length; i++) {
                long peToken = pos.start(CompanionAppsPermissions.APP_PERMISSIONS);
                pos.write(CompanionMessage.MESSAGE_ID, powerEntity[i].id);
                pos.write(1138166333442L, powerEntity[i].name);
                if (powerEntity[i].states != null) {
                    int statesLength = powerEntity[i].states.length;
                    for (int j = 0; j < statesLength; j++) {
                        State state = powerEntity[i].states[j];
                        long stateToken = pos.start(2246267895811L);
                        pos.write(CompanionMessage.MESSAGE_ID, state.id);
                        pos.write(1138166333442L, state.name);
                        pos.end(stateToken);
                    }
                }
                pos.end(peToken);
            }
        }

        public static void print(PowerEntity[] powerEntity) {
            if (powerEntity == null) {
                return;
            }
            for (int i = 0; i < powerEntity.length; i++) {
                Slog.d(ProtoStreamUtils.TAG, "powerEntityId: " + powerEntity[i].id + ", powerEntityName: " + powerEntity[i].name);
                if (powerEntity[i].states != null) {
                    for (int j = 0; j < powerEntity[i].states.length; j++) {
                        Slog.d(ProtoStreamUtils.TAG, "  StateId: " + powerEntity[i].states[j].id + ", StateName: " + powerEntity[i].states[j].name);
                    }
                }
            }
        }

        public static void dumpsys(PowerEntity[] powerEntity, PrintWriter pw) {
            if (powerEntity == null) {
                return;
            }
            for (int i = 0; i < powerEntity.length; i++) {
                pw.println("PowerEntityId: " + powerEntity[i].id + ", PowerEntityName: " + powerEntity[i].name);
                if (powerEntity[i].states != null) {
                    for (int j = 0; j < powerEntity[i].states.length; j++) {
                        pw.println("  StateId: " + powerEntity[i].states[j].id + ", StateName: " + powerEntity[i].states[j].name);
                    }
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    static class StateResidencyResultUtils {
        StateResidencyResultUtils() {
        }

        public static void adjustTimeSinceBootToEpoch(StateResidencyResult[] stateResidencyResult, long startWallTime) {
            if (stateResidencyResult == null) {
                return;
            }
            for (int i = 0; i < stateResidencyResult.length; i++) {
                int stateLength = stateResidencyResult[i].stateResidencyData.length;
                for (int j = 0; j < stateLength; j++) {
                    StateResidency stateResidencyData = stateResidencyResult[i].stateResidencyData[j];
                    stateResidencyData.lastEntryTimestampMs += startWallTime;
                }
            }
        }

        public static byte[] getProtoBytes(StateResidencyResult[] stateResidencyResult) {
            ProtoOutputStream pos = new ProtoOutputStream();
            packProtoMessage(stateResidencyResult, pos);
            return pos.getBytes();
        }

        public static void packProtoMessage(StateResidencyResult[] stateResidencyResult, ProtoOutputStream pos) {
            if (stateResidencyResult == null) {
                return;
            }
            for (int i = 0; i < stateResidencyResult.length; i++) {
                int stateLength = stateResidencyResult[i].stateResidencyData.length;
                long j = 2246267895810L;
                long srrToken = pos.start(2246267895810L);
                pos.write(CompanionMessage.MESSAGE_ID, stateResidencyResult[i].id);
                int j2 = 0;
                while (j2 < stateLength) {
                    StateResidency stateResidencyData = stateResidencyResult[i].stateResidencyData[j2];
                    long srdToken = pos.start(j);
                    pos.write(CompanionMessage.MESSAGE_ID, stateResidencyData.id);
                    pos.write(1112396529666L, stateResidencyData.totalTimeInStateMs);
                    pos.write(1112396529667L, stateResidencyData.totalStateEntryCount);
                    pos.write(1112396529668L, stateResidencyData.lastEntryTimestampMs);
                    pos.end(srdToken);
                    j2++;
                    j = 2246267895810L;
                }
                pos.end(srrToken);
            }
        }

        public static StateResidencyResult[] unpackProtoMessage(byte[] data) throws IOException {
            ProtoInputStream pis = new ProtoInputStream(new ByteArrayInputStream(data));
            List<StateResidencyResult> stateResidencyResultList = new ArrayList<>();
            while (true) {
                try {
                    int nextField = pis.nextField();
                    new StateResidencyResult();
                    if (nextField == 2) {
                        long token = pis.start(2246267895810L);
                        stateResidencyResultList.add(unpackStateResidencyResultProto(pis));
                        pis.end(token);
                    } else if (nextField == -1) {
                        return (StateResidencyResult[]) stateResidencyResultList.toArray(new StateResidencyResult[stateResidencyResultList.size()]);
                    } else {
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in PowerStatsServiceResidencyProto: " + ProtoUtils.currentFieldToString(pis));
                    }
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in PowerStatsServiceResidencyProto: " + ProtoUtils.currentFieldToString(pis));
                }
            }
        }

        private static StateResidencyResult unpackStateResidencyResultProto(ProtoInputStream pis) throws IOException {
            StateResidencyResult stateResidencyResult = new StateResidencyResult();
            List<StateResidency> stateResidencyList = new ArrayList<>();
            while (true) {
                try {
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in StateResidencyResultProto: " + ProtoUtils.currentFieldToString(pis));
                }
                switch (pis.nextField()) {
                    case -1:
                        stateResidencyResult.stateResidencyData = (StateResidency[]) stateResidencyList.toArray(new StateResidency[stateResidencyList.size()]);
                        return stateResidencyResult;
                    case 0:
                    default:
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in StateResidencyResultProto: " + ProtoUtils.currentFieldToString(pis));
                        continue;
                    case 1:
                        stateResidencyResult.id = pis.readInt((long) CompanionMessage.MESSAGE_ID);
                        continue;
                    case 2:
                        long token = pis.start(2246267895810L);
                        stateResidencyList.add(unpackStateResidencyProto(pis));
                        pis.end(token);
                        continue;
                }
                Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in StateResidencyResultProto: " + ProtoUtils.currentFieldToString(pis));
            }
        }

        private static StateResidency unpackStateResidencyProto(ProtoInputStream pis) throws IOException {
            StateResidency stateResidency = new StateResidency();
            while (true) {
                try {
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in StateResidencyProto: " + ProtoUtils.currentFieldToString(pis));
                }
                switch (pis.nextField()) {
                    case -1:
                        return stateResidency;
                    case 0:
                    default:
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in StateResidencyProto: " + ProtoUtils.currentFieldToString(pis));
                        continue;
                    case 1:
                        stateResidency.id = pis.readInt((long) CompanionMessage.MESSAGE_ID);
                        continue;
                    case 2:
                        stateResidency.totalTimeInStateMs = pis.readLong(1112396529666L);
                        continue;
                    case 3:
                        stateResidency.totalStateEntryCount = pis.readLong(1112396529667L);
                        continue;
                    case 4:
                        stateResidency.lastEntryTimestampMs = pis.readLong(1112396529668L);
                        continue;
                }
                Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in StateResidencyProto: " + ProtoUtils.currentFieldToString(pis));
            }
        }

        public static void print(StateResidencyResult[] stateResidencyResult) {
            if (stateResidencyResult == null) {
                return;
            }
            for (int i = 0; i < stateResidencyResult.length; i++) {
                Slog.d(ProtoStreamUtils.TAG, "PowerEntityId: " + stateResidencyResult[i].id);
                for (int j = 0; j < stateResidencyResult[i].stateResidencyData.length; j++) {
                    Slog.d(ProtoStreamUtils.TAG, "  StateId: " + stateResidencyResult[i].stateResidencyData[j].id + ", TotalTimeInStateMs: " + stateResidencyResult[i].stateResidencyData[j].totalTimeInStateMs + ", TotalStateEntryCount: " + stateResidencyResult[i].stateResidencyData[j].totalStateEntryCount + ", LastEntryTimestampMs: " + stateResidencyResult[i].stateResidencyData[j].lastEntryTimestampMs);
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    static class ChannelUtils {
        ChannelUtils() {
        }

        public static byte[] getProtoBytes(Channel[] channel) {
            ProtoOutputStream pos = new ProtoOutputStream();
            packProtoMessage(channel, pos);
            return pos.getBytes();
        }

        public static void packProtoMessage(Channel[] channel, ProtoOutputStream pos) {
            if (channel == null) {
                return;
            }
            for (int i = 0; i < channel.length; i++) {
                long token = pos.start(CompanionAppsPermissions.APP_PERMISSIONS);
                pos.write(CompanionMessage.MESSAGE_ID, channel[i].id);
                pos.write(1138166333442L, channel[i].name);
                pos.write(1138166333443L, channel[i].subsystem);
                pos.end(token);
            }
        }

        public static void print(Channel[] channel) {
            if (channel == null) {
                return;
            }
            for (int i = 0; i < channel.length; i++) {
                Slog.d(ProtoStreamUtils.TAG, "ChannelId: " + channel[i].id + ", ChannelName: " + channel[i].name + ", ChannelSubsystem: " + channel[i].subsystem);
            }
        }

        public static void dumpsys(Channel[] channel, PrintWriter pw) {
            if (channel == null) {
                return;
            }
            for (int i = 0; i < channel.length; i++) {
                pw.println("ChannelId: " + channel[i].id + ", ChannelName: " + channel[i].name + ", ChannelSubsystem: " + channel[i].subsystem);
            }
        }
    }

    /* loaded from: classes2.dex */
    static class EnergyMeasurementUtils {
        EnergyMeasurementUtils() {
        }

        public static void adjustTimeSinceBootToEpoch(EnergyMeasurement[] energyMeasurement, long startWallTime) {
            if (energyMeasurement == null) {
                return;
            }
            for (EnergyMeasurement energyMeasurement2 : energyMeasurement) {
                energyMeasurement2.timestampMs += startWallTime;
            }
        }

        public static byte[] getProtoBytes(EnergyMeasurement[] energyMeasurement) {
            ProtoOutputStream pos = new ProtoOutputStream();
            packProtoMessage(energyMeasurement, pos);
            return pos.getBytes();
        }

        public static void packProtoMessage(EnergyMeasurement[] energyMeasurement, ProtoOutputStream pos) {
            if (energyMeasurement == null) {
                return;
            }
            for (int i = 0; i < energyMeasurement.length; i++) {
                long token = pos.start(2246267895810L);
                pos.write(CompanionMessage.MESSAGE_ID, energyMeasurement[i].id);
                pos.write(1112396529666L, energyMeasurement[i].timestampMs);
                pos.write(1112396529668L, energyMeasurement[i].durationMs);
                pos.write(1112396529667L, energyMeasurement[i].energyUWs);
                pos.end(token);
            }
        }

        public static EnergyMeasurement[] unpackProtoMessage(byte[] data) throws IOException {
            ProtoInputStream pis = new ProtoInputStream(new ByteArrayInputStream(data));
            List<EnergyMeasurement> energyMeasurementList = new ArrayList<>();
            while (true) {
                try {
                    int nextField = pis.nextField();
                    new EnergyMeasurement();
                    if (nextField == 2) {
                        long token = pis.start(2246267895810L);
                        energyMeasurementList.add(unpackEnergyMeasurementProto(pis));
                        pis.end(token);
                    } else if (nextField == -1) {
                        return (EnergyMeasurement[]) energyMeasurementList.toArray(new EnergyMeasurement[energyMeasurementList.size()]);
                    } else {
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in proto: " + ProtoUtils.currentFieldToString(pis));
                    }
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in proto: " + ProtoUtils.currentFieldToString(pis));
                }
            }
        }

        private static EnergyMeasurement unpackEnergyMeasurementProto(ProtoInputStream pis) throws IOException {
            EnergyMeasurement energyMeasurement = new EnergyMeasurement();
            while (true) {
                try {
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in EnergyMeasurementProto: " + ProtoUtils.currentFieldToString(pis));
                }
                switch (pis.nextField()) {
                    case -1:
                        return energyMeasurement;
                    case 0:
                    default:
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in EnergyMeasurementProto: " + ProtoUtils.currentFieldToString(pis));
                        continue;
                    case 1:
                        energyMeasurement.id = pis.readInt((long) CompanionMessage.MESSAGE_ID);
                        continue;
                    case 2:
                        energyMeasurement.timestampMs = pis.readLong(1112396529666L);
                        continue;
                    case 3:
                        energyMeasurement.energyUWs = pis.readLong(1112396529667L);
                        continue;
                    case 4:
                        energyMeasurement.durationMs = pis.readLong(1112396529668L);
                        continue;
                }
                Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in EnergyMeasurementProto: " + ProtoUtils.currentFieldToString(pis));
            }
        }

        public static void print(EnergyMeasurement[] energyMeasurement) {
            if (energyMeasurement == null) {
                return;
            }
            for (int i = 0; i < energyMeasurement.length; i++) {
                Slog.d(ProtoStreamUtils.TAG, "ChannelId: " + energyMeasurement[i].id + ", Timestamp (ms): " + energyMeasurement[i].timestampMs + ", Duration (ms): " + energyMeasurement[i].durationMs + ", Energy (uWs): " + energyMeasurement[i].energyUWs);
            }
        }
    }

    /* loaded from: classes2.dex */
    static class EnergyConsumerUtils {
        EnergyConsumerUtils() {
        }

        public static byte[] getProtoBytes(EnergyConsumer[] energyConsumer) {
            ProtoOutputStream pos = new ProtoOutputStream();
            packProtoMessage(energyConsumer, pos);
            return pos.getBytes();
        }

        public static void packProtoMessage(EnergyConsumer[] energyConsumer, ProtoOutputStream pos) {
            if (energyConsumer == null) {
                return;
            }
            for (int i = 0; i < energyConsumer.length; i++) {
                long token = pos.start(CompanionAppsPermissions.APP_PERMISSIONS);
                pos.write(CompanionMessage.MESSAGE_ID, energyConsumer[i].id);
                pos.write(1120986464258L, energyConsumer[i].ordinal);
                pos.write(1120986464259L, (int) energyConsumer[i].type);
                pos.write(1138166333444L, energyConsumer[i].name);
                pos.end(token);
            }
        }

        public static EnergyConsumer[] unpackProtoMessage(byte[] data) throws IOException {
            ProtoInputStream pis = new ProtoInputStream(new ByteArrayInputStream(data));
            List<EnergyConsumer> energyConsumerList = new ArrayList<>();
            while (true) {
                try {
                    int nextField = pis.nextField();
                    new EnergyConsumer();
                    if (nextField == 1) {
                        long token = pis.start((long) CompanionAppsPermissions.APP_PERMISSIONS);
                        energyConsumerList.add(unpackEnergyConsumerProto(pis));
                        pis.end(token);
                    } else if (nextField == -1) {
                        return (EnergyConsumer[]) energyConsumerList.toArray(new EnergyConsumer[energyConsumerList.size()]);
                    } else {
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in proto: " + ProtoUtils.currentFieldToString(pis));
                    }
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in proto: " + ProtoUtils.currentFieldToString(pis));
                }
            }
        }

        private static EnergyConsumer unpackEnergyConsumerProto(ProtoInputStream pis) throws IOException {
            EnergyConsumer energyConsumer = new EnergyConsumer();
            while (true) {
                try {
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in EnergyConsumerProto: " + ProtoUtils.currentFieldToString(pis));
                }
                switch (pis.nextField()) {
                    case -1:
                        return energyConsumer;
                    case 0:
                    default:
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in EnergyConsumerProto: " + ProtoUtils.currentFieldToString(pis));
                        continue;
                    case 1:
                        energyConsumer.id = pis.readInt((long) CompanionMessage.MESSAGE_ID);
                        continue;
                    case 2:
                        energyConsumer.ordinal = pis.readInt(1120986464258L);
                        continue;
                    case 3:
                        energyConsumer.type = (byte) pis.readInt(1120986464259L);
                        continue;
                    case 4:
                        energyConsumer.name = pis.readString(1138166333444L);
                        continue;
                }
                Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in EnergyConsumerProto: " + ProtoUtils.currentFieldToString(pis));
            }
        }

        public static void print(EnergyConsumer[] energyConsumer) {
            if (energyConsumer == null) {
                return;
            }
            for (int i = 0; i < energyConsumer.length; i++) {
                Slog.d(ProtoStreamUtils.TAG, "EnergyConsumerId: " + energyConsumer[i].id + ", Ordinal: " + energyConsumer[i].ordinal + ", Type: " + ((int) energyConsumer[i].type) + ", Name: " + energyConsumer[i].name);
            }
        }

        public static void dumpsys(EnergyConsumer[] energyConsumer, PrintWriter pw) {
            if (energyConsumer == null) {
                return;
            }
            for (int i = 0; i < energyConsumer.length; i++) {
                pw.println("EnergyConsumerId: " + energyConsumer[i].id + ", Ordinal: " + energyConsumer[i].ordinal + ", Type: " + ((int) energyConsumer[i].type) + ", Name: " + energyConsumer[i].name);
            }
        }
    }

    /* loaded from: classes2.dex */
    static class EnergyConsumerResultUtils {
        EnergyConsumerResultUtils() {
        }

        public static void adjustTimeSinceBootToEpoch(EnergyConsumerResult[] energyConsumerResult, long startWallTime) {
            if (energyConsumerResult == null) {
                return;
            }
            for (EnergyConsumerResult energyConsumerResult2 : energyConsumerResult) {
                energyConsumerResult2.timestampMs += startWallTime;
            }
        }

        public static byte[] getProtoBytes(EnergyConsumerResult[] energyConsumerResult, boolean includeAttribution) {
            ProtoOutputStream pos = new ProtoOutputStream();
            packProtoMessage(energyConsumerResult, pos, includeAttribution);
            return pos.getBytes();
        }

        public static void packProtoMessage(EnergyConsumerResult[] energyConsumerResult, ProtoOutputStream pos, boolean includeAttribution) {
            if (energyConsumerResult == null) {
                return;
            }
            for (int i = 0; i < energyConsumerResult.length; i++) {
                long ecrToken = pos.start(2246267895810L);
                pos.write(CompanionMessage.MESSAGE_ID, energyConsumerResult[i].id);
                pos.write(1112396529666L, energyConsumerResult[i].timestampMs);
                pos.write(1112396529667L, energyConsumerResult[i].energyUWs);
                if (includeAttribution) {
                    int attributionLength = energyConsumerResult[i].attribution.length;
                    for (int j = 0; j < attributionLength; j++) {
                        EnergyConsumerAttribution energyConsumerAttribution = energyConsumerResult[i].attribution[j];
                        long ecaToken = pos.start(2246267895812L);
                        pos.write(CompanionMessage.MESSAGE_ID, energyConsumerAttribution.uid);
                        pos.write(1112396529666L, energyConsumerAttribution.energyUWs);
                        pos.end(ecaToken);
                    }
                }
                pos.end(ecrToken);
            }
        }

        public static EnergyConsumerResult[] unpackProtoMessage(byte[] data) throws IOException {
            ProtoInputStream pis = new ProtoInputStream(new ByteArrayInputStream(data));
            List<EnergyConsumerResult> energyConsumerResultList = new ArrayList<>();
            while (true) {
                try {
                    int nextField = pis.nextField();
                    new EnergyConsumerResult();
                    if (nextField == 2) {
                        long token = pis.start(2246267895810L);
                        energyConsumerResultList.add(unpackEnergyConsumerResultProto(pis));
                        pis.end(token);
                    } else if (nextField == -1) {
                        return (EnergyConsumerResult[]) energyConsumerResultList.toArray(new EnergyConsumerResult[energyConsumerResultList.size()]);
                    } else {
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in proto: " + ProtoUtils.currentFieldToString(pis));
                    }
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in proto: " + ProtoUtils.currentFieldToString(pis));
                }
            }
        }

        private static EnergyConsumerAttribution unpackEnergyConsumerAttributionProto(ProtoInputStream pis) throws IOException {
            EnergyConsumerAttribution energyConsumerAttribution = new EnergyConsumerAttribution();
            while (true) {
                try {
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in EnergyConsumerAttributionProto: " + ProtoUtils.currentFieldToString(pis));
                }
                switch (pis.nextField()) {
                    case -1:
                        return energyConsumerAttribution;
                    case 0:
                    default:
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in EnergyConsumerAttributionProto: " + ProtoUtils.currentFieldToString(pis));
                        continue;
                    case 1:
                        energyConsumerAttribution.uid = pis.readInt((long) CompanionMessage.MESSAGE_ID);
                        continue;
                    case 2:
                        energyConsumerAttribution.energyUWs = pis.readLong(1112396529666L);
                        continue;
                }
                Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in EnergyConsumerAttributionProto: " + ProtoUtils.currentFieldToString(pis));
            }
        }

        private static EnergyConsumerResult unpackEnergyConsumerResultProto(ProtoInputStream pis) throws IOException {
            EnergyConsumerResult energyConsumerResult = new EnergyConsumerResult();
            List<EnergyConsumerAttribution> energyConsumerAttributionList = new ArrayList<>();
            while (true) {
                try {
                } catch (WireTypeMismatchException e) {
                    Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in EnergyConsumerResultProto: " + ProtoUtils.currentFieldToString(pis));
                }
                switch (pis.nextField()) {
                    case -1:
                        energyConsumerResult.attribution = (EnergyConsumerAttribution[]) energyConsumerAttributionList.toArray(new EnergyConsumerAttribution[energyConsumerAttributionList.size()]);
                        return energyConsumerResult;
                    case 0:
                    default:
                        Slog.e(ProtoStreamUtils.TAG, "Unhandled field in EnergyConsumerResultProto: " + ProtoUtils.currentFieldToString(pis));
                        continue;
                    case 1:
                        energyConsumerResult.id = pis.readInt((long) CompanionMessage.MESSAGE_ID);
                        continue;
                    case 2:
                        energyConsumerResult.timestampMs = pis.readLong(1112396529666L);
                        continue;
                    case 3:
                        energyConsumerResult.energyUWs = pis.readLong(1112396529667L);
                        continue;
                    case 4:
                        long token = pis.start(2246267895812L);
                        energyConsumerAttributionList.add(unpackEnergyConsumerAttributionProto(pis));
                        pis.end(token);
                        continue;
                }
                Slog.e(ProtoStreamUtils.TAG, "Wire Type mismatch in EnergyConsumerResultProto: " + ProtoUtils.currentFieldToString(pis));
            }
        }

        public static void print(EnergyConsumerResult[] energyConsumerResult) {
            if (energyConsumerResult == null) {
                return;
            }
            for (EnergyConsumerResult result : energyConsumerResult) {
                Slog.d(ProtoStreamUtils.TAG, "EnergyConsumerId: " + result.id + ", Timestamp (ms): " + result.timestampMs + ", Energy (uWs): " + result.energyUWs);
                int attributionLength = result.attribution.length;
                for (int j = 0; j < attributionLength; j++) {
                    EnergyConsumerAttribution attribution = result.attribution[j];
                    Slog.d(ProtoStreamUtils.TAG, "  UID: " + attribution.uid + "  Energy (uWs): " + attribution.energyUWs);
                }
            }
        }
    }
}
