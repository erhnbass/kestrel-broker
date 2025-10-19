package com.erhan.kestrel_broker.domain.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "asset", uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "asset_name"}))
@Getter
@Setter
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(nullable = false)
    private long size;

    @Column(name = "usable_size", nullable = false)
    private long usableSize;

    @Version
    private long version;
}
