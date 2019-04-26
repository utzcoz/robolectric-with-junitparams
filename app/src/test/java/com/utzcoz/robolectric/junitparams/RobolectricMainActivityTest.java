package com.utzcoz.robolectric.junitparams;

import android.widget.EditText;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class RobolectricMainActivityTest {
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

    @Test
    public void testContentPresenterWithPureNumber123456() {
        EditText etContentInput = mTestActivityInstance.findViewById(R.id.et_content_input);
        etContentInput.setText("123456");
        mTestActivityInstance.findViewById(R.id.btn_present_input).callOnClick();
        TextView tvContentPresenter = mTestActivityInstance.findViewById(R.id.tv_content_presenter);
        assertEquals("123456", tvContentPresenter.getText());
    }

    @Test
    public void testContentPresenterWithPureCharacterABcdEFg() {
        EditText etContentInput = mTestActivityInstance.findViewById(R.id.et_content_input);
        etContentInput.setText("ABcdEFg");
        mTestActivityInstance.findViewById(R.id.btn_present_input).callOnClick();
        TextView tvContentPresenter = mTestActivityInstance.findViewById(R.id.tv_content_presenter);
        assertEquals("ABcdEFg", tvContentPresenter.getText());
    }

    @Test
    public void testContentPresenterWithNumberAndCharacterABcdEFg123456() {
        EditText etContentInput = mTestActivityInstance.findViewById(R.id.et_content_input);
        etContentInput.setText("ABcdEFg123456");
        mTestActivityInstance.findViewById(R.id.btn_present_input).callOnClick();
        TextView tvContentPresenter = mTestActivityInstance.findViewById(R.id.tv_content_presenter);
        assertEquals("ABcdEFg123456", tvContentPresenter.getText());
    }
}
