package com.utzcoz.robolectric.junitparams;

import android.widget.EditText;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import junitparams.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricJUnitParamsTestRunner.class)
public class RobolectricJUnitParamsMainActivityTest {
    private MainActivity mTestActivityInstance;

    @Before
    public void setUp() {
        mTestActivityInstance = Robolectric.setupActivity(MainActivity.class);
    }

    @After
    public void tearDown() {
        mTestActivityInstance = null;
    }

    @Test
    public void testInstanceInitialization() {
        assertNotNull(mTestActivityInstance);
    }

    public void testContentPresenterWithInput() {
    }

    @Test
    @Parameters({"123456", "ABcdEFg", "ABcdEFg123456", "829~28+-';\"%$()"})
    public void testContentPresenterWithInput(String input) {
        EditText etContentInput = mTestActivityInstance.findViewById(R.id.et_content_input);
        etContentInput.setText(input);
        mTestActivityInstance.findViewById(R.id.btn_present_input).callOnClick();
        TextView tvContentPresenter = mTestActivityInstance.findViewById(R.id.tv_content_presenter);
        assertEquals(input, tvContentPresenter.getText());
    }
}
