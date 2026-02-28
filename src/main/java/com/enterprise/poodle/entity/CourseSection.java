package com.enterprise.poodle.entity;

import com.enterprise.poodle.enums.ContentType;
import com.enterprise.poodle.enums.QcmType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_sections", indexes = {
        @Index(name = "idx_section_course", columnList = "course_id"),
        @Index(name = "idx_section_order", columnList = "course_id, order_index")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType contentType;

    @Column(columnDefinition = "TEXT")
    private String contentUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @Enumerated(EnumType.STRING)
    private QcmType qcmType;

    private Integer maxAttempts;

    @Column(nullable = false)
    @Builder.Default
    private boolean llmDraftEnabled = false;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QCMQuestion> questions = new ArrayList<>();
}
