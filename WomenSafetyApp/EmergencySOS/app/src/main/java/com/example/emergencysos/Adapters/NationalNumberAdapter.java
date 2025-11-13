package com.example.emergencysos.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencysos.Screens.Helpline;
import com.example.emergencysos.R;

import java.util.List;

public class NationalNumberAdapter extends RecyclerView.Adapter<NationalNumberAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(String phoneNumber);
    }

    private List<Helpline> helplineList;
    private OnItemClickListener listener;

    public NationalNumberAdapter(List<Helpline> helplineList, OnItemClickListener listener) {
        this.helplineList = helplineList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_helpline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Helpline helpline = helplineList.get(position);
        holder.tvName.setText(helpline.getName());
        holder.tvNumber.setText(helpline.getNumber());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(helpline.getNumber()));
    }

    @Override
    public int getItemCount() {
        return helplineList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvNumber;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvHelplineName);
            tvNumber = itemView.findViewById(R.id.tvHelplineNumber);
        }
    }
}
