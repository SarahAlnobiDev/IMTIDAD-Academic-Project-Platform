package model;

/** Lightweight metadata for a project attachment stored on disk and referenced by Oracle. */
public record ProjectAttachmentInfo(String fileName, String filePath, String fileType) {
    public boolean isImage() {
        if (fileType == null) return false;
        String normalized = fileType.trim().toUpperCase();
        return normalized.equals("PNG") || normalized.equals("JPG") || normalized.equals("JPEG");
    }

    public boolean isPdf() {
        return fileType != null && fileType.trim().equalsIgnoreCase("PDF");
    }
}
