package com.android.server.usb.descriptors.report;

import com.android.server.usb.descriptors.UsbDescriptorParser;
/* loaded from: classes2.dex */
public final class TextReportCanvas extends ReportCanvas {
    private static final int LIST_INDENT_AMNT = 2;
    private static final String TAG = "TextReportCanvas";
    private int mListIndent;
    private final StringBuilder mStringBuilder;

    public TextReportCanvas(UsbDescriptorParser parser, StringBuilder stringBuilder) {
        super(parser);
        this.mStringBuilder = stringBuilder;
    }

    private void writeListIndent() {
        for (int space = 0; space < this.mListIndent; space++) {
            this.mStringBuilder.append(" ");
        }
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void write(String text) {
        this.mStringBuilder.append(text);
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void openHeader(int level) {
        writeListIndent();
        this.mStringBuilder.append("[");
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void closeHeader(int level) {
        this.mStringBuilder.append("]\n");
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void openParagraph(boolean emphasis) {
        writeListIndent();
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void closeParagraph() {
        this.mStringBuilder.append("\n");
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void writeParagraph(String text, boolean inRed) {
        openParagraph(inRed);
        if (inRed) {
            this.mStringBuilder.append("*" + text + "*");
        } else {
            this.mStringBuilder.append(text);
        }
        closeParagraph();
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void openList() {
        this.mListIndent += 2;
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void closeList() {
        this.mListIndent -= 2;
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void openListItem() {
        writeListIndent();
        this.mStringBuilder.append("- ");
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void closeListItem() {
        this.mStringBuilder.append("\n");
    }
}
