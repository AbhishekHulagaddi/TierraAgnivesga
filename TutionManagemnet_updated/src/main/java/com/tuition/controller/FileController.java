package com.tuition.controller;

import com.tuition.model.*;
import com.tuition.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileController {

    @Value("${app.upload.dir:./uploads/notes}")
    private String uploadDir;

    private final NotesRepository notesRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    // ── UPLOAD ───────────────────────────────────────────
    @PostMapping("/api/tutor/notes/upload")
    public ResponseEntity<?> uploadNote(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("tutorId") Long tutorId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "chapter", required = false) String chapter) {

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Subject subject = subjectRepository.findById(subjectId).orElseThrow();
            User tutor = userRepository.findById(tutorId).orElseThrow();

            Notes notes = Notes.builder()
                .title(title).subject(subject).tutor(tutor)
                .description(description).chapter(chapter)
                .fileName(file.getOriginalFilename())
                .filePath(fileName).fileSize(file.getSize())
                .isActive(true).downloadCount(0)
                .build();

            notesRepository.save(notes);
            return ResponseEntity.ok(Map.of("message", "Notes uploaded successfully", "fileName", fileName));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    // ── DOWNLOAD (attachment — forces browser save dialog) ──
    @GetMapping("/api/tutor/notes/download/{noteId}")
    public ResponseEntity<Resource> downloadNote(@PathVariable Long noteId) {
        return serveNote(noteId, true);
    }

    // ── VIEW INLINE (for in-portal PDF reader — no download counter increment) ──
    @GetMapping("/api/notes/view/{noteId}")
    public ResponseEntity<Resource> viewNote(@PathVariable Long noteId) {
        return serveNote(noteId, false);
    }

    // ── SHARED SERVE LOGIC ───────────────────────────────
    private ResponseEntity<Resource> serveNote(Long noteId, boolean asAttachment) {
        Notes notes = notesRepository.findById(noteId).orElseThrow();
        try {
            Path filePath = Paths.get(uploadDir).resolve(notes.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) return ResponseEntity.notFound().build();

            if (asAttachment) {
                // Increment download counter only for actual downloads
                notes.setDownloadCount(notes.getDownloadCount() + 1);
                notesRepository.save(notes);
            }

            String disposition = asAttachment
                ? "attachment; filename=\"" + notes.getFileName() + "\""
                : "inline; filename=\"" + notes.getFileName() + "\"";

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.status(500).build();
        }
    }

    // ── DELETE ───────────────────────────────────────────
    @DeleteMapping("/api/tutor/notes/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id) {
        Notes notes = notesRepository.findById(id).orElseThrow();
        try {
            Path filePath = Paths.get(uploadDir).resolve(notes.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {}
        notesRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Notes deleted"));
    }
}
