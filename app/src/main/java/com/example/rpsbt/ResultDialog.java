package com.example.rpsbt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class ResultDialog extends AppCompatDialogFragment {

    private int result;
    private int hand;
    private int opponentHand;

    public ResultDialog() {

    }

    public void setVariables(int result, int hand, int opponentHand) {
        this.result = result;
        this.hand = hand;
        this.opponentHand = opponentHand;
    }

    @Nullable
    ResultDialog.DialogListener dialogListener = null;

    interface DialogListener {
        void onPositiveButtonPress();
    }

    public void setDialogListener(@Nullable ResultDialog.DialogListener dialogListener) {
            this.dialogListener = dialogListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.result_dialog, null);

        builder.setView(view)
                .setNegativeButton("Return", (DialogInterface dialog, int which) -> dialogListener.onPositiveButtonPress());

        LinearLayout resultDialog = view.findViewById(R.id.resultDialog);
        ImageView resultImage = view.findViewById(R.id.result);
        ImageView handImage = view.findViewById(R.id.hand);
        ImageView opponentHandImage = view.findViewById(R.id.opponentHand);

        switch(result) {
            case 1:
                resultImage.setBackgroundResource(R.drawable.tie);
                resultDialog.setBackgroundColor(Color.parseColor("#b6b7b5"));
                break;
            case 2:
                resultImage.setBackgroundResource(R.drawable.win);
                resultDialog.setBackgroundColor(Color.parseColor("#9dcf87"));
                break;
            case 3:
                resultImage.setBackgroundResource(R.drawable.lose);
                resultDialog.setBackgroundColor(Color.parseColor("#CF8787"));
                break;
        }

        switch(hand) {
            case 1:
                handImage.setBackgroundResource(R.drawable.rock);
                break;
            case 2:
                handImage.setBackgroundResource(R.drawable.paper);
                break;
            case 3:
                handImage.setBackgroundResource(R.drawable.scissors);
                break;
        }

        switch(opponentHand) {
            case 1:
                opponentHandImage.setBackgroundResource(R.drawable.rock);
                break;
            case 2:
                opponentHandImage.setBackgroundResource(R.drawable.paper);
                break;
            case 3:
                opponentHandImage.setBackgroundResource(R.drawable.scissors);
                break;
        }

        return builder.create();
    }
}
