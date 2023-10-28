package com.lucid.propertiesapi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
/* loaded from: classes2.dex */
public interface PowerXtendControlAPI {
    void CleanGlobalConfiguration() throws IOException;

    void GetGameXtendConfig(ModulePropertiesInfo modulePropertiesInfo) throws FileNotFoundException;

    int GetLogLevel() throws InvalidParameterException, IllegalStateException, IOException;

    void GetNavXtendConfig(ModulePropertiesInfo modulePropertiesInfo) throws FileNotFoundException;

    Triggers GetPowerXtendAutoModeTriggers() throws FileNotFoundException, IllegalStateException, IOException, InvalidParameterException;

    int GetPowerXtendCurrentState() throws FileNotFoundException, IllegalStateException, InvalidParameterException;

    int GetPowerXtendMode() throws FileNotFoundException, IllegalStateException, IOException, InvalidParameterException;

    long GetPowerXtendRevision() throws IllegalStateException, UnsatisfiedLinkError;

    void GetSocialXtendConfig(ModulePropertiesInfo modulePropertiesInfo) throws FileNotFoundException;

    void GetWebXtendConfig(ModulePropertiesInfo modulePropertiesInfo) throws FileNotFoundException;

    boolean IsPowerXtendControlPanelInstalled() throws IllegalStateException;

    void ResetPowerXtendAutoModeTriggers() throws FileNotFoundException, IllegalStateException, IOException;

    void SetGameXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException;

    void SetGameXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException;

    void SetGameXtendToOff() throws FileNotFoundException, IllegalStateException;

    void SetLogLevel(int i) throws IllegalStateException, InvalidParameterException, IOException;

    void SetNavXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException;

    void SetNavXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException;

    void SetNavXtendToOff() throws FileNotFoundException, IllegalStateException;

    void SetPowerXtendAutoModeTriggers(Triggers triggers) throws InvalidParameterException, FileNotFoundException, IllegalStateException, IOException;

    void SetPowerXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException, IOException;

    void SetPowerXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException, IOException;

    void SetPowerXtendToOff() throws FileNotFoundException, IllegalStateException, IOException;

    void SetSocialXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException;

    void SetSocialXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException;

    void SetSocialXtendToOff() throws FileNotFoundException, IllegalStateException;

    void SetWebXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException;

    void SetWebXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException;

    void SetWebXtendToOff() throws FileNotFoundException, IllegalStateException;
}
