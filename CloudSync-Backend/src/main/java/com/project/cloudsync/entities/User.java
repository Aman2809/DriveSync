package com.project.cloudsync.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name="users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    private String id;

    private String name;

    @Column(unique=true)
    private String email;

    @OneToMany(mappedBy = "user" , cascade = CascadeType.ALL)
    private List<CloudProvider> connectedProviders;
}
