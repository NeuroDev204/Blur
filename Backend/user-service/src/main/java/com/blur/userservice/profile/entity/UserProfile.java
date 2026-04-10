package com.blur.userservice.profile.entity;

import com.blur.userservice.profile.crypto.EncryptedLocalDateConverter;
import com.blur.userservice.profile.crypto.EncryptedStringConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Node("user_profile")// khai bao entity trong neo4j
public class UserProfile {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    @Property("user_id") // tuong nhu nhu column ben dbms khac
    String userId;
    String username;
    String passwordHash;
    String firstName;
    String lastName;
    String bio;
    String city;
    @ConvertWith(converter = EncryptedStringConverter.class)
    String phone;
    @ConvertWith(converter = EncryptedStringConverter.class)
    String email;
    String emailIndex; // blind index cho email lockup
    @ConvertWith(converter = EncryptedStringConverter.class)
    String gender;
    String website;
    String imageUrl;
    @ConvertWith(converter = EncryptedStringConverter.class)
    String address;
    LocalDate updatedAt;
    @ConvertWith(converter = EncryptedLocalDateConverter.class)
    LocalDate dob;
    LocalDate createdAt;
    LocalDate lastActiveAt;
    Integer followersCount;
    Integer followingCount;
    Integer postCount;
    Boolean verified;
    Boolean emailVerified;
    List<String> roles;

    @JsonIgnore
    @Relationship(type = "follows", direction = Relationship.Direction.OUTGOING)
    Set<UserProfile> following = new HashSet<>();

    @JsonIgnore
    @Relationship(type = "follows", direction = Relationship.Direction.INCOMING)
    Set<UserProfile> followers = new HashSet<>();
}
