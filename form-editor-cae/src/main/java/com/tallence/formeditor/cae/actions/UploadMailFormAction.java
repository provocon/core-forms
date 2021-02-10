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
import com.tallence.formeditor.cae.model.FormProcessingResult;
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

import static com.tallence.formeditor.cae.handler.FormErrors.UPLOAD_MAIL;
import static com.tallence.formeditor.cae.handler.FormErrors.USER_MAIL;
import static com.tallence.formeditor.contentbeans.FormEditor.MAIL_ACTION;
import static com.tallence.formeditor.contentbeans.FormEditor.UPLOAD_MAIL_ACTION;

/**
 * Mail Action for the form framework.
 * Sends the form data via mail to a configured mail address.
 * Allows user uploads.
 *
 */
@Component
public class UploadMailFormAction implements FormAction {

  private static final Logger LOG = LoggerFactory.getLogger(UploadMailFormAction.class);

  protected final FormEditorUploadMailAdapter mailAdapter;

  @Autowired
  public UploadMailFormAction(FormEditorUploadMailAdapter mailAdapter) {
    this.mailAdapter = mailAdapter;
  }


  public FormProcessingResult handleFormSubmit(FormEditor target,
                                               List<MultipartFile> files,
                                               List<FormElement> formElements,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws IOException {

    boolean errorSendingMail = true;
    try {
      mailAdapter.sendMail(target, formElements, files);
      errorSendingMail = false;
    } catch (Exception e) {
      LOG.error("Mail could not be sent! For Form  " + target.getContentId(), e);
    }

    return new FormProcessingResult(true, errorSendingMail ? UPLOAD_MAIL : null);
  }

  public boolean isResponsible(String key) {
    return UPLOAD_MAIL_ACTION.equals(key);
  }
}
