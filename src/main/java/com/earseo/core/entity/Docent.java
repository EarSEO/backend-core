package com.earseo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Docent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false, unique = true)
    private String contentId;

    @Column(name = "script", columnDefinition = "TEXT")
    private String script;

    @Column(name = "docent_url")
    private String docentUrl;
}
