/**
 * Compliance module for ENS control evaluation and registry.
 *
 * This module is the core domain layer responsible for evaluating ENS controls
 * against cloud resources and managing the control registry. It contains
 * evaluators for different resource types (S3, RDS, IAM) and coordinates
 * their execution.
 *
 * Published API:
 * - ComplianceService: Main service for running evaluations
 * - ControlRegistry: Service for accessing ENS control definitions
 */
package com.github.juanmorschrott.ensauditor.compliance;
