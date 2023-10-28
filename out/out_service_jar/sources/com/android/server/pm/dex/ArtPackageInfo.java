package com.android.server.pm.dex;

import java.util.List;
/* loaded from: classes2.dex */
public class ArtPackageInfo {
    private final List<String> mCodePaths;
    private final List<String> mInstructionSets;
    private final String mOatDir;
    private final String mPackageName;

    public ArtPackageInfo(String packageName, List<String> instructionSets, List<String> codePaths, String oatDir) {
        this.mPackageName = packageName;
        this.mInstructionSets = instructionSets;
        this.mCodePaths = codePaths;
        this.mOatDir = oatDir;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public List<String> getInstructionSets() {
        return this.mInstructionSets;
    }

    public List<String> getCodePaths() {
        return this.mCodePaths;
    }

    public String getOatDir() {
        return this.mOatDir;
    }
}
