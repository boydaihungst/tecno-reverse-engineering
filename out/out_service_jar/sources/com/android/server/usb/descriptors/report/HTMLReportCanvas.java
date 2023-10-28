package com.android.server.usb.descriptors.report;

import com.android.server.usb.descriptors.UsbDescriptorParser;
/* loaded from: classes2.dex */
public final class HTMLReportCanvas extends ReportCanvas {
    private static final String TAG = "HTMLReportCanvas";
    private final StringBuilder mStringBuilder;

    public HTMLReportCanvas(UsbDescriptorParser parser, StringBuilder stringBuilder) {
        super(parser);
        this.mStringBuilder = stringBuilder;
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void write(String text) {
        this.mStringBuilder.append(text);
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void openHeader(int level) {
        this.mStringBuilder.append("<h").append(level).append('>');
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void closeHeader(int level) {
        this.mStringBuilder.append("</h").append(level).append('>');
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void openParagraph(boolean emphasis) {
        if (emphasis) {
            this.mStringBuilder.append("<p style=\"color:red\">");
        } else {
            this.mStringBuilder.append("<p>");
        }
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void closeParagraph() {
        this.mStringBuilder.append("</p>");
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void writeParagraph(String text, boolean inRed) {
        openParagraph(inRed);
        this.mStringBuilder.append(text);
        closeParagraph();
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void openList() {
        this.mStringBuilder.append("<ul>");
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void closeList() {
        this.mStringBuilder.append("</ul>");
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void openListItem() {
        this.mStringBuilder.append("<li>");
    }

    @Override // com.android.server.usb.descriptors.report.ReportCanvas
    public void closeListItem() {
        this.mStringBuilder.append("</li>");
    }
}
