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

package com.tallence.formeditor.cae.handler;

import com.coremedia.blueprint.common.contentbeans.CMChannel;
import com.coremedia.blueprint.common.services.context.CurrentContextService;
import com.coremedia.objectserver.web.links.Link;
import com.tallence.formeditor.cae.FormFreemarkerFacade;
import com.tallence.formeditor.cae.actions.DefaultFormAction;
import com.tallence.formeditor.cae.actions.FormAction;
import com.tallence.formeditor.cae.elements.FileUpload;
import com.tallence.formeditor.cae.elements.FormElement;
import static com.tallence.formeditor.cae.handler.FormErrors.RECAPTCHA;
import static com.tallence.formeditor.cae.handler.FormErrors.SERVER_VALIDATION;

import com.tallence.formeditor.cae.elements.TextField;
import com.tallence.formeditor.cae.validator.TextValidator;
import com.tallence.formeditor.contentbeans.FormEditor;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Handler for Form-Requests for {@link FormEditor}s.
 */
@Link
@RequestMapping
@Controller
public class FormController {

  private static final Logger LOG = LoggerFactory.getLogger(FormController.class);

  private static final String FORMS_ROOT_URL_SEGMENT = "/d/forms";

  private static final String PROCESS_SOCIAL_FORM_VIEW = "formEditorSubmit";
  static final String PROCESS_SOCIAL_FORM = FORMS_ROOT_URL_SEGMENT + "/" + PROCESS_SOCIAL_FORM_VIEW + "/{currentContext}/{target}";

  private final List<FormAction> formActions;
  private final DefaultFormAction defaultFormAction;
  private final ReCaptchaService recaptchaService;
  private final FormFreemarkerFacade formFreemarkerFacade;
  private final CurrentContextService currentContextService;

  public FormController(List<FormAction> formActions, DefaultFormAction defaultFormAction, ReCaptchaService recaptchaService, FormFreemarkerFacade formFreemarkerFacade, CurrentContextService currentContextService) {
    this.formActions = formActions;
    this.defaultFormAction = defaultFormAction;
    this.recaptchaService = recaptchaService;
    this.formFreemarkerFacade = formFreemarkerFacade;
    this.currentContextService = currentContextService;
  }

  @Link (type = FormEditor.class, view = PROCESS_SOCIAL_FORM_VIEW, uri = PROCESS_SOCIAL_FORM)
  public UriComponents buildLinkForSocialForm(FormEditor form, UriComponentsBuilder uriComponentsBuilder) {
    return uriComponentsBuilder.buildAndExpand(currentContextService.getContext().getContentId(), form.getContentId());
  }

  @ResponseBody
  @RequestMapping(value = PROCESS_SOCIAL_FORM, method = RequestMethod.POST)
  public FormProcessingResult socialFormAction(@PathVariable CMChannel currentContext,
                                               @PathVariable FormEditor target,
                                               @RequestParam MultiValueMap<String, String> postData,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws IOException {

    List<FormElement> formElements = getFormElements(target);

    List<MultipartFile> files = new ArrayList<>();
    parseFormData(target, postData, request, formElements, files);

    /* CloudTelekom Extension
     * we need to handle the hidden fields of the form as well - not only studio-defined FormElements
     */
    parseHiddenFields(postData, formElements);

    //After all values are set: handle validationResult
    for (FormElement<?> formElement : formElements) {
      List<String> validationResult = formElement.getValidationResult();
      if (!validationResult.isEmpty()) {
        //This should not happen, since a client side validation is expected.
        LOG.warn("Validation failed for field {} in form {}: {}",formElement.getName(), target.getContentId(), validationResult);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return new FormProcessingResult(false, SERVER_VALIDATION);
      }
    }

    if (target.isSpamProtectionEnabled()) {
      if (!isHumanByReCaptcha(target, currentContext, postData)) {
        LOG.warn("Google reCaptcha detected a bot for Form " + target.getContentId());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return new FormProcessingResult(false, RECAPTCHA);
      }
    }

    //Default for an empty actionKey: the DefaultAction
    String actionKey = target.getFormAction();
    if (!StringUtils.hasText(actionKey)) {
      return defaultFormAction.handleFormSubmit(target, files, formElements, request, response);
    }

    Optional<FormAction> optional = formActions.stream().filter((action) -> action.isResponsible(actionKey)).findFirst();
    if (optional.isPresent()) {
      return optional.get().handleFormSubmit(target, files, formElements, request, response);
    } else {
      LOG.error("Cannot find a formAction for configured key [{}] for Form [{}]", actionKey, target.getContentId());
      throw new IllegalStateException("No action configured for form " + target.getContentId() + " with form type " + target.getFormAction());
    }
  }

  private List<FormElement> getFormElements(FormEditor target) {

    List<FormElement> formElements = formFreemarkerFacade.parseFormElements(target);

    if (formElements.isEmpty()) {
      throw new IllegalStateException("Studio Form is not configured for Form " + target.getContentId());
    }
    return formElements;
  }


  private void parseFormData(FormEditor target, MultiValueMap<String, String> postData, HttpServletRequest request,
                             List<FormElement> formElements, List<MultipartFile> files) {
    for (FormElement formElement : formElements) {
      //Special Handling for FileUploads
      if (formElement instanceof FileUpload) {
        if (!(request instanceof MultipartHttpServletRequest)) {
          throw new IllegalStateException(
              "Request is no instance of org.springframework.web.multipart.MultipartHttpServletRequest, cannot handle MultipartFile Upload for form " +
                  target.getContentId());
        }
        MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
        MultipartFile file = multipartHttpServletRequest.getFile(formElement.getId());
        ((FileUpload) formElement).setValue(file);
        files.add(file);
      } else {
        formElement.setValue(postData, request);
      }
    }
  }

  /**
   * Cloud Telekom Extension:
   * We need to handle hidden Fields as well as studio defined FormElements.
   * We take all entries of formData and check if it is not a FormElement. If it is we ignore it here because
   * it has been processed earlier on. For all other Elements we simply define a
   * new {@link com.tallence.formeditor.cae.elements.TextField}, set the values accordingly and add these new
   * elements to the list of formElements.
   *
   * @param formElements the List of FormElements declared in Studio - these are already processed.
   * @param postData the MultiValueMap of all post data
   */
  private void parseHiddenFields(MultiValueMap<String, String> postData, List<FormElement> formElements) {
    List<FormElement> newTextFields = new ArrayList<>();
    Stream.of(postData.entrySet()).forEach(e -> e.parallelStream().forEach(e1 -> {
        String entryKey = e1.getKey();

        boolean exists = formElements
                .parallelStream()
                .anyMatch(fe -> fe.getTechnicalName().equals(entryKey));
        LOG.debug("Found {} inside the list of FormElements - ignore the entry", entryKey);
        if (!exists) {
          LOG.debug("{} NOT FOUND inside the list of FormElements - create a new TextField", entryKey);
          TextField tf = new TextField();
          tf.setName(entryKey);
          tf.setTecName(entryKey);
          tf.setValue(e1.getValue().get(0));
          tf.setValidator(new TextValidator());
          newTextFields.add(tf);
        }
    }));
    formElements.addAll(newTextFields);
  }

  /**
   * Validates the generated response-token in postData
   * @param target form with reCaptcha
   * @param postData includes the response-parameter, generated by the reCaptcha widget
   * @return true, if google says, the token is valid
   */
  private boolean isHumanByReCaptcha(FormEditor target, CMChannel currentContext, MultiValueMap<String, String> postData) {
    try {
      String googleReCaptchaResponse = postData.get("g-recaptcha-response").get(0);
      return recaptchaService.isHuman(googleReCaptchaResponse, currentContext);
    } catch (Exception ex) {
      LOG.error("Failed to verify reCapture via google for form " + target.getContentId(), ex);
      return false;
    }
  }

  public static class FormProcessingResult {

    private final Boolean success;
    private final String error;

    public FormProcessingResult(Boolean success, String error) {
      this.success = success;
      this.error = error;
    }

    public Boolean isSuccess() {
      return success;
    }

    public String getError() {
      return error;
    }
  }

}
