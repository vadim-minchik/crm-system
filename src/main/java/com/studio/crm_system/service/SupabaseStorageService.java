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
public class SupabaseStorageService {

    /** Максимальная сторона (пикселей) после масштабирования */
    private static final int MAX_DIMENSION = 1600;
    /** Качество JPEG (0.0–1.0) */
    private static final float JPEG_QUALITY = 0.82f;
    /** Максимальный размер уже обработанного файла (байты) — 3 МБ */
    private static final long MAX_OUTPUT_BYTES = 3 * 1024 * 1024;

    @Autowired
    private SupabaseProperties props;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Сжимает изображение и загружает в Supabase Storage.
     * Возвращает публичный URL файла.
     */
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
     * Удаляет файл из Supabase Storage по публичному URL.
     */
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

    // ─── Сжатие ──────────────────────────────────────────────────────────────

    private byte[] compressImage(MultipartFile file) throws IOException {
        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new IOException("Не удалось прочитать изображение: " + file.getOriginalFilename());
        }

        BufferedImage scaled = scaleDown(original);

        // Первая попытка — выбранное качество
        byte[] result = encodeJpeg(scaled, JPEG_QUALITY);

        // Если всё ещё слишком большой — снижаем качество до 0.65
        if (result.length > MAX_OUTPUT_BYTES) {
            result = encodeJpeg(scaled, 0.65f);
        }

        // Крайний случай — дополнительно уменьшаем размер и сжимаем
        if (result.length > MAX_OUTPUT_BYTES) {
            scaled = halfSize(scaled);
            result = encodeJpeg(scaled, 0.65f);
        }

        System.out.printf("[Supabase] Сжато: %.1f КБ → %.1f КБ%n",
                file.getSize() / 1024.0, result.length / 1024.0);

        return result;
    }

    /** Масштабирует изображение чтобы обе стороны не превышали MAX_DIMENSION */
    private BufferedImage scaleDown(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return img;

        double ratio = (double) MAX_DIMENSION / Math.max(w, h);
        int newW = (int) (w * ratio);
        int newH = (int) (h * ratio);

        return resize(img, newW, newH);
    }

    /** Уменьшает изображение вдвое */
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
