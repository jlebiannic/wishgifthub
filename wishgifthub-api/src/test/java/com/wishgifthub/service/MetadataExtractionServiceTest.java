package com.wishgifthub.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataExtractionServiceTest {
    @Test
    void extractsProductImageWithoutExtensionWhileSkippingTrackingImages() throws Exception {
        String imageUrl = "https://assets.katogroup.eu/i/katogroup/KZ1114A1_01_kizer?width=800";
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/product", exchange -> {
            byte[] html = ("<html><head><title>Produit</title></head><body>"
                    + "<img class='product-image' src='https://cdn.example.com/pixel'>"
                    + "<img class='product-main-image' src='" + imageUrl + "'>"
                    + "</body></html>").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.length);
            try (var output = exchange.getResponseBody()) {
                output.write(html);
            }
        });
        server.start();
        try {
            var metadata = new MetadataExtractionService().extractMetadata(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/product");
            assertEquals(imageUrl, metadata.get("image"));
            assertEquals("Produit", metadata.get("title"));
        } finally {
            server.stop(0);
        }
    }
}
