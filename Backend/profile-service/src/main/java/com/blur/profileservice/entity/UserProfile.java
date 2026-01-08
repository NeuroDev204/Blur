package com.blur.profileservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDate;
import java.util.HashSet;
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
    String firstName;
    String lastName;
    String bio;
    String city;
    String phone;
    String email;
    String gender;
    String website;
    String imageUrl;
    String address;
    LocalDate updatedAt;
    LocalDate dob;
    LocalDate createdAt;
    @Relationship(type = "follows",direction = Relationship.Direction.OUTGOING)
    @JsonIgnore
    Set<UserProfile> following = new HashSet<>();

    @Relationship(type = "follows",direction = Relationship.Direction.INCOMING)
    @JsonIgnore
    Set<UserProfile> followers = new HashSet<>();
}
