package com.example.codebase;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Fragment for the final step ("Created") of the create-event wizard.
 *
 * <p>Displays the event name and the QR code {@link android.graphics.Bitmap} generated
 * by {@code CreateEventActivity#generateQr(String)} after a successful Firestore write.
 * The organiser can also share the QR code image to other apps via the system share sheet.
 *
 * <p>For private events (US 02.01.02), no QR code is generated. In this case the QR
 * image, hint text, and share button are hidden, and a message is shown explaining
 * that private events do not have a promotional QR code.
 *
 * <p>UI population is deferred to {@link #onResume()} rather than
 * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} because
 * {@link CreateEventViewModel#generatedQr} is set asynchronously after the Firestore
 * operation completes and the ViewPager advances to this step; by the time
 * {@code onResume()} runs the bitmap is guaranteed to be available.
 *
 * @see CreateEventActivity
 * @see CreateEventViewModel
 */
public class CreateQrFragment extends Fragment {

    /** Shared ViewModel providing the generated QR bitmap and event metadata. */
    private CreateEventViewModel viewModel;

    /**
     * Inflates the fragment layout and obtains the shared {@link CreateEventViewModel}.
     * No view binding or data population is performed here; see {@link #onResume()}.
     *
     * @param inflater           The {@link LayoutInflater} used to inflate the view.
     * @param container          The parent {@link ViewGroup}, or {@code null}.
     * @param savedInstanceState Previously saved state, or {@code null}.
     * @return The inflated root {@link View} for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_qr, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);
        return view;
    }

    /**
     * Populates the UI with the event name and either the QR code or a private-event
     * message, depending on whether a QR code was generated.
     *
     * <p><b>Public event</b> ({@link CreateEventViewModel#generatedQr} is non-null):
     * <ul>
     *   <li>{@code tvEventNameDisplay} – set to {@link CreateEventViewModel#name}.</li>
     *   <li>{@code ivQrCode} – set to the generated {@link android.graphics.Bitmap}.</li>
     *   <li>{@code tvQrHint} – shows "Scan to view event details".</li>
     *   <li>{@code btnShare} – visible and wired to {@link #shareQrCode()}.</li>
     *   <li>{@code tvPrivateNotice} – hidden.</li>
     * </ul>
     *
     * <p><b>Private event</b> (US 02.01.02 — {@link CreateEventViewModel#generatedQr}
     * is {@code null}):
     * <ul>
     *   <li>{@code tvEventNameDisplay} – set to {@link CreateEventViewModel#name}.</li>
     *   <li>{@code ivQrCode} – hidden.</li>
     *   <li>{@code tvQrHint} – hidden.</li>
     *   <li>{@code btnShare} – hidden.</li>
     *   <li>{@code tvPrivateNotice} – visible with explanation message.</li>
     * </ul>
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getView() == null) return;

        TextView tvEventNameDisplay = getView().findViewById(R.id.tvEventNameDisplay);
        ImageView ivQrCode          = getView().findViewById(R.id.ivQrCode);
        TextView tvQrHint           = getView().findViewById(R.id.tvQrHint);
        Button btnShare             = getView().findViewById(R.id.btnShare);
        TextView tvPrivateNotice    = getView().findViewById(R.id.tvPrivateNotice);

        // Always show the event name
        tvEventNameDisplay.setText(viewModel.name);

        if (viewModel.generatedQr != null) {
            // Public event — show QR code normally
            ivQrCode.setImageBitmap(viewModel.generatedQr);
            ivQrCode.setVisibility(View.VISIBLE);
            tvQrHint.setVisibility(View.VISIBLE);
            btnShare.setVisibility(View.VISIBLE);
            tvPrivateNotice.setVisibility(View.GONE);

            btnShare.setOnClickListener(v -> shareQrCode());
        } else {
            // Private event (US 02.01.02) — no QR code generated
            ivQrCode.setVisibility(View.GONE);
            tvQrHint.setVisibility(View.GONE);
            btnShare.setVisibility(View.GONE);
            tvPrivateNotice.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Saves the QR code bitmap to the app's internal cache directory and launches
     * the system share sheet so the organiser can send it to another app.
     *
     * <p>The method performs the following steps:
     * <ol>
     *   <li>Creates (or reuses) the {@code <cacheDir>/images/} directory.</li>
     *   <li>Writes {@link CreateEventViewModel#generatedQr} as a lossless PNG file
     *       named {@code qr_<eventId>.png}.</li>
     *   <li>Obtains a {@link Uri} for the file via {@link FileProvider}, using the
     *       authority {@code <packageName>.fileprovider}.</li>
     *   <li>Fires an {@link Intent#ACTION_SEND} intent with
     *       {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and MIME type
     *       {@code image/png}, wrapped in a chooser titled "Share Event QR".</li>
     * </ol>
     *
     * <p>Any exception during file I/O or intent dispatch is caught, logged to the
     * error stream, and reported to the user with a short toast.
     */
    private void shareQrCode() {
        try {
            File cachePath = new File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "qr_" + viewModel.generatedEventId + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            viewModel.generatedQr.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);

            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(shareIntent, "Share Event QR"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error sharing QR code", Toast.LENGTH_SHORT).show();
        }
    }
}