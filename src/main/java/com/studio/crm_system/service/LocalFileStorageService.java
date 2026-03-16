package com.studio.crm_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Хранение фото паспорта на диске сервера (папка uploads/).
 * В БД сохраняется путь вида /uploads/clients/{clientId}_{uuid}.jpg — отдаётся как статика.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

	private static final int MAX_DIMENSION = 1600;
	private static final float JPEG_QUALITY = 0.82f;
	private static final long MAX_OUTPUT_BYTES = 3 * 1024 * 1024;
	private static final String URL_PREFIX = "/uploads/";

	@Value("${file.upload-dir:uploads}")
	private String uploadDir;

	@Override
	public String uploadPassportPhoto(MultipartFile file, Long clientId) throws IOException {
		byte[] imageBytes = compressImage(file);
		Path base = Paths.get(uploadDir).toAbsolutePath();
		Path clientsDir = base.resolve("clients");
		Files.createDirectories(clientsDir);

		String filename = clientId + "_" + UUID.randomUUID() + ".jpg";
		Path target = clientsDir.resolve(filename);
		Files.copy(new ByteArrayInputStream(imageBytes), target, StandardCopyOption.REPLACE_EXISTING);

		return URL_PREFIX + "clients/" + filename;
	}

	@Override
	public void deleteByUrl(String urlOrPath) {
		if (urlOrPath == null || !urlOrPath.startsWith(URL_PREFIX)) return;
		String relative = urlOrPath.substring(URL_PREFIX.length());
		Path full = Paths.get(uploadDir).toAbsolutePath().resolve(relative);
		try {
			Files.deleteIfExists(full);
		} catch (IOException e) {
			System.err.println("[LocalStorage] Не удалось удалить файл: " + full + " — " + e.getMessage());
		}
	}

	private byte[] compressImage(MultipartFile file) throws IOException {
		BufferedImage original = ImageIO.read(file.getInputStream());
		if (original == null) {
			throw new IOException("Не удалось прочитать изображение: " + file.getOriginalFilename());
		}
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

	private BufferedImage scaleDown(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return img;
		double ratio = (double) MAX_DIMENSION / Math.max(w, h);
		return resize(img, (int) (w * ratio), (int) (h * ratio));
	}

	private BufferedImage halfSize(BufferedImage img) {
		return resize(img, img.getWidth() / 2, img.getHeight() / 2);
	}

	private BufferedImage resize(BufferedImage img, int newW, int newH) {
		BufferedImage result = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = result.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, newW, newH);
		g.drawImage(img, 0, 0, newW, newH, null);
		g.dispose();
		return result;
	}

	private byte[] encodeJpeg(BufferedImage img, float quality) throws IOException {
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
