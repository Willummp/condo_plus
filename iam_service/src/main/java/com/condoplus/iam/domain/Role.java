package com.condoplus.iam.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Role {
    @Id
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private TipoRole nome;
    @Column(length = 200)
    private String descricao;
}
