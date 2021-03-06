package im.tny.segvault.disturbances.ui.fragment;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import im.tny.segvault.disturbances.InternalLinkHandler;
import im.tny.segvault.disturbances.OurHtmlHttpImageGetter;
import im.tny.segvault.disturbances.R;
import im.tny.segvault.disturbances.ui.util.RichTextUtils;

/**
 * Created by gabriel on 7/12/17.
 */
public class HtmlDialogFragment extends DialogFragment {
    private static final String ARG_CONTENT = "content";
    private static final String ARG_HTML = "html";

    public static HtmlDialogFragment newInstance(String content, boolean isHtml) {
        HtmlDialogFragment fragment = new HtmlDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONTENT, content);
        args.putBoolean(ARG_HTML, isHtml);
        fragment.setArguments(args);
        return fragment;
    }

    public static HtmlDialogFragment newInstance(String content) {
        return newInstance(content, true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String content = "";
        boolean isHtml = false;
        if (getArguments() != null) {
            content = getArguments().getString(ARG_CONTENT);
            content = content == null ? "" : content;
            isHtml = getArguments().getBoolean(ARG_HTML);
        }
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_html, null);

        HtmlTextView htmltv = view.findViewById(R.id.html_view);
        if (isHtml) {
            htmltv.setHtml(content, new OurHtmlHttpImageGetter(htmltv, null, OurHtmlHttpImageGetter.ParentFitType.FIT_PARENT_WIDTH));
            htmltv.setText(RichTextUtils.replaceAll((Spanned) htmltv.getText(), URLSpan.class, new RichTextUtils.URLSpanConverter(), new InternalLinkHandler(getContext())));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                htmltv.setTextAppearance(R.style.TextAppearance_AppCompat_Small);
            } else {
                htmltv.setTextAppearance(getContext(), R.style.TextAppearance_AppCompat_Small);
            }
            htmltv.setText(content);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        try {
            return super.show(transaction, tag);
        } catch (IllegalStateException e) {
            // ignore state loss
            return -1;
        }
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            super.show(manager, tag);
        } catch (IllegalStateException e) {
            // ignore state loss
        }
    }
}
