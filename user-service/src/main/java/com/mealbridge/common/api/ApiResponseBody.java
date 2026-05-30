package com.mealbridge.common.api;

public record ApiResponseBody<T>(int statusCode, String statusMessage, T result) {
}
