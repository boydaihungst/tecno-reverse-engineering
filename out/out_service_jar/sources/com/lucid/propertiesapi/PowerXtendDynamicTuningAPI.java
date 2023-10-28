package com.lucid.propertiesapi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
/* loaded from: classes2.dex */
public interface PowerXtendDynamicTuningAPI {
    void CleanDTConfiguration() throws IOException;

    String GetAvailableIrisParams();

    ICEEngineConfigParams GetDynamicTuningICEEngineParamsPerApp(String str) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException;

    IRISEngineConfigParams GetDynamicTuningIrisEngineParamsPerApp(String str) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException;

    PSEngineConfigParams GetDynamicTuningPSEngineParamsPerApp(String str) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException;

    DTConfigParams GetDynamicTuningParamsPerApp(String str) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException;

    boolean GetDynamicTuningState() throws FileNotFoundException, IOException, IllegalArgumentException;

    boolean GetDynamicTuningStatePerApp(String str) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException;

    boolean IsPowerXtendApproved(String str) throws UnsatisfiedLinkError, InvalidParameterException;

    boolean IsPowerXtendCompatible(String str) throws UnsatisfiedLinkError, InvalidParameterException;

    void ResetDynamicTuningParamsPerApp(String str) throws InvalidParameterException, IOException;

    void SetDynamicTuningICEEngineParamsPerApp(String str, ICEEngineConfigParams iCEEngineConfigParams) throws InvalidParameterException, IOException;

    void SetDynamicTuningIRISEngineParamsPerApp(String str, IRISEngineConfigParams iRISEngineConfigParams) throws InvalidParameterException, IOException;

    void SetDynamicTuningIrisCurve(String str, String str2) throws IOException;

    void SetDynamicTuningPSEngineParamsPerApp(String str, PSEngineConfigParams pSEngineConfigParams) throws InvalidParameterException, IOException;

    void SetDynamicTuningParams(String str, PSEngineConfigParams pSEngineConfigParams, ICEEngineConfigParams iCEEngineConfigParams, IRISEngineConfigParams iRISEngineConfigParams) throws InvalidParameterException, IOException;

    void SetDynamicTuningState(Boolean bool) throws IOException;

    void SetDynamicTuningStatePerApp(String str, Boolean bool) throws InvalidParameterException, IOException;
}
