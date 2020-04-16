package com.tallence.formeditor.cae.actions;

import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.cae.handler.FormController.FormProcessingResult;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.apache.commons.lang3.StringUtils;
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
 * WebToCase Service Action for the form framework.
 * Sends the form data via a WebToCase endpoint to a SalesForce instance.
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

        FormProcessingResult processingResult = new FormProcessingResult(true, null);

        // send data to webtocase
        boolean sfResult = sendDataToWebToCase(target, files, formElements, request, response);
        if (!sfResult) {
            processingResult = new FormProcessingResult(false, WTC_SAVE);
        }

        // send admin emails
        if (!sendAdminMail(target, formElements, sfResult)) {
            processingResult = new FormProcessingResult(false, ADMIN_MAIL);
        }
        return processingResult;
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

    /*
     * Since the resolving of ALL the recipients is done inside CTBaseAdapters#sendMail it is only
     * necessary to call sendAdminMail on the Adapter once !
     */
    private boolean sendAdminMail(FormEditor target,
                                  List<FormElement> formElements,
                                  boolean sfResult) {
        return webToCaseServiceAdapter.sendAdminMail(target, StringUtils.EMPTY, StringUtils.EMPTY, formElements, sfResult);
    }

}
