package com.example.data.model

enum class ScanProcessingStage(val statusText: String) {
    IDLE(""),
    CAPTURED("Reading your sign…"),
    LOCAL_DETECTION("Finding the sign…"),
    CROP_CREATION("Reading your sign…"),
    LOCATION_RESOLUTION("Reading the parking rules…"),
    GEMINI_REQUEST("Checking the details…"),
    GEMINI_INTERPRETATION("Reading the parking rules…"),
    EVIDENCE_VALIDATION("Verifying the result…"),
    RESULT_VERIFICATION("Verifying the result…"),
    RESULT_SAVED("Verifying the result…"),
    COMPLETED(""),
    FAILED("Couldn't process the captured photo. Please try again.")
}
