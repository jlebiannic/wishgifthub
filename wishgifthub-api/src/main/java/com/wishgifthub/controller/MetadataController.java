package com.wishgifthub.controller;

import com.wishgifthub.openapi.api.MetadataApi;
import com.wishgifthub.openapi.model.MetadataResponse;
import com.wishgifthub.service.MetadataExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * Controller pour l'extraction de métadonnées depuis des URLs
 */
@RestController
public class MetadataController implements MetadataApi {

    @Autowired
    private MetadataExtractionService metadataExtractionService;

    @Override
    public ResponseEntity<MetadataResponse> extractMetadata(URI url) {
        try {
            // Convertir l'URI en String pour le service
            Map<String, String> metadata = metadataExtractionService.extractMetadata(url.toString());

            MetadataResponse response = new MetadataResponse();
            response.setTitle(metadata.getOrDefault("title", ""));
            response.setDescription(metadata.getOrDefault("description", ""));
            response.setImage(metadata.getOrDefault("image", ""));
            response.setPrice(metadata.getOrDefault("price", ""));
            response.setError(null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // En cas d'erreur, retourner un objet avec le message d'erreur
            MetadataResponse response = new MetadataResponse();
            response.setTitle("");
            response.setDescription("");
            response.setImage("");
            response.setPrice("");
            response.setError(e.getMessage());

            return ResponseEntity.ok(response);
        }
    }
}

