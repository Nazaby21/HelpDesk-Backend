package com.zaby.helpdesk.model;

import com.zaby.helpdesk.enumeration.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@NoArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends AuditableCustom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(length = 50)
    private String email;

    @Column(name = "phone_number",  length = 50)
    private String phoneNumber;

    @Column(length = 50)
    private String password;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
