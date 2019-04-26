/*
 * Copyright 2018 Tallence AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

        //String formData = serializeFormElements(target, formElements, files);
        String formData = "";

        if (!sendDataToSalesForce(target, formData, formElements, request, response)) {
            return new FormProcessingResult(false, SF_SAVE);
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
        return PROSPECTSERVICE_ACTION.equals(key);
    }

    // -------------------------------------------------------------------------------------------------------------------
    //
    // private methods
    //
    // -------------------------------------------------------------------------------------------------------------------

    private boolean sendDataToSalesForce(FormEditor target,
            String formData,
            List<FormElement> formElements,
            HttpServletRequest request,
            HttpServletResponse response) {
        boolean result = false;
        try {
            if (prospectServiceAdapter.sendDataToSalesForce(target, formData, formElements, request, response)) {
                result = true;
            }
        } catch (Exception e) {
            LOG.error("sendDataToSalesForce() could send for form  "+target.getContentId(), e);
        }
        LOG.info("sendDataToSalesForce() form {}: {}", target.getContentId(), result);
        return result;
    }


    private boolean sendAdminMail(FormEditor target, String formData, List<FormElement> formElements) {
        boolean result = false;
        try {
            int count = 0;
            for (String address : target.getAdminEmails()) {
                if (prospectServiceAdapter.sendAdminMail(target, address, formData, formElements)) {
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
