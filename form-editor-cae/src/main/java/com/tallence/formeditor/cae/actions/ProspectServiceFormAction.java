package com.tallence.formeditor.cae.actions;

import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.cae.handler.FormController.FormProcessingResult;
import static com.tallence.formeditor.cae.handler.FormErrors.ADMIN_MAIL;
import static com.tallence.formeditor.cae.handler.FormErrors.SF_SAVE;
import com.tallence.formeditor.contentbeans.FormEditor;
import static com.tallence.formeditor.contentbeans.FormEditor.PROSPECTSERVICE_ACTION;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


/**
 * Prospect Service Action for the form framework.
 * Sends the form data via ProspectService to SalesForce.
 *
 */
@Component
public class ProspectServiceFormAction implements FormAction {

    private static final Logger LOG = LoggerFactory.getLogger(ProspectServiceFormAction.class);

    @Autowired
    protected final FormEditorProspectServiceAdapter prospectServiceAdapter;


    // -------------------------------------------------------------------------------------------------------------------
    //
    // constructor
    //
    // -------------------------------------------------------------------------------------------------------------------
    @Autowired
    public ProspectServiceFormAction(FormEditorProspectServiceAdapter pProspectServiceAdapter) {
        prospectServiceAdapter = pProspectServiceAdapter;
    }


    // -------------------------------------------------------------------------------------------------------------------
    //
    // public methods
    //
    // -------------------------------------------------------------------------------------------------------------------
    @Override
    public FormProcessingResult handleFormSubmit(FormEditor target,
            List<MultipartFile> files,
            List<FormElement> formElements,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if (!files.isEmpty()) {
            throw new IllegalStateException("A ProspectServiceAction is not responsible for forms with file upload fields. "
                    +"A studio validator should take care of this - FormActionValidator");
        }

        // send data to salesforce
        if (!sendDataToSalesForce(target, formElements, request, response)) {
            return new FormProcessingResult(false, SF_SAVE);
        }

        // send email(s) to studio configured recipients
        if (!sendAdminMail(target, formElements)) {
            return new FormProcessingResult(false, ADMIN_MAIL);
        }

        return new FormProcessingResult(true, null);
    }


    @Override
    public boolean isResponsible(String key) {
        return PROSPECTSERVICE_ACTION.equals(key);
    }

    // -------------------------------------------------------------------------------------------------------------------
    //
    // private methods
    //
    // -------------------------------------------------------------------------------------------------------------------

    private boolean sendDataToSalesForce(FormEditor target,
                                         List<FormElement> formElements,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        boolean result = false;
        try {
            if (prospectServiceAdapter.sendDataToSalesForce(target, StringUtils.EMPTY, formElements, request, response)) {
                result = true;
            }
        } catch (Exception e) {
            LOG.error("sendDataToSalesForce() could send for form  "+target.getContentId(), e);
        }
        LOG.info("sendDataToSalesForce() form {}: {}", target.getContentId(), result);
        return result;
    }

    /*
     * Since the resolving of ALL the recipients is done inside CTBaseAdapters#sendMail it is only
     * necessary to call sendAdminMail on the Adapter once.
     */
    private boolean sendAdminMail(FormEditor target, List<FormElement> formElements) {
        return prospectServiceAdapter.sendAdminMail(target, StringUtils.EMPTY, StringUtils.EMPTY, formElements);
    }

}
