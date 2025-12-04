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

        // Télécharger et parser la page HTML avec des headers plus complets pour éviter les blocages
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .referrer("https://www.google.com/")
                .timeout(15000)
                .followRedirects(true)
                .get();

        // Extraire le titre (priorité: og:title, puis title tag, puis sélecteurs spécifiques)
        String title = extractOpenGraphTag(doc, "og:title");
        if (title == null || title.isEmpty() || title.equals("Amazon.fr")) {
            // Essayer les sélecteurs spécifiques Amazon
            Element amazonTitle = doc.selectFirst("#productTitle, h1.product-title, #title");
            if (amazonTitle != null) {
                title = amazonTitle.text().trim();
            }
        }
        if (title == null || title.isEmpty() || title.equals("Amazon.fr")) {
            Element titleElement = doc.selectFirst("title");
            title = titleElement != null ? titleElement.text() : "";
            // Nettoyer le titre Amazon (enlever " : Amazon.fr" à la fin)
            if (title.contains(" : Amazon")) {
                title = title.substring(0, title.indexOf(" : Amazon")).trim();
            }
        }
        metadata.put("title", title);

        // Extraire la description (priorité: og:description, puis meta description, puis contenu de la page)
        String description = extractOpenGraphTag(doc, "og:description");
        if (description == null || description.isEmpty()) {
            description = extractMetaTag(doc, "description");
        }
        if (description == null || description.isEmpty()) {
            description = extractMetaTag(doc, "twitter:description");
        }
        if (description == null || description.isEmpty()) {
            // Essayer les sélecteurs spécifiques Amazon
            Element amazonDesc = doc.selectFirst("#feature-bullets, #productDescription, .product-description, [data-feature-name='featurebullets']");
            if (amazonDesc != null) {
                description = amazonDesc.text().trim();
                // Limiter la longueur de la description
                if (description.length() > 500) {
                    description = description.substring(0, 497) + "...";
                }
            }
        }
        metadata.put("description", description != null ? description : "");

        // Extraire l'image (priorité: og:image, puis twitter:image, puis sélecteurs spécifiques)
        String image = extractOpenGraphTag(doc, "og:image");
        if (image == null || image.isEmpty()) {
            image = extractMetaTag(doc, "twitter:image");
        }
        if (image == null || image.isEmpty()) {
            image = extractProductImage(doc);
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
     * Extrait l'image d'un produit en cherchant dans les sélecteurs courants des sites e-commerce
     */
    private String extractProductImage(Document doc) {
        // 1. Essayer les sélecteurs Amazon spécifiques avec data-old-hires (meilleure qualité)
        String[] amazonSelectorsWithData = {
            "#landingImage",
            "#imgBlkFront",
            "#main-image",
            ".a-dynamic-image"
        };

        for (String selector : amazonSelectorsWithData) {
            Element amazonImg = doc.selectFirst(selector);
            if (amazonImg != null) {
                // Amazon peut stocker l'URL de la grande image dans data-old-hires
                String dataHires = amazonImg.attr("data-old-hires");
                if (dataHires != null && !dataHires.isEmpty() && isValidImageUrl(dataHires)) {
                    return dataHires;
                }

                // Ou dans data-a-dynamic-image (JSON avec plusieurs URLs)
                String dataDynamic = amazonImg.attr("data-a-dynamic-image");
                if (dataDynamic != null && !dataDynamic.isEmpty()) {
                    try {
                        String firstUrl = dataDynamic.substring(dataDynamic.indexOf("\"") + 1, dataDynamic.indexOf("\"", dataDynamic.indexOf("\"") + 1));
                        if (firstUrl.startsWith("http") && isValidImageUrl(firstUrl)) {
                            return firstUrl;
                        }
                    } catch (Exception e) {
                        // Ignorer les erreurs de parsing
                    }
                }

                // Fallback sur l'attribut src
                String src = amazonImg.attr("abs:src");
                if (src != null && !src.isEmpty() && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // Chercher dans les sélecteurs courants des sites e-commerce
        String[] selectors = {
            "img.product-image",
            "img[itemprop=image]",
            ".product-main-image img",
            ".product-image-wrapper img",
            "#product-image",
            ".gallery-image img",
            "[data-testid=product-image]",
            "img[class*=product]",
            "img[class*=Product]"
        };

        for (String selector : selectors) {
            Element img = doc.selectFirst(selector);
            if (img != null) {
                // Chercher d'abord dans data-src (lazy loading)
                String dataSrc = img.attr("data-src");
                if (dataSrc != null && !dataSrc.isEmpty() && dataSrc.startsWith("http") && isValidImageUrl(dataSrc)) {
                    return dataSrc;
                }

                // Puis dans src
                String src = img.attr("abs:src");
                if (src != null && !src.isEmpty() && !src.contains("placeholder") && !src.contains("loading") && isValidImageUrl(src)) {
                    return src;
                }
            }
        }

        // Fallback : chercher la première image qui semble être une image de produit (pas trop petite)
        for (Element img : doc.select("img[src]")) {
            String src = img.attr("abs:src");
            String width = img.attr("width");
            String height = img.attr("height");

            // Ignorer les petites images (icônes, logos, etc.)
            if (src != null && !src.isEmpty() && !src.contains("logo") && !src.contains("icon") && isValidImageUrl(src)) {
                try {
                    int w = width.isEmpty() ? 0 : Integer.parseInt(width);
                    int h = height.isEmpty() ? 0 : Integer.parseInt(height);

                    // Prendre l'image si elle est assez grande ou si on n'a pas les dimensions
                    if ((w == 0 && h == 0) || (w > 100 && h > 100)) {
                        return src;
                    }
                } catch (NumberFormatException e) {
                    // Si on ne peut pas parser les dimensions, on prend l'image
                    return src;
                }
            }
        }

        return null;
    }

    /**
     * Vérifie qu'une URL se termine par une extension d'image valide
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Exclure les URLs de tracking, analytics, CSI, etc.
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("/oc-csi/") ||
            lowerUrl.contains("/analytics") ||
            lowerUrl.contains("/track") ||
            lowerUrl.contains("/beacon") ||
            lowerUrl.contains("/pixel") ||
            lowerUrl.contains("data:image") ||
            lowerUrl.startsWith("data:")) {
            return false;
        }

        // Enlever les paramètres de requête pour vérifier l'extension
        String urlWithoutParams = url.split("\\?")[0];

        // Extensions d'images valides
        String[] validExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp", ".ico", ".avif"};

        for (String ext : validExtensions) {
            if (urlWithoutParams.toLowerCase().endsWith(ext)) {
                return true;
            }
        }

        return false;
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

        // Sélecteurs spécifiques Amazon
        String[] amazonSelectors = {
            ".a-price .a-offscreen",           // Prix principal Amazon
            "#priceblock_ourprice",             // Ancien sélecteur Amazon
            "#priceblock_dealprice",            // Prix en promotion
            ".a-price-whole",                   // Partie entière du prix
            "span.priceToPay",                  // Nouveau format Amazon
            ".a-section.a-spacing-small .a-price .a-offscreen",  // Prix dans la section prix
            "#corePrice_feature_div .a-offscreen",  // Nouvelle structure Amazon
            "[data-a-color=price] .a-offscreen"
        };

        for (String selector : amazonSelectors) {
            Element priceElement = doc.selectFirst(selector);
            if (priceElement != null) {
                String priceText = priceElement.text();
                if (priceText != null && !priceText.isEmpty()) {
                    String cleaned = cleanPrice(priceText);
                    if (cleaned != null && !cleaned.isEmpty()) {
                        return cleaned;
                    }
                }
            }
        }

        // Chercher dans les balises avec itemprop=price (standard Schema.org)
        Element itemPropPrice = doc.selectFirst("[itemprop=price]");
        if (itemPropPrice != null) {
            String content = itemPropPrice.attr("content");
            if (content != null && !content.isEmpty()) {
                String currency = extractMetaTag(doc, "priceCurrency");
                if (currency == null) {
                    Element currencyElement = doc.selectFirst("[itemprop=priceCurrency]");
                    currency = currencyElement != null ? currencyElement.attr("content") : null;
                }
                return content + (currency != null ? " " + currency : "");
            }

            String priceText = itemPropPrice.text();
            if (priceText != null && !priceText.isEmpty()) {
                return cleanPrice(priceText);
            }
        }

        // Chercher dans les balises avec classes/attributs courants
        String[] genericSelectors = {
            ".price",
            ".product-price",
            "[class*=price]",
            "[data-price]",
            ".sale-price",
            ".current-price"
        };

        for (String selector : genericSelectors) {
            Element priceElement = doc.selectFirst(selector);
            if (priceElement != null) {
                String priceText = priceElement.text();
                String cleaned = cleanPrice(priceText);
                if (cleaned != null && !cleaned.isEmpty()) {
                    return cleaned;
                }
            }
        }

        // Chercher dans le texte avec regex (patterns de prix communs)
        String bodyText = doc.body().text();
        String pricePattern = "(?:€|EUR|\\$|USD)\\s*([0-9]+[,.]?[0-9]*)|([0-9]+[,.]?[0-9]*)\\s*(?:€|EUR|\\$|USD)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(pricePattern);
        java.util.regex.Matcher matcher = pattern.matcher(bodyText);
        if (matcher.find()) {
            return cleanPrice(matcher.group());
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

        // Supprimer les espaces invisibles et normaliser
        priceText = priceText.trim().replaceAll("\\s+", " ");

        // Garder seulement les chiffres, virgules, points, espaces et symboles de devise
        String cleaned = priceText.replaceAll("[^0-9.,€$£¥\\s]", "").trim();

        // Si le prix ne contient pas de symbole de devise, essayer de le trouver dans le texte original
        if (!cleaned.matches(".*[€$£¥].*")) {
            if (priceText.contains("EUR") || priceText.contains("€")) {
                cleaned += " €";
            } else if (priceText.contains("USD") || priceText.contains("$")) {
                cleaned += " $";
            } else if (priceText.contains("GBP") || priceText.contains("£")) {
                cleaned += " £";
            }
        }

        return cleaned.isEmpty() ? null : cleaned;
    }
}

