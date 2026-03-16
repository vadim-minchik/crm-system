package com.studio.crm_system.service;

import com.studio.crm_system.config.SupabaseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class SupabaseStorageService implements FileStorageService, TemplateStorageService {

    private static final int MAX_DIMENSION = 1600;
    private static final float JPEG_QUALITY = 0.82f;
    private static final long MAX_OUTPUT_BYTES = 3 * 1024 * 1024;

    @Autowired
    private SupabaseProperties props;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadPassportPhoto(MultipartFile file, Long clientId) throws IOException {
        byte[] imageBytes = compressImage(file);

        String bucket = props.getStorage().getBucket();
        String path = "clients/" + clientId + "/" + UUID.randomUUID() + ".jpg";
        String uploadUrl = props.getUrl() + "/storage/v1/object/" + bucket + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + props.getServiceRoleKey());
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.set("x-upsert", "true");

        HttpEntity<byte[]> entity = new HttpEntity<>(imageBytes, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl, HttpMethod.POST, entity, String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return props.getUrl() + "/storage/v1/object/public/" + bucket + "/" + path;
        }

        throw new RuntimeException("Ошибка загрузки в Supabase Storage: " + response.getStatusCode());
    }

    /**
     * Загружает шаблон документа (Word .doc/.docx) в отдельную папку bucket: documents/templates/{id}_{filename}.
     * Без сжатия — файл передаётся как есть.
     */
    public String uploadTemplate(MultipartFile file, Long templateId) throws IOException {
        String bucket = props.getStorage().getBucket();
        String safeName = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "document.docx";
        String path = "documents/templates/" + templateId + "_" + safeName;
        String uploadUrl = props.getUrl() + "/storage/v1/object/" + bucket + "/" + path;

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            if (safeName.toLowerCase().endsWith(".docx")) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            else if (safeName.toLowerCase().endsWith(".doc")) contentType = "application/msword";
            else contentType = "application/octet-stream";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + props.getServiceRoleKey());
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("x-upsert", "true");

        byte[] bytes = file.getBytes();
        HttpEntity<byte[]> entity = new HttpEntity<>(bytes, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl, HttpMethod.POST, entity, String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return props.getUrl() + "/storage/v1/object/public/" + bucket + "/" + path;
        }
        throw new RuntimeException("Ошибка загрузки шаблона в Supabase Storage: " + response.getStatusCode());
    }

    /**
     * Скачивает файл из Storage по сохранённому public URL, используя служебный ключ.
     * Нужно, если бакет приватный и публичный URL отдаёт 403.
     */
    public byte[] downloadByStoredUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) return null;

        String bucket = props.getStorage().getBucket();
        String prefix = props.getUrl() + "/storage/v1/object/public/" + bucket + "/";
        if (!publicUrl.startsWith(prefix)) return null;

        String path = publicUrl.substring(prefix.length());
        String downloadUrl = props.getUrl() + "/storage/v1/object/" + bucket + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + props.getServiceRoleKey());
        headers.set("Accept", "application/octet-stream,*/*");

        ResponseEntity<byte[]> resp = restTemplate.exchange(
                downloadUrl, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null)
            return null;
        return resp.getBody();
    }

    public void deleteByUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) return;

        String bucket = props.getStorage().getBucket();
        String prefix = props.getUrl() + "/storage/v1/object/public/" + bucket + "/";
        if (!publicUrl.startsWith(prefix)) return;

        String path = publicUrl.substring(prefix.length());
        String deleteUrl = props.getUrl() + "/storage/v1/object/" + bucket + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + props.getServiceRoleKey());

        try {
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        } catch (Exception e) {
            System.err.println("[Supabase] Не удалось удалить файл: " + path);
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

        System.out.printf("[Supabase] Сжато: %.1f КБ → %.1f КБ%n",
                file.getSize() / 1024.0, result.length / 1024.0);

        return result;
    }

    private BufferedImage scaleDown(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return img;

        double ratio = (double) MAX_DIMENSION / Math.max(w, h);
        int newW = (int) (w * ratio);
        int newH = (int) (h * ratio);

        return resize(img, newW, newH);
    }

    private BufferedImage halfSize(BufferedImage img) {
        return resize(img, img.getWidth() / 2, img.getHeight() / 2);
    }

    private BufferedImage resize(BufferedImage img, int newW, int newH) {
        BufferedImage result = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, newW, newH);
        g.drawImage(img, 0, 0, newW, newH, null);
        g.dispose();
        return result;
    }

    private byte[] encodeJpeg(BufferedImage img, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
