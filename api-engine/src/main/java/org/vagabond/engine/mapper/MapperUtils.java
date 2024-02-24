package org.vagabond.engine.mapper;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.vagabond.engine.crud.dto.PageResponse;

public class MapperUtils {
    private MapperUtils() {
        // DO NOTHING
    }

    public static final ModelMapper mapper = new ModelMapper();

    public static <T, U> U toDto(T data, Class<U> dtoClass) {
        return mapper.map(data, dtoClass);
    }

    public static <T, U> List<U> toList(List<T> datas, Class<U> dtoClass) {
        return datas.stream().map(data -> mapper.map(data, dtoClass)).toList();
    }

    public static <U> PageResponse toPage(PageResponse response, Class<U> dtoClass) {
        return new PageResponse(response.page(), response.totalPages(), response.totalElements(), response.max(),
                MapperUtils.toList(response.content(), dtoClass));
    }
}
