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
 * <p>Mo bang {@link ConsentCmp#show} hoac {@link ConsentCmp#showIfNeeded}. Viec tai cau hinh, hien
 * loi + thu lai, kiem tra bat buoc va gui du lieu deu do {@link ConsentFormView} lam.</p>
 */
public class ConsentSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "cmp_consent_sheet";

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
        formView = view.findViewById(R.id.cmp_form);

        formView.setOnLoadListener(new ConsentFormView.OnLoadListener() {
            @Override
            public void onLoaded(ConsentConfig config) {
                if (isAdded() && !config.isActive()) {
                    // Form inactive: khong hien UI, coi nhu khong can hoi nguoi dung.
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onLoadFailed(ConsentException error) {
                notifyError(error);
            }
        });

        // Khong dat OnActionListener: nut "Dong y" / "Tu choi tat ca" cua form tu kiem tra va gui.
        formView.setOnSubmitResultListener(new ConsentFormView.OnSubmitResultListener() {
            @Override
            public void onSubmitted(ConsentState state, SendConsentResult result) {
                completed = true;
                ConsentListener listener = ConsentCmp.get().listener();
                if (listener != null) {
                    listener.onCompleted(state, result);
                }
                if (isAdded()) {
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onSubmitFailed(ConsentException error) {
                notifyError(error);
            }
        });

        // Luon lay ban moi nhat khi mo man hinh consent: cau hinh co the vua doi tren portal.
        // Mat mang thi SDK tu rot ve ban cache cu.
        formView.reload();
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
        if (!ConsentCmp.isInitialized()) {
            return;
        }
        ConsentListener listener = ConsentCmp.get().listener();
        if (listener != null) {
            listener.onError(error);
        }
    }
}
