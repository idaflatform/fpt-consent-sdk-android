package vn.fpt.fis.cmp.consent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.ConsentField;
import vn.fpt.fis.cmp.consent.model.ConsentItem;
import vn.fpt.fis.cmp.consent.model.ConsentUiConfig;

public class ConsentStateTest {

    private static final String PURPOSE = "p1";
    private static final String VISIBLE = "f_visible";
    private static final String HIDDEN = "f_hidden";

    /** Mot purpose: 1 truong hien, 1 truong an (display = false) duoc chia se cho he thong. */
    private static ConsentConfig config() {
        ConsentField visible = new ConsentField(VISIBLE, "full_name", "Ho ten", null,
                false, false, false, true);
        ConsentField hidden = new ConsentField(HIDDEN, "email", "Email", "EMAIL",
                false, false, true, false);
        ConsentItem item = new ConsentItem(PURPOSE, PURPOSE, "Marketing", null, false, false,
                null, null, null, Arrays.asList(visible, hidden), Collections.<String>emptyList());
        return new ConsentConfig("cp", "Form", "active", null,
                new ConsentUiConfig(null, null, null, Collections.singletonList(item)));
    }

    @Test
    public void copy_isIndependentOfOriginal() {
        ConsentConfig config = config();
        ConsentState state = ConsentState.defaultsOf(config);
        state.applyAcceptAll(config);
        state.setFieldValue(PURPOSE, VISIBLE, "Le Van A");

        ConsentState copy = state.copy();
        state.setGranted(PURPOSE, false);
        state.setFieldValue(PURPOSE, VISIBLE, "doi sau khi copy");

        assertTrue(copy.isGranted(PURPOSE));
        assertTrue(copy.isFieldGranted(PURPOSE, VISIBLE));
        assertEquals("Le Van A", copy.fieldValue(PURPOSE, VISIBLE));
    }

    @Test
    public void copy_keepsDisplayFlag() {
        ConsentConfig config = config();
        ConsentState copy = ConsentState.defaultsOf(config).copy();

        copy.setFieldGranted(PURPOSE, HIDDEN, true);

        assertFalse(copy.isFieldGranted(PURPOSE, HIDDEN));
    }

    @Test
    public void applyDisplay_blocksHiddenFieldOnStateNotBuiltFromConfig() {
        ConsentConfig config = config();
        // Nhu state doc tu storage / app tu tao: display mac dinh true.
        ConsentState state = new ConsentState();
        state.setFieldGranted(PURPOSE, VISIBLE, true);
        state.setFieldGranted(PURPOSE, HIDDEN, true);
        assertTrue(state.isFieldGranted(PURPOSE, HIDDEN));

        state.applyDisplay(config);

        assertFalse(state.isFieldGranted(PURPOSE, HIDDEN));
        assertTrue(state.isFieldGranted(PURPOSE, VISIBLE));
        state.setFieldGranted(PURPOSE, HIDDEN, true);
        assertFalse(state.isFieldGranted(PURPOSE, HIDDEN));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void toValues_hiddenFieldKeepsValueButNeverAccepted() {
        ConsentConfig config = config();
        ConsentState state = ConsentState.defaultsOf(config);
        state.applyAcceptAll(config);
        state.setFieldValue(PURPOSE, HIDDEN, "a@example.com");

        Map<String, Object> purpose = (Map<String, Object>) state.toValues().get(PURPOSE);
        Map<String, Object> hidden = (Map<String, Object>) purpose.get(HIDDEN);

        assertEquals(Boolean.TRUE, purpose.get(ConsentState.KEY_IS_ACCEPT));
        assertEquals(Boolean.FALSE, hidden.get(ConsentState.KEY_IS_ACCEPT));
        assertEquals("a@example.com", hidden.get(ConsentState.KEY_VALUE));
        assertTrue(state.isAllGranted());
    }

    @Test
    public void applyDisplay_nullConfigIsNoop() {
        ConsentState state = new ConsentState();
        state.setFieldGranted(PURPOSE, HIDDEN, true);

        state.applyDisplay(null);

        assertTrue(state.isFieldGranted(PURPOSE, HIDDEN));
        assertNull(state.fieldValue(PURPOSE, HIDDEN));
    }
}
