package com.api.chatstack.mappers;

public interface Mapper<D, E> {
    D toDto(E entity);
    E toEntity(D dto);
}
