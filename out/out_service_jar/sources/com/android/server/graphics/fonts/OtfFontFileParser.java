package com.android.server.graphics.fonts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontFileUtil;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import com.android.server.graphics.fonts.UpdatableFontDir;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DirectByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.NioUtils;
import java.nio.channels.FileChannel;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class OtfFontFileParser implements UpdatableFontDir.FontFileParser {
    @Override // com.android.server.graphics.fonts.UpdatableFontDir.FontFileParser
    public String getPostScriptName(File file) throws IOException {
        ByteBuffer buffer = mmap(file);
        try {
            return FontFileUtil.getPostScriptName(buffer, 0);
        } finally {
            unmap(buffer);
        }
    }

    @Override // com.android.server.graphics.fonts.UpdatableFontDir.FontFileParser
    public String buildFontFileName(File file) throws IOException {
        String extension;
        ByteBuffer buffer = mmap(file);
        try {
            String psName = FontFileUtil.getPostScriptName(buffer, 0);
            int isType1Font = FontFileUtil.isPostScriptType1Font(buffer, 0);
            int isCollection = FontFileUtil.isCollectionFont(buffer);
            if (!TextUtils.isEmpty(psName) && isType1Font != -1 && isCollection != -1) {
                if (isCollection == 1) {
                    extension = isType1Font == 1 ? ".otc" : ".ttc";
                } else {
                    extension = isType1Font == 1 ? ".otf" : ".ttf";
                }
                return psName + extension;
            }
            return null;
        } finally {
            unmap(buffer);
        }
    }

    @Override // com.android.server.graphics.fonts.UpdatableFontDir.FontFileParser
    public long getRevision(File file) throws IOException {
        ByteBuffer buffer = mmap(file);
        try {
            return FontFileUtil.getRevision(buffer, 0);
        } finally {
            unmap(buffer);
        }
    }

    @Override // com.android.server.graphics.fonts.UpdatableFontDir.FontFileParser
    public void tryToCreateTypeface(File file) throws Throwable {
        ByteBuffer buffer = mmap(file);
        try {
            Font font = new Font.Builder(buffer).build();
            FontFamily family = new FontFamily.Builder(font).build();
            Typeface typeface = new Typeface.CustomFallbackBuilder(family).build();
            TextPaint p = new TextPaint();
            p.setTextSize(24.0f);
            p.setTypeface(typeface);
            int width = (int) Math.ceil(Layout.getDesiredWidth("abcXYZ@- \u1fad6🇺🇸💏🏻👨🏼\u200d❤️\u200d💋\u200d👨🏿", p));
            StaticLayout layout = StaticLayout.Builder.obtain("abcXYZ@- \u1fad6🇺🇸💏🏻👨🏼\u200d❤️\u200d💋\u200d👨🏿", 0, "abcXYZ@- \u1fad6🇺🇸💏🏻👨🏼\u200d❤️\u200d💋\u200d👨🏿".length(), p, width).build();
            Bitmap bmp = Bitmap.createBitmap(layout.getWidth(), layout.getHeight(), Bitmap.Config.ALPHA_8);
            Canvas canvas = new Canvas(bmp);
            layout.draw(canvas);
        } finally {
            unmap(buffer);
        }
    }

    private static ByteBuffer mmap(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            FileChannel fileChannel = in.getChannel();
            MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0L, fileChannel.size());
            in.close();
            return map;
        } catch (Throwable th) {
            try {
                in.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void unmap(ByteBuffer buffer) {
        if (buffer instanceof DirectByteBuffer) {
            NioUtils.freeDirectBuffer(buffer);
        }
    }
}
