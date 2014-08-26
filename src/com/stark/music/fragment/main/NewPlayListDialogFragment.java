package com.stark.music.fragment.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.stark.music.R;
import com.stark.util.Dip2Px;

public class NewPlayListDialogFragment extends DialogFragment {
	AlertDialog dialog;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater mInflater = LayoutInflater.from(getActivity());
		View view = mInflater.inflate(R.layout.new_playlist_dialog, null);
		dialog = new AlertDialog.Builder(getActivity()).setCancelable(true)
				.create();
		dialog.show();
		dialog.setContentView(R.layout.new_playlist_dialog);
		setDialogAttr();
		return dialog;
	}

	/**
	 * 设置dialog属性
	 */
	private void setDialogAttr() {
		View view2 = LayoutInflater.from(getActivity()).inflate(
				R.layout.new_playlist_dialog, null);
		Window window = dialog.getWindow();
		/** 弹出软键盘 **/
		window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		/** 防止Activity挤变形 **/
		getActivity().getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
						| WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
		/** 设置dialog宽度 **/
		WindowManager.LayoutParams lp = window.getAttributes();
		lp.width = Dip2Px.dip2px(getActivity(), 300);
		window.setAttributes(lp);

	}

}
