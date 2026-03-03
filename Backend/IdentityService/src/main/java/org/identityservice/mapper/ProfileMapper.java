package org.identityservice.mapper;

import org.identityservice.dto.request.ProfileCreationRequest;
import org.identityservice.dto.request.UserCreationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProfileMapper {
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "city", ignore = true)
    ProfileCreationRequest toProfileCreationRequest(UserCreationRequest userCreationRequest);
}
