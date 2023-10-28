package com.lucid.propertiesapi;

import java.io.IOException;
import java.security.InvalidParameterException;
/* loaded from: classes2.dex */
public interface PowerXtendInternalInterface {
    long GetConfigXmlsRevision() throws IllegalStateException, UnsatisfiedLinkError;

    boolean GetControlPanelStatus() throws InvalidParameterException;

    String GetExclusionList() throws InvalidParameterException;

    int GetICEEngineOverrideParameter() throws InvalidParameterException;

    String GetIrisEngineOverrideFSParameter() throws InvalidParameterException;

    String GetIrisEngineOverrideGPParameter() throws InvalidParameterException;

    int GetLogLevel() throws InvalidParameterException;

    int GetPSEngineOverrideParameter() throws InvalidParameterException, IllegalStateException;

    long GetPowerXtendRevision() throws IllegalStateException, UnsatisfiedLinkError;

    int GetPowerXtendTouchEnabled() throws InvalidParameterException;

    int GetPowerXtendTouchPsParam() throws InvalidParameterException;

    void SetControlPanelStatus(boolean z) throws IOException;

    void SetExclusionList(String str) throws IOException;

    void SetICEEngineOverrideParameter(int i) throws IOException, InvalidParameterException;

    void SetIrisEngineOverrideFSParameter(String str) throws IOException, InvalidParameterException;

    void SetIrisEngineOverrideGPParameter(String str) throws IOException, InvalidParameterException;

    void SetLogLevel(int i) throws IOException;

    void SetPSEngineOverrideParameter(int i) throws IOException, InvalidParameterException;

    void SetPowerXtendTouchToOff() throws IOException;

    void SetPowerXtendTouchToOn(int i) throws IOException, InvalidParameterException;

    boolean ValidateXmls(String str) throws IllegalStateException, UnsatisfiedLinkError;

    int getLib32OR64() throws IllegalStateException, UnsatisfiedLinkError;

    String getPermission(String str) throws IllegalStateException, UnsatisfiedLinkError;
}
