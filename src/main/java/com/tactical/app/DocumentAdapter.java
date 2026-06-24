package com.tactical.app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocViewHolder> {

    private List<Document> documents = new ArrayList<>();
    private int lockedPosition = -1; // Tracks the currently selected card
    private final TargetCommandListener commandListener;

    // Interface to communicate with MainActivity
    public interface TargetCommandListener {
        void onTargetLocked(Document document);
        void onTargetCleared();
        void onTargetOpened(Document document);
    }

    public DocumentAdapter(TargetCommandListener listener) {
        this.commandListener = listener;
    }

    public void setDocuments(List<Document> docs) {
        this.documents = docs;
        notifyDataSetChanged();
    }

    public void clearLock() {
        lockedPosition = -1;
        notifyDataSetChanged();
        commandListener.onTargetCleared();
    }

    @NonNull
    @Override
    public DocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new DocViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocViewHolder holder, int position) {
        Document doc = documents.get(position);

        holder.tvDocName.setText(doc.fileName);
        holder.tvDocCategory.setText(doc.category);
        
        // Format the LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        holder.tvDocDate.setText(doc.uploadedAt.format(formatter));

        // Shift color if this is the locked target
        if (lockedPosition == position) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#2E7D32")); // Tactical Green
        } else {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#1E1E1E")); // Default Dark
        }

        // Long Press Protocol
        holder.itemView.setOnLongClickListener(v -> {
            lockedPosition = holder.getAdapterPosition();
            notifyDataSetChanged(); // Refresh colors
            commandListener.onTargetLocked(doc);
            return true;
        });

        // Tap to clear lock
        holder.itemView.setOnClickListener(v -> {
            if (lockedPosition != -1) {
                clearLock();
            }
            else{
                commandListener.onTargetOpened(doc);
            }
        });
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static class DocViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvDocDate, tvDocName, tvDocCategory;

        public DocViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.docCard);
            tvDocDate = itemView.findViewById(R.id.tvDocDate);
            tvDocName = itemView.findViewById(R.id.tvDocName);
            tvDocCategory = itemView.findViewById(R.id.tvDocCategory);
        }
    }
}
