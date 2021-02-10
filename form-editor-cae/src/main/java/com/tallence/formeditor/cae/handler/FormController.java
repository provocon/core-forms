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

import com.coremedia.blueprint.cae.web.i18n.RequestMessageSource;
import com.coremedia.blueprint.cae.web.i18n.ResourceBundleInterceptor;
import com.coremedia.blueprint.cae.web.links.NavigationLinkSupport;
import com.coremedia.blueprint.common.contentbeans.CMChannel;
import com.coremedia.blueprint.common.navigation.Navigation;
import com.coremedia.blueprint.common.services.context.CurrentContextService;
import com.coremedia.objectserver.web.links.Link;
import com.tallence.formeditor.cae.FormFreemarkerFacade;
import com.tallence.formeditor.cae.actions.DefaultFormAction;
import com.tallence.formeditor.cae.actions.FormAction;
import com.tallence.formeditor.cae.elements.FileUpload;
import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.cae.elements.TextField;
import com.tallence.formeditor.cae.model.FormProcessingResult;
import com.tallence.formeditor.cae.model.FormSuccessResult;
import com.tallence.formeditor.cae.model.FormValidationResult;
import com.tallence.formeditor.cae.serializer.ValidationSerializationHelper;
import com.tallence.formeditor.cae.validator.TextValidator;
import com.tallence.formeditor.cae.validator.ValidationFieldError;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.coremedia.objectserver.web.HandlerHelper.MODEL_ROOT;
import static com.tallence.formeditor.cae.handler.FormErrors.RECAPTCHA;
import static com.tallence.formeditor.cae.handler.FormErrors.SERVER_VALIDATION;

/**
 * Handler for Form-Requests for {@link FormEditor}s.
 */
@Link
@RequestMapping
@Controller
public class FormController  {

  private static final Logger LOG = LoggerFactory.getLogger(FormController.class);

  private static final String FORMS_ROOT_URL_SEGMENT = "/d/forms";

  protected static final String FORM_EDITOR_SUBMIT_VIEW = "formEditorSubmit";
  static final String FORM_EDITOR_SUBMIT_URL = FORMS_ROOT_URL_SEGMENT + "/" + FORM_EDITOR_SUBMIT_VIEW + "/{currentContext}/{target}";

  private final List<FormAction> formActions;
  private final DefaultFormAction defaultFormAction;
  private final ReCaptchaService recaptchaService;
  private final FormFreemarkerFacade formFreemarkerFacade;
  private final CurrentContextService currentContextService;
  private final RequestMessageSource messageSource;
  private final ResourceBundleInterceptor pageResourceBundlesInterceptor;
  private final boolean encodeFormData;

  public FormController(List<FormAction> formActions,
                        DefaultFormAction defaultFormAction,
                        ReCaptchaService recaptchaService,
                        FormFreemarkerFacade formFreemarkerFacade,
                        CurrentContextService currentContextService,
                        RequestMessageSource messageSource,
                        ResourceBundleInterceptor pageResourceBundlesInterceptor,
                        @Value("${formEditor.cae.encodeData:true}") boolean encodeFormData) {
    this.formActions = formActions;
    this.defaultFormAction = defaultFormAction;
    this.recaptchaService = recaptchaService;
    this.formFreemarkerFacade = formFreemarkerFacade;
    this.currentContextService = currentContextService;
    this.messageSource = messageSource;
    this.pageResourceBundlesInterceptor = pageResourceBundlesInterceptor;
    this.encodeFormData = encodeFormData;
  }

  @Link(type = FormEditor.class, view = FORM_EDITOR_SUBMIT_VIEW, uri = FORM_EDITOR_SUBMIT_URL)
  public UriComponents buildLinkForFormSubmit(FormEditor form, UriComponentsBuilder uriComponentsBuilder) {
    return uriComponentsBuilder.buildAndExpand(currentContextService.getContext().getContentId(), form.getContentId());
  }

  @ResponseBody
  @PostMapping(value = FORM_EDITOR_SUBMIT_URL)
  public FormProcessingResult socialFormAction(@PathVariable(name = "currentContext") CMChannel navigation,
                                               @PathVariable FormEditor target,
                                               @RequestParam MultiValueMap<String, String> postData,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {

    if (target == null || navigation == null) {
      // Log the form data for debugging purpose, wrapped in a LinkedHashMap to make sure, the toString method is overwritten.
      LOG.warn("No form or context document found, cannot handle the request. Form data: {}", new LinkedHashMap<>(postData));
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return new FormProcessingResult(false, SERVER_VALIDATION);
    }

    //set up message interceptor
    ModelAndView modelAndView = new ModelAndView();
    modelAndView.addObject(MODEL_ROOT, navigation);
    pageResourceBundlesInterceptor.postHandle(request, response, null, modelAndView);
    request.setAttribute(NavigationLinkSupport.ATTR_NAME_CMNAVIGATION, navigation);

    List<FormElement> formElements = formFreemarkerFacade.parseFormElements(target);
    if (formElements.isEmpty()) {
      return new FormProcessingResult(
              false,
              SERVER_VALIDATION,
              FormValidationResult.globalError(getMessage("com.tallence.forms.error.empty", navigation)),
              null);
    }

    parseInputFormData(postData, request, formElements);
    /* CloudTelekom Extension
     * we need to handle the hidden fields of the form as well - not only studio-defined FormElements
     */
    List<FormElement> visibleAndHiddenElements = addAdditionalParametersAsTextField(postData, formElements);
    /* CloudTelekom Extension
     * validate also hidden fields
     */
    /* CloudTelekom Extension
     * don't re-parse hidden fields but use them now for the rest of the processing
     */
    formElements = visibleAndHiddenElements;

    //parse the files here already, before the validator runs
    List<MultipartFile> files = parseFileFormData(target, request, formElements);

    //Remove elements with unfulfilled dependencies, e.g. dependencies on other elements.
    for (Iterator<FormElement> iterator = formElements.iterator(); iterator.hasNext(); ) {
      if (!iterator.next().dependencyFulfilled(formElements)) {
        iterator.remove();
      }
    }

    //After all values are set: handle validationResult
    Map<String, List<String>> validationResults = validateFields(formElements, navigation);
    if (!validationResults.isEmpty()) {
      LOG.warn("Validation failed for Form [{}]. Validation-Result: [{}]", target.getContentId(), validationResults);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return new FormProcessingResult(
              false,
              SERVER_VALIDATION,
              new FormValidationResult(null, validationResults),
              null);
    }

    if (target.isSpamProtectionEnabled()) {
      if (!isHumanByReCaptcha(target, navigation, postData)) {
        LOG.warn("Google reCaptcha detected a bot for Form " + target.getContentId());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return new FormProcessingResult(
                false,
                RECAPTCHA,
                FormValidationResult.globalError(getMessage("com.tallence.forms.error.spamprotection", navigation)),
                null);
      }
    }

    //If not disabled by property, encode the formData before serializing. This might cause field values with length
    // greater than configured in the field's validator.max!
    //The encoding does not happen before the validation, because escaped inputs are longer and might cause a validation
    // failure, which does not match the frontend-validation behaviour.
    if (encodeFormData) {
      //Html-escape the input data to prevent security issues
      MultiValueMap<String, String> escapedPostData = new LinkedMultiValueMap<>();
      postData.forEach((key, value) ->
          escapedPostData.put(key, value.stream().map(HtmlUtils::htmlEscape).collect(Collectors.toList())));
      parseInputFormData(escapedPostData, request, formElements);
      // START CloudTelekom Extension
      // because we have added hidden field handling we need to be sure they will added here (now escaped) as well
      formElements = addAdditionalParametersAsTextField(escapedPostData, formElements);
      // END CloudTelekom Extension
    }

    //Default for an empty actionKey: the DefaultAction
    String actionKey = target.getFormAction();
    if (!StringUtils.hasText(actionKey)) {
      return prepareSubmitResult(defaultFormAction.handleFormSubmit(target, files, formElements, request, response), navigation);
    }

    Optional<FormAction> optional = formActions.stream().filter((action) -> action.isResponsible(actionKey)).findFirst();
    if (optional.isPresent()) {
      return prepareSubmitResult(optional.get().handleFormSubmit(target, files, formElements, request, response), navigation);
    } else {
      LOG.error("Cannot find a formAction for configured key [{}] for Form [{}]", actionKey, target.getContentId());
      throw new IllegalStateException("No action configured for form " + target.getContentId() + " with form type " + target.getFormAction());
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
   * @param postData the MultiValueMap of all post data
   * @param formElements the List of FormElements declared in Studio - these are already processed
   */
  private List<FormElement> addAdditionalParametersAsTextField(MultiValueMap<String, String> postData, List<FormElement> formElements) {
    List<FormElement> newTextFields = new ArrayList<>();
    Stream.of(postData.entrySet()).forEach(e -> e.parallelStream().forEach(e1 -> {
        String entryKey = e1.getKey();

        if ("g-recaptcha-response".equals(entryKey)) {
          LOG.info("Ignore Re-Captcha parameters.");
          return; // only skips this iteration.
        }

        if (!isPresentInFormElements(formElements, entryKey)) {
          addEntryToForm(newTextFields, e1, entryKey);
          return; // only skips this iteration.
        }

        LOG.debug("Found {} inside the list of FormElements - ignore the entry", entryKey);
    }));
    return addLists(formElements, newTextFields);
  }

  /**
   * Cloud Telekom Extension:
   */
  private List<FormElement> addLists(List<FormElement> formElements, List<FormElement> newTextFields) {
    List<FormElement> result = new ArrayList<>(formElements);

    /*
     * There is one edge case {this.encodeFormData == true} when the formValues will be
     * re-parsed from the encoded request which leads to NULL values in our hidden-fields.
     * So we need to check if the newly created TextField is already inside the list of formElements.
     * If it already is inside we just set the value again, otherwise we add it to the given list of formElements.
     */
    newTextFields.forEach(newTextField -> {
      var oldOne = formElements.stream()
              .filter(formElement -> formElement.getTechnicalName().equals(newTextField.getTechnicalName()))
              .findFirst();
      if (oldOne.isPresent()) {
        var fe = oldOne.get();
        fe.setValue(newTextField.getValue());
      } else {
        result.add(newTextField);
      }
    });

    return result;
  }

  /**
   * Cloud Telekom Extension:
   */
  private void addEntryToForm(List<FormElement> newTextFields, Map.Entry<String, List<String>> e1, String entryKey) {
    String fieldValue = e1.getValue().get(0);
    LOG.debug("Adding {} to list of FormElements with value '{}'", entryKey, fieldValue);
    TextField tf = new TextField();
    tf.setName(entryKey);
    tf.setTecName(entryKey);
    tf.setId(entryKey);
    TextValidator textValidator = new TextValidator();
    // refs #BMPCMS-7228 - uploaded Files maxSize check
    textValidator.setMandatory(false);
    textValidator.setMinSize(0);
    textValidator.setMaxSize(250);
    tf.setValidator(textValidator);
    tf.setValue(fieldValue);
    newTextFields.add(tf);
    // LOG.info("New FormElement {}: '{}'", tf.getTecName(), tf.getValue());
  }

  private boolean isPresentInFormElements(List<FormElement> formElements, String entryKey) {
    return formElements
            .parallelStream()
            .anyMatch(fe -> fe.getTechnicalName().equals(entryKey));
  }

  private void parseInputFormData(MultiValueMap<String, String> postData, HttpServletRequest request,
                                  List<FormElement> formElements) {
    formElements.stream().filter(f -> !(f instanceof FileUpload))
        .forEach(f -> f.setValue(postData, request));
  }


  private List<MultipartFile> parseFileFormData(FormEditor target, HttpServletRequest request, List<FormElement> formElements) {

    List<FileUpload> fileFields = formElements.stream()
            .filter(FileUpload.class::isInstance)
            .map(FileUpload.class::cast)
            .collect(Collectors.toList());
    if (!fileFields.isEmpty()) {
      return extractMultipartFileRequest(request)
              .map(r -> fileFields.stream().map(e -> processFileInput(r, e)).collect(Collectors.toList()))
              .orElseThrow(() ->
                      new IllegalStateException("Request is no instance of org.springframework.web.multipart.MultipartHttpServletRequest, cannot handle MultipartFile Upload for form " +
                              target.getContentId()));
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Extract the {@link MultipartHttpServletRequest} out of the spring security request object structure.
   * The MultipartFile cannot be fetched via {@link RequestParam} because the name is dynamic: based on the FileUploadField name.
   * @return the MultipartHttpServletRequest if available
   */
  private Optional<MultipartHttpServletRequest> extractMultipartFileRequest(HttpServletRequest request) {

    ServletRequest tmpReq = request;
    do {
      if (tmpReq instanceof MultipartHttpServletRequest) {
        return Optional.of((MultipartHttpServletRequest) tmpReq);
      } else if (tmpReq instanceof ServletRequestWrapper) {
        tmpReq = ((ServletRequestWrapper) tmpReq).getRequest();
      }
    } while (tmpReq != null);

    return Optional.empty();
  }

  private MultipartFile processFileInput(MultipartHttpServletRequest multipartRequest, FileUpload fileUpload) {
    MultipartFile file = multipartRequest.getFile(fileUpload.getTechnicalName());
    fileUpload.setValue(file);
    return file;
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

  private Map<String, List<String>> validateFields(List<FormElement> formElements, Navigation page) {
    Map<String, List<String>> validationResults = new HashMap<>();
    formElements.forEach(f -> {
      List<ValidationFieldError> validationResult = f.getValidationResult();
      if (!validationResult.isEmpty()) {
        validationResults.put(f.getTechnicalName(), validationResult.stream()
                .map(error -> ValidationSerializationHelper.getValidationMessage(
                        f.getName(),
                        error,
                        (key, args) -> messageSource.getMessage(key, args, page.getLocale())))
                .collect(Collectors.toList()));
      }
    });
    return validationResults;
  }

  private FormProcessingResult prepareSubmitResult(FormProcessingResult processingResult, Navigation navigation) {
    if (processingResult.isSuccess() && processingResult.getSuccessData() == null) {
      processingResult.setSuccessData(new FormSuccessResult(
              getMessage("com.tallence.forms.label.success.page.title", navigation),
              getMessage("com.tallence.forms.label.success.page.text", navigation),
              getMessage("com.tallence.forms.label.success.page.button", navigation)
      ));
    }
    if (processingResult.getError() != null && processingResult.getErrorData() == null) {
      processingResult.setErrorData(FormValidationResult.globalError(getMessage("com.tallence.forms.error.general", navigation)));
    }
    return processingResult;
  }


  private String getMessage(String messageKey, Navigation navigation) {
    try {
      return messageSource.getMessage(messageKey, null, navigation.getLocale());
    } catch (NoSuchMessageException x) {
      return messageKey; //simply return the message key instead of "crashing"
    }
  }
}
