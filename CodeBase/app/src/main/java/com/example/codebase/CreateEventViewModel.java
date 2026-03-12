package com.example.codebase;

import android.graphics.Bitmap;
import androidx.lifecycle.ViewModel;

public class CreateEventViewModel extends ViewModel {
    // Step 0: Basics
    public String posterBase64 = "";
    public String name = "";
    public String description = "";
    public String location = "";
    public double price = 0.0;

    // Step 1: Schedule (Storing as strings for simplicity from the EditTexts)
    public String eventStart = "";
    public String eventEnd = "";
    public String regOpen = "";
    public String regClose = "";
    public int capacity = 0;

    // Step 2: Settings
    public int waitlistLimit = -1;
    public boolean geoRequired = false;

    // Step 3: QR Result
    public Bitmap generatedQr = null;
    public String generatedEventId = "";
}