package com.wishgifthub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wishes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wish {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "gift_name", nullable = false)
    private String giftName;

    @Column
    private String description;

    @Column
    private String url;

    @Column(name = "image_url")
    private String imageUrl;

    @Column
    private String price;

    @ManyToOne
    @JoinColumn(name = "reserved_by")
    private User reservedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
