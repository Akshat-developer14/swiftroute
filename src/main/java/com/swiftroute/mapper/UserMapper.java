package com.swiftroute.mapper;

import com.swiftroute.dto.request.RegisterRequest;
import com.swiftroute.dto.response.UserResponse;
import com.swiftroute.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "urls", ignore = true)
    User toEntity(RegisterRequest request);
}
