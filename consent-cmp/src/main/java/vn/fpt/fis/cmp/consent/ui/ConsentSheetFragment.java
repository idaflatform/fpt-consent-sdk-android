package vn.fpt.fis.cmp.consent.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import vn.fpt.fis.cmp.consent.ConsentCallback;
import vn.fpt.fis.cmp.consent.ConsentCmp;
import vn.fpt.fis.cmp.consent.ConsentException;
import vn.fpt.fis.cmp.consent.ConsentListener;
import vn.fpt.fis.cmp.consent.ConsentState;
import vn.fpt.fis.cmp.consent.R;
import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.SendConsentResult;

/**
 * Bottom sheet hien danh sach su dong y: tu goi /config khi mo, tu goi /sendData khi nguoi dung luu.
 *
 * <p>Mo bang {@link ConsentCmp#show} hoac {@link ConsentCmp#showIfNeeded}.</p>
 */
public class ConsentSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "cmp_consent_sheet";

    private View loadingView;
    private View errorView;
    private android.widget.TextView errorText;
    private View scrollContent;
    private ConsentFormView formView;

    private boolean completed;

    public static void show(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return;
        }
        new ConsentSheetFragment().show(fragmentManager, TAG);
    }

    /** Theme rieng cua SDK de app khong bat buoc phai dung theme Material. */
    @Override
    public int getTheme() {
        return R.style.Cmp_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cmp_sheet_consent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadingView = view.findViewById(R.id.cmp_state_loading);
        errorView = view.findViewById(R.id.cmp_state_error);
        errorText = view.findViewById(R.id.cmp_tv_state_error);
        scrollContent = view.findViewById(R.id.cmp_scroll_content);
        formView = view.findViewById(R.id.cmp_form);

        view.findViewById(R.id.cmp_btn_retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadConfig();
            }
        });

        formView.setOnActionListener(new ConsentFormView.OnActionListener() {
            @Override
            public void onSubmit(ConsentState state) {
                submit(state);
            }

            @Override
            public void onRejectAll(ConsentState state) {
                // "Tu choi tat ca" cung la mot quyet dinh hop le -> van ghi nhan bang chung.
                submit(state);
            }
        });

        loadConfig();
    }

    private void loadConfig() {
        showState(loadingView);
        // Luon lay ban moi nhat khi mo man hinh consent: cau hinh co the vua doi tren portal.
        // Mat mang thi SDK tu rot ve ban cache cu.
        ConsentCmp.get().fetchConfig(new ConsentCallback<ConsentConfig>() {
            @Override
            public void onSuccess(ConsentConfig config) {
                if (!isAdded()) {
                    return;
                }
                if (!config.isActive()) {
                    // Form inactive: khong hien UI, coi nhu khong can hoi nguoi dung.
                    dismissAllowingStateLoss();
                    return;
                }
                // bind() tu gop lua chon cu theo cau hinh moi (bo khoa da bi go khoi config).
                formView.bind(config, ConsentCmp.get().savedState());
                showState(scrollContent);
            }

            @Override
            public void onError(ConsentException error) {
                if (!isAdded()) {
                    return;
                }
                errorText.setText(error.getMessage() != null
                        ? error.getMessage() : getString(R.string.cmp_error_generic));
                showState(errorView);
                notifyError(error);
            }
        });
    }

    private void submit(final ConsentState state) {
        formView.setSubmitting(true);
        ConsentCmp.get().submit(state, new ConsentCallback<SendConsentResult>() {
            @Override
            public void onSuccess(SendConsentResult result) {
                completed = true;
                ConsentListener listener = ConsentCmp.get().listener();
                if (listener != null) {
                    listener.onCompleted(state, result);
                }
                if (isAdded()) {
                    formView.setSubmitting(false);
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onError(ConsentException error) {
                if (isAdded()) {
                    formView.setSubmitting(false);
                    formView.setError(error.getMessage());
                }
                notifyError(error);
            }
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!completed && ConsentCmp.isInitialized()) {
            ConsentListener listener = ConsentCmp.get().listener();
            if (listener != null) {
                listener.onDismissed();
            }
        }
    }

    private void notifyError(ConsentException error) {
        ConsentListener listener = ConsentCmp.get().listener();
        if (listener != null) {
            listener.onError(error);
        }
    }

    private void showState(View visible) {
        loadingView.setVisibility(visible == loadingView ? View.VISIBLE : View.GONE);
        errorView.setVisibility(visible == errorView ? View.VISIBLE : View.GONE);
        scrollContent.setVisibility(visible == scrollContent ? View.VISIBLE : View.GONE);
    }
}
