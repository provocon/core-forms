package com.tallence.formeditor.cae.mocks;

import com.tallence.formeditor.cae.actions.FormEditorSipTrunkAdapter;
import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


/**
 * Mocking the {@link FormEditorSipTrunkAdapter} with an empty implementation.
 *
 */
@Component
public class SipTrunkAdapterMock implements FormEditorSipTrunkAdapter {

    @Override
    public boolean sendAdminMail(FormEditor target, String recipient, String formData, List<FormElement> elements) {
        return true;
    }


    @Override
    public boolean sendDataToSalesForce(FormEditor target,
                                        String formData,
                                        List<FormElement> elements,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        return true;
    }

}
