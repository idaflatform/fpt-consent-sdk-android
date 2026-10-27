package vn.fpt.fis.cmp.consent.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import vn.fpt.fis.cmp.consent.*;

/** Applies only to SDK-owned views. Resource tags preserve semantic colors sharing the same default. */
final class AppearanceRenderer {
    static String token(int id) {
        if (id == R.color.cmp_bg) return token("cmp_bg");
        if (id == R.color.cmp_surface) return token("cmp_surface");
        if (id == R.color.cmp_border) return token("cmp_border");
        if (id == R.color.cmp_divider) return token("cmp_divider");
        if (id == R.color.cmp_text_primary) return token("cmp_text_primary");
        if (id == R.color.cmp_text_secondary) return token("cmp_text_secondary");
        if (id == R.color.cmp_text_muted) return token("cmp_text_muted");
        if (id == R.color.cmp_primary) return token("cmp_primary");
        if (id == R.color.cmp_primary_pressed) return token("cmp_primary_pressed");
        if (id == R.color.cmp_primary_soft) return token("cmp_primary_soft");
        if (id == R.color.cmp_primary_border) return token("cmp_primary_border");
        if (id == R.color.cmp_field_border) return token("cmp_field_border");
        if (id == R.color.cmp_link) return token("cmp_link");
        if (id == R.color.cmp_link_alt) return token("cmp_link_alt");
        if (id == R.color.cmp_accent) return token("cmp_accent");
        if (id == R.color.cmp_error) return token("cmp_error");
        if (id == R.color.cmp_chip_bg) return token("cmp_chip_bg");
        if (id == R.color.cmp_chip_text) return token("cmp_chip_text");
        if (id == R.color.cmp_chip_border) return token("cmp_chip_border");
        if (id == R.color.cmp_switch_thumb_off) return token("cmp_switch_thumb_off");
        if (id == R.color.cmp_switch_track_off) return token("cmp_switch_track_off");
        return "";
    }
    static String backgroundName(int id) {
        if (id == R.drawable.cmp_bg_box) return "cmp_bg_box";
        if (id == R.drawable.cmp_bg_card) return "cmp_bg_card";
        if (id == R.drawable.cmp_bg_card_subtle) return "cmp_bg_card_subtle";
        if (id == R.drawable.cmp_bg_field_chip) return "cmp_bg_field_chip";
        if (id == R.drawable.cmp_bg_button_primary) return "cmp_bg_button_primary";
        if (id == R.drawable.cmp_bg_list_toggle) return "cmp_bg_list_toggle";
        if (id == R.drawable.cmp_bg_chip) return "cmp_bg_chip";
        if (id == R.drawable.cmp_bg_sheet) return "cmp_bg_sheet";
        return "";
    }
    static String token(String name) {
        switch (name) {
            case "cmp_bg": return "background";
            case "cmp_text_primary": return "textPrimary";
            case "cmp_text_secondary": return "textSecondary";
            case "cmp_text_muted": return "disabled";
            case "cmp_primary_soft": return "selectedBackground";
            case "cmp_primary_border": return "selectedBorder";
            case "cmp_field_border": return "fieldBorder";
            case "cmp_chip_bg": return "chipBackground";
            case "cmp_chip_border": return "chipBorder";
            case "cmp_chip_text": return "chipText";
            case "cmp_switch_thumb_off": return "switchThumbOff";
            case "cmp_switch_track_off": return "switchTrackOff";
            case "cmp_link_alt": return "linkAlt";
            case "cmp_divider": return "border";
            case "cmp_primary_pressed": return "primaryPressed";
            default: return name.replace("cmp_", "");
        }
    }
    private static String role(int id) {
        if (id == R.id.cmp_tv_purpose_subtitle) return "caption";
        if (id == R.id.cmp_tv_purpose_counter) return "caption";
        if (id == R.id.cmp_tv_purpose_description) return "caption";
        if (id == R.id.cmp_btn_show_more) return "button";
        if (id == R.id.cmp_tv_fields_count) return "caption";
        if (id == R.id.cmp_btn_select_all) return "button";
        if (id == R.id.cmp_form_btn_retry) return "button";
        if (id == R.id.cmp_tv_title) return "title";
        if (id == R.id.cmp_tv_description) return "caption";
        if (id == R.id.cmp_btn_toggle_list) return "button";
        if (id == R.id.cmp_btn_submit) return "button";
        return "body";
    }
    private static int color(View view, ConsentAppearance a, String name, int fallback) {
        int id = 0;
        if (name.equals("cmp_bg")) id = R.color.cmp_bg;
        if (name.equals("cmp_surface")) id = R.color.cmp_surface;
        if (name.equals("cmp_border")) id = R.color.cmp_border;
        if (name.equals("cmp_divider")) id = R.color.cmp_divider;
        if (name.equals("cmp_text_primary")) id = R.color.cmp_text_primary;
        if (name.equals("cmp_text_secondary")) id = R.color.cmp_text_secondary;
        if (name.equals("cmp_text_muted")) id = R.color.cmp_text_muted;
        if (name.equals("cmp_primary")) id = R.color.cmp_primary;
        if (name.equals("cmp_primary_pressed")) id = R.color.cmp_primary_pressed;
        if (name.equals("cmp_primary_soft")) id = R.color.cmp_primary_soft;
        if (name.equals("cmp_primary_border")) id = R.color.cmp_primary_border;
        if (name.equals("cmp_field_border")) id = R.color.cmp_field_border;
        if (name.equals("cmp_link")) id = R.color.cmp_link;
        if (name.equals("cmp_link_alt")) id = R.color.cmp_link_alt;
        if (name.equals("cmp_accent")) id = R.color.cmp_accent;
        if (name.equals("cmp_error")) id = R.color.cmp_error;
        if (name.equals("cmp_chip_bg")) id = R.color.cmp_chip_bg;
        if (name.equals("cmp_chip_text")) id = R.color.cmp_chip_text;
        if (name.equals("cmp_chip_border")) id = R.color.cmp_chip_border;
        if (name.equals("cmp_switch_thumb_off")) id = R.color.cmp_switch_thumb_off;
        if (name.equals("cmp_switch_track_off")) id = R.color.cmp_switch_track_off;
        return a.color(token(name), id == 0 ? fallback : ContextCompat.getColor(view.getContext(), id));
    }
    static void apply(View view, ConsentAppearance a, AppearanceRepository repository) {
        if (a.json().optInt("schemaVersion") != 1) return;
        String[] tags = view.getTag() instanceof String ? ((String) view.getTag()).split(":", -1) : new String[0];
        String bg = tags.length == 3 && tags[0].equals("cmp") ? tags[1] : "";
        String textColor = tags.length == 3 && tags[0].equals("cmp") ? tags[2] : "";
        Object override = view.getTag(R.id.cmp_appearance_background);
        if (override instanceof String) bg = (String) override;
        if (!bg.isEmpty()) background(view, a, bg);
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (!textColor.isEmpty()) text.setTextColor(color(view, a, textColor, text.getCurrentTextColor()));
            String role = role(view.getId());
            int weight = a.weight(role, text.getTypeface() != null && text.getTypeface().isBold() ? 700 : 400);
            if (repository != null) text.setTypeface(repository.typeface(a, weight));
            else text.setTypeface(Typeface.create("system".equals(a.family()) ? null : a.family(),
                    weight >= 600 ? Typeface.BOLD : Typeface.NORMAL));
            float sp = text.getTextSize() / view.getResources().getDisplayMetrics().scaledDensity;
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, a.size(role, sp));
            if (text instanceof CompoundButton && !(text instanceof SwitchMaterial)) {
                ((CompoundButton) text).setButtonTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
                        new int[]{color(view, a, "cmp_primary", 0xff146052), color(view, a, "cmp_field_border", 0xffd5dce5)}));
            }
        }
        if (view instanceof SwitchMaterial) {
            SwitchMaterial toggle = (SwitchMaterial) view;
            toggle.setUseMaterialThemeColors(false);
            toggle.setThumbTintList(ColorStateList.valueOf(color(view, a, "cmp_switch_thumb_off", -1)));
            toggle.setTrackTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
                    new int[]{color(view, a, "cmp_primary", 0xff146052), color(view, a, "cmp_switch_track_off", 0xffc4cfdc)}));
        }
        if (view instanceof ImageView) ((ImageView) view).setColorFilter(color(view, a, "cmp_primary", 0xff146052));
        if (view instanceof ProgressBar) ((ProgressBar) view).setIndeterminateTintList(ColorStateList.valueOf(color(view, a, "cmp_primary", 0xff146052)));
        if (view instanceof LinearLayout && ((LinearLayout) view).getDividerDrawable() != null)
            ((LinearLayout) view).getDividerDrawable().mutate().setTint(color(view, a, "cmp_border", 0xffe3e8ef));
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
            apply(((ViewGroup) view).getChildAt(i), a, repository);
    }
    private static GradientDrawable shape(View v, ConsentAppearance a, String fill, String border, String radius, float fallbackRadius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color(v, a, fill, 0xffffffff));
        if (border != null) d.setStroke(Math.max(1, Math.round(v.getResources().getDisplayMetrics().density)), color(v, a, border, 0xffe3e8ef));
        d.setCornerRadius(a.radius(radius, fallbackRadius) * v.getResources().getDisplayMetrics().density);
        return d;
    }
    private static void background(View v, ConsentAppearance a, String name) {
        switch (name) {
            case "cmp_bg_field_chip": {
                StateListDrawable d = new StateListDrawable();
                d.addState(new int[]{android.R.attr.state_checked}, shape(v, a, "cmp_primary_soft", "cmp_primary_border", "pill", 999));
                d.addState(new int[]{}, shape(v, a, "cmp_bg", "cmp_field_border", "pill", 999)); v.setBackground(d); break;
            }
            case "cmp_bg_card": case "cmp_bg_card_subtle": case "cmp_bg_box": case "cmp_bg_sheet":
                v.setBackground(shape(v, a, name.equals("cmp_bg_card_subtle") ? "cmp_surface" : "cmp_bg", "cmp_border", "cardRadius", 12)); break;
            case "cmp_bg_chip": v.setBackground(shape(v, a, "cmp_chip_bg", "cmp_chip_border", "chipRadius", 4)); break;
            case "cmp_bg_list_toggle": v.setBackground(shape(v, a, "cmp_surface", "cmp_border", "fieldRadius", 8)); break;
            case "cmp_bg_button_primary": {
                StateListDrawable d = new StateListDrawable();
                d.addState(new int[]{-android.R.attr.state_enabled}, shape(v, a, "cmp_switch_track_off", null, "fieldRadius", 8));
                d.addState(new int[]{android.R.attr.state_pressed}, shape(v, a, "cmp_primary_pressed", null, "fieldRadius", 8));
                d.addState(new int[]{}, shape(v, a, "cmp_primary", null, "fieldRadius", 8)); v.setBackground(d); break;
            }
            case "cmp_divider": v.setBackgroundColor(color(v, a, "cmp_divider", 0xffe3e8ef)); break;
            default: break;
        }
    }
}
