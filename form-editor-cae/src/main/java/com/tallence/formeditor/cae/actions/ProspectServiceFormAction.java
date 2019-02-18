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
import static com.tallence.formeditor.cae.handler.FormErrors.SF_SAVE;
import static com.tallence.formeditor.cae.handler.FormErrors.USER_MAIL;
import static com.tallence.formeditor.contentbeans.FormEditor.MAIL_ACTION;
import static com.tallence.formeditor.contentbeans.FormEditor.PROSPECTSERVICE_ACTION;

/**
 * Prospect Service Action for the form framework.
 * Sends the form data via ProspectService to SalesForce.
 *
 */
@Component
public class ProspectServiceFormAction implements FormAction {

  private static final Logger LOG = LoggerFactory.getLogger(ProspectServiceFormAction.class);

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
      throw new IllegalStateException("A ProspectServiceAction is not responsible for forms with file upload fields. " +
        "A studio validator should take care of this - FormActionValidator");
    }

    //String formData = serializeFormElements(target, formElements, files);
    String formData = "";
    if (!sendDataToSalesForce(target, formData, formElements)) {
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
  private boolean sendDataToSalesForce(FormEditor target, String formData, List<FormElement> formElements) {
    try {
        if (!prospectServiceAdapter.sendDataToSalesForce(target, formData, formElements)) {
          throw new IllegalStateException("Data to Salesforce could not be sent!");
        }
    } catch (Exception e) {
      LOG.error("Data to SalesForce could not be sent! For Form  " + target.getContentId(), e);
      return false;
    }
    return true;
  }

  private boolean sendAdminMail(FormEditor target, String formData, List<FormElement> formElements) {
    try {
      for (String address : target.getAdminEmails()) {
        if (!prospectServiceAdapter.sendAdminMail(target, address, formData, formElements)) {
          throw new IllegalStateException("Mail to " + address + " could not be sent!");
        }
      }
    } catch (Exception e) {
      LOG.error("Confirmation Mail to admin(s) " + target.getAdminEmails() + " could not be sent! For Form  " + target.getContentId(), e);
      return false;
    }
    return true;
  }
}
