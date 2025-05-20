package com.project.cloudsync.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudProvider {

    @Id
    private String id;

    @ManyToOne
    private User user;

    private String providerName;

    private String providerUserId;

    private String accessToken;
}
