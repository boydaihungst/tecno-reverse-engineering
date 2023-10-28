package com.lucid.propertiesapi;

import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
/* loaded from: classes2.dex */
public interface LucidPropertiesAPI {
    void GetGameXtendConfig(ModulePropertiesInfo modulePropertiesInfo) throws FileNotFoundException;

    void GetNavXtendConfig(ModulePropertiesInfo modulePropertiesInfo) throws FileNotFoundException;

    boolean GetPowerXtendConfig(ModulePropertiesInfo modulePropertiesInfo) throws FileNotFoundException;

    long GetPowerXtendRevision() throws IllegalStateException, UnsatisfiedLinkError;

    void GetSocialXtendConfig(ModulePropertiesInfo modulePropertiesInfo) throws FileNotFoundException;

    void GetWebXtendConfig(ModulePropertiesInfo modulePropertiesInfo) throws FileNotFoundException;

    void SetGameXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException;

    void SetGameXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException;

    void SetGameXtendToOff() throws FileNotFoundException, IllegalStateException;

    void SetNavXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException;

    void SetNavXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException;

    void SetNavXtendToOff() throws FileNotFoundException, IllegalStateException;

    void SetPowerXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException;

    void SetPowerXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException;

    void SetPowerXtendToOff() throws FileNotFoundException, IllegalStateException;

    void SetSocialXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException;

    void SetSocialXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException;

    void SetSocialXtendToOff() throws FileNotFoundException, IllegalStateException;

    void SetWebXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException;

    void SetWebXtendToManualMode(int i) throws FileNotFoundException, InvalidParameterException, IllegalStateException;

    void SetWebXtendToOff() throws FileNotFoundException, IllegalStateException;

    int getState(String str) throws InvalidParameterException;

    boolean isInternalTestAppInstalled();
}
