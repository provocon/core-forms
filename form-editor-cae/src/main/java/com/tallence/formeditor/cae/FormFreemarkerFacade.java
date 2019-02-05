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

package com.tallence.formeditor.cae;

import com.coremedia.cap.struct.Struct;
import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.cae.handler.ReCaptchaService;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FreemarkerFacade for Form elements.
 *
 */
public class FormFreemarkerFacade {

  private final FormElementFactory formElementFactory;

  @Autowired
  private final ReCaptchaService reCaptchaService;

  public FormFreemarkerFacade(FormElementFactory formElementFactory, ReCaptchaService pReCaptchaService) {
    this.reCaptchaService = pReCaptchaService;
    this.formElementFactory = formElementFactory;
  }

  public List<FormElement> parseFormElements(FormEditor formEditor) {

    Struct formData = formEditor.getFormElements();

    if (formData == null) {
      return Collections.emptyList();
    }

    return formData.getProperties().entrySet().stream()
        .filter(e -> e.getValue() instanceof Struct)
        .map(e -> parseElement((Struct) e.getValue(), e.getKey()))
        .collect(Collectors.toList());
  }

  public String getReCaptchaWebsiteSecretForSite() {
    return reCaptchaService.getWebsiteSecretForSite(null);
  }

  private FormElement parseElement(Struct value, String key) {
    return formElementFactory.createFormElement(value, key);
  }
}
