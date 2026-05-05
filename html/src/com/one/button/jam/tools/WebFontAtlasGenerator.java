package com.one.button.jam.tools;

import java.io.File;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeBitmapFontData;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter.FontInfo;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter.OutputFormat;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter.Padding;
import com.badlogic.gdx.utils.GdxNativesLoader;

public final class WebFontAtlasGenerator {
    private WebFontAtlasGenerator() {
    }

    private static String characters() {
        StringBuilder builder = new StringBuilder();
        for (int id = 32; id <= 255; id++) {
            if (!Character.isISOControl(id)) {
                builder.append((char) id);
            }
        }
        return builder.toString();
    }

    private static void generate(String ttfPath, String outDir, String baseName, int fontSize, int pad, int pageSize)
            throws Exception {
        PixmapPacker packer = new PixmapPacker(pageSize, pageSize, Format.RGBA8888, pad, false);
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(new FileHandle(new File(ttfPath)));
        try {
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = fontSize;
            parameter.characters = characters();
            parameter.genMipMaps = false;
            parameter.minFilter = TextureFilter.Linear;
            parameter.magFilter = TextureFilter.Linear;
            parameter.kerning = true;
            parameter.packer = packer;

            FreeTypeBitmapFontData data = generator.generateData(parameter);
            writeFont(outDir, baseName, fontSize, pad, pageSize, data, packer);
        } finally {
            generator.dispose();
            packer.dispose();
        }
    }

    private static void writeFont(String outDir, String baseName, int fontSize, int pad, int pageSize, BitmapFontData data,
            PixmapPacker packer) {
        FileHandle outputDir = new FileHandle(new File(outDir));
        outputDir.mkdirs();

        BitmapFontWriter.setOutputFormat(OutputFormat.Text);
        String[] pageRefs = BitmapFontWriter.writePixmaps(packer.getPages(), outputDir, baseName);

        FontInfo fontInfo = new FontInfo(data.name, fontSize);
        fontInfo.padding = new Padding(pad, pad, pad, pad);
        fontInfo.smooth = true;
        fontInfo.aa = 1;
        fontInfo.unicode = true;
        fontInfo.overrideMetrics(data);

        FileHandle fontFile = outputDir.child(baseName + ".fnt");
        BitmapFontWriter.writeFont(data, pageRefs, fontFile, fontInfo, pageSize, pageSize);

        System.out.println("Generated " + fontFile.file().getAbsolutePath());
        for (String pageRef : pageRefs) {
            System.out.println("Generated " + outputDir.child(pageRef).file().getAbsolutePath());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println("Usage: <calibriTtf> <harlowTtf> <outDir> <referenceWidth> <density> <pageSize>");
            System.exit(1);
        }

        GdxNativesLoader.load();

        String calibriTtf = args[0];
        String harlowTtf = args[1];
        String outDir = args[2];
        int referenceWidth = Integer.parseInt(args[3]);
        int density = Integer.parseInt(args[4]);
        int pageSize = Integer.parseInt(args[5]);
        int pad = Math.max(4, density * 3);

        generate(calibriTtf, outDir, "web_font1_hd", Math.round(referenceWidth / 26f * density), pad, pageSize);
        generate(harlowTtf, outDir, "web_title_hd", Math.round(referenceWidth / 12f * density), pad, pageSize);
        generate(harlowTtf, outDir, "web_restart_hd", Math.round(referenceWidth / 10f * density), pad, pageSize);
    }
}
