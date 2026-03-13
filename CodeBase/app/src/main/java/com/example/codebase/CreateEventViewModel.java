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
    public String startDate = "";
    public String endDate = "";
    public String registrationOpen = "";
    public String registrationDeadline = "";
    public int capacity = 0;

    // Step 2: Settings
    public int waitlistLimit = -1;
    public boolean geoRequired = false;

    // Step 3: QR Result
    public Bitmap generatedQr = null;
    public String generatedEventId = "";
    public boolean isEditMode = false;
    public String editingEventId = "";
}
