package com.github.juanmorschrott.ensauditor.compliance;

import java.util.List;
import java.util.Optional;

/**
 * Public API for the ENS control registry.
 * Manages the ENS control registry and provides access to control definitions.
 */
public interface ControlRegistry {
    /**
     * Gets all controls in the registry.
     * @return list of all control definitions
     */
    List<ControlDefinition> getAllControls();

    /**
     * Gets a control by its ID.
     * @param controlId the control identifier (e.g. "C2.1")
     * @return the control definition, or empty if not found
     */
    Optional<ControlDefinition> getControlById(String controlId);
}
