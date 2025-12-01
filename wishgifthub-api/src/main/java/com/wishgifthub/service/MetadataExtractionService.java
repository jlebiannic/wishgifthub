package com.wishgifthub.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service pour extraire les métadonnées d'une URL (OpenGraph, meta tags, etc.)
 */
@Service
public class MetadataExtractionService {

    /**
     * Extrait les métadonnées d'une URL
     */
    public Map<String, String> extractMetadata(String url) throws IOException {
        Map<String, String> metadata = new HashMap<>();

        // Télécharger et parser la page HTML
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        // Extraire le titre (priorité: og:title, puis title tag)
        String title = extractOpenGraphTag(doc, "og:title");
        if (title == null || title.isEmpty()) {
            Element titleElement = doc.selectFirst("title");
            title = titleElement != null ? titleElement.text() : "";
        }
        metadata.put("title", title);

        // Extraire la description (priorité: og:description, puis meta description)
        String description = extractOpenGraphTag(doc, "og:description");
        if (description == null || description.isEmpty()) {
            description = extractMetaTag(doc, "description");
        }
        if (description == null || description.isEmpty()) {
            description = extractMetaTag(doc, "twitter:description");
        }
        metadata.put("description", description != null ? description : "");

        // Extraire l'image (priorité: og:image, puis twitter:image)
        String image = extractOpenGraphTag(doc, "og:image");
        if (image == null || image.isEmpty()) {
            image = extractMetaTag(doc, "twitter:image");
        }
        if (image == null || image.isEmpty()) {
            // Chercher la première image dans le contenu
            Element imgElement = doc.selectFirst("img[src]");
            image = imgElement != null ? imgElement.attr("abs:src") : "";
        }
        metadata.put("image", image != null ? image : "");

        // Extraire le prix (recherche dans le contenu)
        String price = extractPrice(doc);
        metadata.put("price", price != null ? price : "");

        return metadata;
    }

    /**
     * Extrait un tag OpenGraph
     */
    private String extractOpenGraphTag(Document doc, String property) {
        Element element = doc.selectFirst("meta[property=" + property + "]");
        return element != null ? element.attr("content") : null;
    }

    /**
     * Extrait un meta tag standard
     */
    private String extractMetaTag(Document doc, String name) {
        Element element = doc.selectFirst("meta[name=" + name + "]");
        return element != null ? element.attr("content") : null;
    }

    /**
     * Tente d'extraire un prix depuis la page
     * Cherche des patterns communs de prix (€, EUR, $, USD, etc.)
     */
    private String extractPrice(Document doc) {
        // Chercher dans les meta tags spécifiques e-commerce
        String priceFromMeta = extractMetaTag(doc, "product:price:amount");
        if (priceFromMeta != null && !priceFromMeta.isEmpty()) {
            String currency = extractMetaTag(doc, "product:price:currency");
            return priceFromMeta + (currency != null ? " " + currency : "");
        }

        // Chercher dans les balises avec classes/attributs courants
        Element priceElement = doc.selectFirst("[class*=price], [itemprop=price], .price, .product-price");
        if (priceElement != null) {
            String priceText = priceElement.text();
            // Nettoyer et extraire le prix numérique
            return cleanPrice(priceText);
        }

        // Chercher dans le texte avec regex (patterns de prix communs)
        String bodyText = doc.body().text();
        String pricePattern = "(?:€|EUR|\\$|USD)\\s*([0-9]+[,.]?[0-9]*)|([0-9]+[,.]?[0-9]*)\\s*(?:€|EUR|\\$|USD)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(pricePattern);
        java.util.regex.Matcher matcher = pattern.matcher(bodyText);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * Nettoie et normalise un prix extrait
     */
    private String cleanPrice(String priceText) {
        if (priceText == null || priceText.isEmpty()) {
            return null;
        }

        // Garder seulement les chiffres, virgules, points et symboles de devise
        String cleaned = priceText.replaceAll("[^0-9.,€$£¥]", "").trim();

        return cleaned.isEmpty() ? null : cleaned;
    }
}

