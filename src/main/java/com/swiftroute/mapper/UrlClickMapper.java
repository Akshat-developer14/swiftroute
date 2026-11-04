package com.swiftroute.mapper;

import com.swiftroute.dto.response.UrlClickResponse;
import com.swiftroute.entity.UrlClick;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UrlClickMapper {
    UrlClickResponse toResponse(UrlClick urlClick);

    List<UrlClickResponse> toResponseList(List<UrlClick> urlClicks);
}
