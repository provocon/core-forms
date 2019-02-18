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
import com.tallence.formeditor.contentbeans.FormEditor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Wraps the access to the salesforce system. Used when sending data to salesforce.
 */
public interface FormEditorProspectServiceAdapter {

  /**
   * Informs the form admin about a new form request.
   *
   * Field values of type {@link com.tallence.formeditor.cae.elements.FileUpload} can not yet be included in the mail.
   *
   * @param target the ContentBean of the current Form Document
   * @param recipient address which will receive the mail.
   * @param formData formData already serialized to one plain string
   * @param elements all the form elements, containing the current form request value.
   * @return true, if the data was saved successfully. False otherwise
   */
  boolean sendAdminMail(FormEditor target, String recipient, String formData, List<FormElement> elements);

  /**
   * Serialize the given data to the form editor storage.
   *
   * @param target the ContentBean of the current Form Document
   * @param formData formData already serialized to one plain string
   * @param elements all the form elements, containing the current form request value.
   * @return true, if the data was saved successfully. False otherwise
   */
  boolean sendDataToSalesForce(FormEditor target, String formData, List<FormElement> elements);
}
