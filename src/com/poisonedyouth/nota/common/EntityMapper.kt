package com.poisonedyouth.nota.common

/**
 * Common interface for entity to DTO mapping
 */
interface EntityMapper<E, D> {
    fun toDto(entity: E): D
    fun fromDto(dto: D): E
}

/**
 * Base interface for entities that can be mapped to DTOs
 */
interface MappableEntity<D> {
    fun toDto(): D
}

/**
 * Base interface for DTOs that can be mapped from entities
 */
interface MappableDto<E> {
    fun toEntity(): E
}

/**
 * Utility functions for safe entity mapping
 */
object EntityMapperUtils {

    /**
     * Safely maps a nullable entity to a DTO
     */
    fun <E, D> mapNullable(entity: E?, mapper: (E) -> D): D? {
        return entity?.let(mapper)
    }

    /**
     * Maps a list of entities to DTOs
     */
    fun <E, D> mapList(entities: List<E>, mapper: (E) -> D): List<D> {
        return entities.map(mapper)
    }

    /**
     * Maps a list of entities to DTOs with null safety
     */
    fun <E, D> mapNullableList(entities: List<E>?, mapper: (E) -> D): List<D> {
        return entities?.map(mapper) ?: emptyList()
    }

    /**
     * Validates that required ID is not null during mapping
     */
    fun validateId(id: Long?, entityName: String): Long {
        return id ?: throw IllegalStateException("$entityName ID cannot be null during mapping")
    }
}
