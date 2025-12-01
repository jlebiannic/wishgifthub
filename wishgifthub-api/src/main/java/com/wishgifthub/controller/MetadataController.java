package com.wishgifthub.controller;

import com.wishgifthub.service.MetadataExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller pour l'extraction de métadonnées depuis des URLs
 */
@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    @Autowired
    private MetadataExtractionService metadataExtractionService;

    /**
     * Extrait les métadonnées d'une URL
     *
     * @param url L'URL à analyser
     * @return Les métadonnées extraites (title, description, image, price)
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> extractMetadata(@RequestParam String url) {
        try {
            Map<String, String> metadata = metadataExtractionService.extractMetadata(url);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            // En cas d'erreur, retourner un objet vide
            return ResponseEntity.ok(Map.of(
                "title", "",
                "description", "",
                "image", "",
                "price", "",
                "error", e.getMessage()
            ));
        }
    }
}

