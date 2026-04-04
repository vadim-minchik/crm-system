package com.studio.crm_system.storage;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


public final class PassportImageCompressor {

	private static final int MAX_DIMENSION = 1600;
	private static final float JPEG_QUALITY = 0.82f;
	private static final long MAX_OUTPUT_BYTES = 3 * 1024 * 1024;

	private PassportImageCompressor() {
	}

	public static byte[] compress(MultipartFile file) throws IOException {
		try (InputStream in = file.getInputStream()) {
			return compress(in, file.getOriginalFilename());
		}
	}

	public static byte[] compress(InputStream inputStream, String originalFilename) throws IOException {
		BufferedImage original = ImageIO.read(inputStream);
		if (original == null) {
			throw new IOException("Не удалось прочитать изображение: " + originalFilename);
		}
		original = toRgbBufferedImage(original);
		BufferedImage scaled = scaleDown(original);
		byte[] result = encodeJpeg(scaled, JPEG_QUALITY);
		if (result.length > MAX_OUTPUT_BYTES) {
			result = encodeJpeg(scaled, 0.65f);
		}
		if (result.length > MAX_OUTPUT_BYTES) {
			scaled = halfSize(scaled);
			result = encodeJpeg(scaled, 0.65f);
		}
		return result;
	}

	private static BufferedImage toRgbBufferedImage(BufferedImage src) {
		int w = src.getWidth();
		int h = src.getHeight();
		BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rgb.createGraphics();
		try {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, w, h);
			g.drawImage(src, 0, 0, null);
		} finally {
			g.dispose();
		}
		return rgb;
	}

	private static BufferedImage scaleDown(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return img;
		double ratio = (double) MAX_DIMENSION / Math.max(w, h);
		return resize(img, (int) (w * ratio), (int) (h * ratio));
	}

	private static BufferedImage halfSize(BufferedImage img) {
		return resize(img, img.getWidth() / 2, img.getHeight() / 2);
	}

	private static BufferedImage resize(BufferedImage img, int newW, int newH) {
		BufferedImage result = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = result.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, newW, newH);
		g.drawImage(img, 0, 0, newW, newH, null);
		g.dispose();
		return result;
	}

	private static byte[] encodeJpeg(BufferedImage img, float quality) throws IOException {
		if (img.getType() != BufferedImage.TYPE_INT_RGB) {
			img = toRgbBufferedImage(img);
		}
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ImageWriteParam param = writer.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(quality);
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
			writer.setOutput(ios);
			writer.write(null, new IIOImage(img, null, null), param);
		} finally {
			writer.dispose();
		}
		return baos.toByteArray();
	}
}
