package com.tallence.formeditor.cae.actions;

import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.cae.model.FormProcessingResult;
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
import static com.tallence.formeditor.cae.handler.FormErrors.SF_SAVE;
import static com.tallence.formeditor.contentbeans.FormEditor.SIPTRUNK_ACTION;


/**
 * Prospect Service Action for the form framework.
 * Sends the form data via ProspectService to SalesForce.
 * Before that it adds the requestParam siptrunkConfig to the description field of the Prospect.
 *
 */
@Component
public class SipTrunkFormAction implements FormAction {

    private static final Logger LOG = LoggerFactory.getLogger(SipTrunkFormAction.class);

    @Autowired
    protected final FormEditorSipTrunkAdapter sipTrunkAdapter;


    // -------------------------------------------------------------------------------------------------------------------
    //
    // constructor
    //
    // -------------------------------------------------------------------------------------------------------------------
    @Autowired
    public SipTrunkFormAction(FormEditorSipTrunkAdapter pFormEditorSipTrunkAdapter) {
        sipTrunkAdapter = pFormEditorSipTrunkAdapter;
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
            throw new IllegalStateException("A SipTrunkAction is not responsible for forms with file upload fields. "
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
        return SIPTRUNK_ACTION.equals(key);
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
            if (sipTrunkAdapter.sendDataToSalesForce(target, StringUtils.EMPTY, formElements, request, response)) {
                result = true;
            }
        } catch (Exception e) {
            LOG.error("sendDataToSalesForce() could not send for form  "+target.getContentId(), e);
        }
        LOG.info("sendDataToSalesForce() form {}: {}", target.getContentId(), result);
        return result;
    }

    /*
     * Since the resolving of ALL the recipients is done inside CTBaseAdapters#sendMail it is only
     * necessary to call sendAdminMail on the Adapter once !
     */
    private boolean sendAdminMail(FormEditor target, List<FormElement> formElements) {
        return sipTrunkAdapter.sendAdminMail(target, StringUtils.EMPTY, StringUtils.EMPTY, formElements);
    }

}
