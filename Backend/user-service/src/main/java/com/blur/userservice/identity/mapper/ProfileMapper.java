package com.blur.userservice.identity.mapper;

import com.blur.userservice.identity.dto.request.UserCreationRequest;
import com.blur.userservice.profile.dto.request.ProfileCreationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProfileMapper {
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "city", ignore = true)
    ProfileCreationRequest toProfileCreationRequest(UserCreationRequest userCreationRequest);
}
