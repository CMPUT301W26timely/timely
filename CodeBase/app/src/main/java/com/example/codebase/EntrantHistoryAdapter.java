package com.example.codebase;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for entrant registration-history cards.
 */
public class EntrantHistoryAdapter extends RecyclerView.Adapter<EntrantHistoryAdapter.ViewHolder> {

    /**
     * Listener notified when a history card is tapped.
     */
    public interface OnHistoryClickListener {
        /**
         * Called when the user taps a history row.
         *
         * @param entry the tapped history entry
         */
        void onHistoryClick(EntrantHistoryHelper.HistoryEntry entry);
    }

    private final List<EntrantHistoryHelper.HistoryEntry> entries;
    private final OnHistoryClickListener listener;

    /** Formats event dates for the history list. */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /**
     * Creates the adapter.
     *
     * @param entries the history entries to render
     * @param listener callback for row taps
     */
    public EntrantHistoryAdapter(List<EntrantHistoryHelper.HistoryEntry> entries,
                                 OnHistoryClickListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    /**
     * ViewHolder for one history card.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView tvTitle;
        private final TextView tvDate;
        private final TextView tvLocation;
        private final TextView tvStatus;
        private final TextView tvSummary;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvTitle = itemView.findViewById(R.id.tvHistoryEventTitle);
            tvDate = itemView.findViewById(R.id.tvHistoryEventDate);
            tvLocation = itemView.findViewById(R.id.tvHistoryEventLocation);
            tvStatus = itemView.findViewById(R.id.tvHistoryEventStatus);
            tvSummary = itemView.findViewById(R.id.tvHistoryEventSummary);
        }

        /**
         * Binds one history entry to the row UI.
         *
         * @param entry the history entry to display
         */
        void bind(EntrantHistoryHelper.HistoryEntry entry) {
            Event event = entry.getEvent();
            android.content.Context context = itemView.getContext();

            String title = TextUtils.isEmpty(event.getTitle())
                    ? context.getString(R.string.untitled_event)
                    : event.getTitle();
            String location = TextUtils.isEmpty(event.getLocation())
                    ? context.getString(R.string.history_location_not_set)
                    : event.getLocation();
            String date = event.getStartDate() != null
                    ? dateFormat.format(event.getStartDate())
                    : context.getString(R.string.date_not_set);

            tvTitle.setText(title);
            tvLocation.setText(location);
            tvDate.setText(date);
            tvStatus.setText(entry.getStatus().getLabel());
            tvSummary.setText(resolveSummary(entry.getStatus()));
            applyStatusStyle(entry.getStatus());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryClick(entry);
                }
            });
        }

        private void applyStatusStyle(EntrantHistoryHelper.HistoryStatus status) {
            int badgeBackgroundRes;
            int badgeTextColorRes;
            int cardBackgroundColorRes = R.color.surface;
            int cardStrokeColorRes = R.color.border;
            int summaryTextColorRes = R.color.textPrimary;

            switch (status) {
                case ENROLLED:
                case SELECTED:
                    badgeBackgroundRes = R.drawable.bg_pill_green;
                    badgeTextColorRes = R.color.textPrimary;
                    break;
                case CANCELLED:
                    badgeBackgroundRes = R.drawable.bg_history_badge_cancelled;
                    badgeTextColorRes = R.color.historyCancelledText;
                    cardBackgroundColorRes = R.color.historyCancelledCardBg;
                    cardStrokeColorRes = R.color.historyCancelledBorder;
                    summaryTextColorRes = R.color.historyCancelledText;
                    break;
                case NOT_SELECTED:
                    badgeBackgroundRes = R.drawable.bg_history_badge_not_selected;
                    badgeTextColorRes = R.color.historyNotSelectedText;
                    cardBackgroundColorRes = R.color.historyNotSelectedCardBg;
                    cardStrokeColorRes = R.color.historyNotSelectedBorder;
                    summaryTextColorRes = R.color.historyNotSelectedText;
                    break;
                case REGISTERED:
                default:
                    badgeBackgroundRes = R.drawable.bg_pill_amber;
                    badgeTextColorRes = R.color.textPrimary;
                    break;
            }

            // Give finished negative outcomes stronger contrast than the default neutral cards.
            cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), cardBackgroundColorRes));
            cardView.setStrokeColor(
                    ContextCompat.getColor(itemView.getContext(), cardStrokeColorRes));
            tvStatus.setBackgroundResource(badgeBackgroundRes);
            tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), badgeTextColorRes));
            tvSummary.setTextColor(ContextCompat.getColor(itemView.getContext(), summaryTextColorRes));
        }

        private String resolveSummary(EntrantHistoryHelper.HistoryStatus status) {
            switch (status) {
                case ENROLLED:
                    return itemView.getContext().getString(R.string.history_summary_enrolled);
                case SELECTED:
                    return itemView.getContext().getString(R.string.history_summary_selected);
                case NOT_SELECTED:
                    return itemView.getContext().getString(R.string.history_summary_not_selected);
                case CANCELLED:
                    return itemView.getContext().getString(R.string.history_summary_cancelled);
                case REGISTERED:
                default:
                    return itemView.getContext().getString(R.string.history_summary_registered);
            }
        }
    }
}
