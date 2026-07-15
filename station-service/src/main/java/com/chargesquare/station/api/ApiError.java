package com.chargesquare.station.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stable JSON error response returned for expected API failures.")
public record ApiError(String error, String message) {
}
