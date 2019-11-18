package com.tallence.formeditor.cae.actions;

import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.cae.handler.FormController.FormProcessingResult;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.tallence.formeditor.cae.handler.FormErrors.ADMIN_MAIL;
import static com.tallence.formeditor.cae.handler.FormErrors.WTC_SAVE;
import static com.tallence.formeditor.contentbeans.FormEditor.WEBTOCASESERVICE_ACTION;


/**
 * web2case Service Action for the form framework.
 * Sends the form data via Web2CaseService.
 *
 */
@Component
public class WebToCaseServiceFormAction implements FormAction {

    private static final Logger LOG = LoggerFactory.getLogger(WebToCaseServiceFormAction.class);

    @Autowired
    protected final FormEditorWebToCaseServiceAdapter webToCaseServiceAdapter;


    // -------------------------------------------------------------------------------------------------------------------
    //
    // constructor
    //
    // -------------------------------------------------------------------------------------------------------------------
    @Autowired
    public WebToCaseServiceFormAction(FormEditorWebToCaseServiceAdapter pWebToCaseServiceAdapter) {
        webToCaseServiceAdapter = pWebToCaseServiceAdapter;
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

        String formData = "";

        if (!sendDataToWebToCase(target, files, formElements, request, response)) {
            return new FormProcessingResult(false, WTC_SAVE);
        }

        // should an admin information email be send ?
        if (!sendAdminMail(target, formData, formElements)) {
            return new FormProcessingResult(false, ADMIN_MAIL);
        }

        // should an user confirmation email be send ?
        // boolean errorSendingUserMail = sendUserConfirmationMail(target, formElements, formData, request, response, files);
        return new FormProcessingResult(true, null);
    }


    @Override
    public boolean isResponsible(String key) {
        return WEBTOCASESERVICE_ACTION.equals(key);
    }

    // -------------------------------------------------------------------------------------------------------------------
    //
    // private methods
    //
    // -------------------------------------------------------------------------------------------------------------------

    private boolean sendDataToWebToCase(FormEditor target,
                                        List<MultipartFile> files,
                                        List<FormElement> formElements,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        boolean result = false;
        try {
            if (webToCaseServiceAdapter.sendDataToWebToCase(target, files, formElements, request, response)) {
                result = true;
            }
        } catch (Exception e) {
            LOG.error("sendDataToWebToCase() could send for form  "+target.getContentId(), e);
        }
        LOG.info("sendDataToWebToCase() form {}: {}", target.getContentId(), result);
        return result;
    }


    private boolean sendAdminMail(FormEditor target, String formData, List<FormElement> formElements) {
        boolean result = false;
        try {
            int count = 0;
            for (String address : target.getAdminEmails()) {
                if (webToCaseServiceAdapter.sendAdminMail(target, address, formData, formElements)) {
                    count++;
                } else {
                    LOG.error("sendAdminMail() mail to "+address+" could not be sent.");
                }
                result = count==target.getAdminEmails().size();
            }
        } catch (Exception e) {
            LOG.error("sendAdminMail() Confirmation mail to admin(s) "+target.getAdminEmails()+" could not be sent for form "+target.getContentId(), e);
        }
        LOG.info("sendAdminMail() form {}: {}", target.getContentId(), result);
        return result;
    }

}
