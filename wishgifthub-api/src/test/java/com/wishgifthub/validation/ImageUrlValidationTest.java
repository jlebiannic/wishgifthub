package com.wishgifthub.validation;

import com.wishgifthub.openapi.model.WishRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class ImageUrlValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();
    private static final String CDN = "https://assets.katogroup.eu/i/katogroup/KZ1114A1_01_kizer";

    record StringImage(@ValidImageUrl String imageUrl) {}
    record UriImage(@ValidImageUrl URI imageUrl) {}

    @AfterAll
    static void closeValidator() {
        FACTORY.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            CDN, CDN + "?width=800&format=webp", "https://cdn.example.com/image.jpg",
            "http://cdn.example.com/render?id=123", "https://cdn.example.com/image.avif"
    })
    void acceptsHttpImagesWithOrWithoutExtensions(String url) {
        assertTrue(VALIDATOR.validate(new StringImage(url)).isEmpty());
        assertTrue(VALIDATOR.validate(new UriImage(URI.create(url))).isEmpty());
        assertTrue(VALIDATOR.validate(new WishRequest().giftName("Image CDN").imageUrl(URI.create(url))).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "cdn.example.com/image", "/relative/image", "https:/image",
            "ftp://cdn.example.com/image.jpg", "javascript:alert(1)", "data:image/png;base64,AA"
    })
    void rejectsInvalidProtocolsAndMissingHosts(String url) {
        assertFalse(VALIDATOR.validate(new StringImage(url)).isEmpty());
        assertFalse(VALIDATOR.validate(new UriImage(URI.create(url))).isEmpty());
    }

    @Test
    void preservesOptionalValuesAndLengthLimit() {
        assertTrue(VALIDATOR.validate(new StringImage(null)).isEmpty());
        assertTrue(VALIDATOR.validate(new StringImage("")).isEmpty());
        assertTrue(VALIDATOR.validate(new UriImage(null)).isEmpty());
        String maxLength = "https://cdn.example.com/" + "a".repeat(2048 - "https://cdn.example.com/".length());
        assertTrue(VALIDATOR.validate(new StringImage(maxLength)).isEmpty());
        assertTrue(VALIDATOR.validate(new UriImage(URI.create(maxLength))).isEmpty());
        assertFalse(VALIDATOR.validate(new StringImage(maxLength + "a")).isEmpty());
        assertFalse(VALIDATOR.validate(new UriImage(URI.create(maxLength + "a"))).isEmpty());
    }
}
