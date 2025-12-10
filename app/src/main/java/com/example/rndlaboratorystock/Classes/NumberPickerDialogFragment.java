package com.example.rndlaboratorystock.Classes;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.rndlaboratorystock.R;

public class NumberPickerDialogFragment extends DialogFragment {

    public interface NumberSelectListener {
        void onNumberSelected(int value);
    }

    private int initial;
    private int min;
    private int max;
    private int step;
    private String title;

    private int currentValue;

    private NumberSelectListener listener;

    public NumberPickerDialogFragment(int initial, int min, int max, int step, String title, NumberSelectListener listener) {
        this.initial = initial;
        this.min = min;
        this.max = max;
        this.step = step;
        this.title = title;
        this.listener = listener;

        this.currentValue = Math.max(min, Math.min(initial, max));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_number_picker, null);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvValue = view.findViewById(R.id.tvValue);
        SeekBar seekBar = view.findViewById(R.id.seekBar);
        TextView tvMin = view.findViewById(R.id.tvMin);
        TextView tvMax = view.findViewById(R.id.tvMax);
        Button btnMinus = view.findViewById(R.id.btnMinus);
        Button btnPlus = view.findViewById(R.id.btnPlus);
        Button btnOk = view.findViewById(R.id.btnOk);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        tvTitle.setText(title);
        tvMin.setText(String.valueOf(min));
        tvMax.setText(String.valueOf(max));

        int steps = Math.max(1, (max - min) / step);
        seekBar.setMax(steps);

// SeekBar progress’ı: 0…steps
        int initialProgress = (currentValue - min) / step;
        seekBar.setProgress(initialProgress);
        tvValue.setText(String.valueOf(currentValue));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // -80..0 arası
                currentValue = min + (progress * step);

                if (currentValue < min) currentValue = min;
                if (currentValue > max) currentValue = max;

                tvValue.setText(String.valueOf(currentValue));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnMinus.setOnClickListener(v -> {
            currentValue = Math.max(min, currentValue - step);
            int newProgress = (currentValue - min) / step;
            seekBar.setProgress(newProgress);
            tvValue.setText(String.valueOf(currentValue));
        });

        btnPlus.setOnClickListener(v -> {
            currentValue = Math.min(max, currentValue + step);
            int newProgress = (currentValue - min) / step;
            seekBar.setProgress(newProgress);
            tvValue.setText(String.valueOf(currentValue));
        });

        btnOk.setOnClickListener(v -> {
            if (listener != null) listener.onNumberSelected(currentValue);
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }
}
