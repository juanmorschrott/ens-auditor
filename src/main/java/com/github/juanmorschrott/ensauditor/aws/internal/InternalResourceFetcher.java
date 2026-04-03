package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.ResourceType;

/**
 * Internal interface for resource type-specific fetchers.
 * Implementations are auto-discovered and routed by ResourceType.
 */
public interface InternalResourceFetcher {

    /**
     * Gets the resource type that this fetcher handles.
     *
     * @return the resource type
     */
    ResourceType getResourceType();

    /**
     * Gets the name of this fetcher for logging and identification.
     *
     * @return the fetcher name
     */
    String getName();
}
