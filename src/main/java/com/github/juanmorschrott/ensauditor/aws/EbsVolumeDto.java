package com.github.juanmorschrott.ensauditor.aws;

public record EbsVolumeDto(String volumeId, boolean encrypted, String kmsKeyId) {}
