package com.tallence.formeditor.cae.mocks;

import com.tallence.formeditor.cae.actions.FormEditorProspectServiceAdapter;
import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.contentbeans.FormEditor;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;


/**
 * Mocking the {@link FormEditorProspectServiceAdapter} with an empty implementation.
 *
 */
@Component
public class ProspectServiceAdapterMock implements FormEditorProspectServiceAdapter {

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
