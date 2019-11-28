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
package com.tallence.formeditor.cae.mocks;

import com.tallence.formeditor.cae.actions.FormEditorMailAdapter;
import com.tallence.formeditor.cae.actions.FormEditorUploadMailAdapter;
import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Mocking the {@link FormEditorMailAdapter} and storing the params in local variables,
 * which can be checked by tests afterwards.
 *
 */
@Component
@Primary
public class UploadMailAdapterMock implements FormEditorUploadMailAdapter {

  public String usersFormData;
  public String usersRecipient;
  public String adminFormData;
  public String adminRecipient;


  @Override
  public boolean sendMail(FormEditor target, List<FormElement> elements, List<MultipartFile> files) {

    if (this.usersFormData != null || this.usersRecipient != null) {
      throw new IllegalStateException("Call com.tallence.formeditor.cae.mocks.MailAdapterMock.clear before setting usersFormData or usersRecipient again");
    }

    return true;
  }

  public void clear() {
    this.usersFormData = null;
    this.usersRecipient = null;
    this.adminFormData = null;
    this.adminRecipient = null;
  }
}
