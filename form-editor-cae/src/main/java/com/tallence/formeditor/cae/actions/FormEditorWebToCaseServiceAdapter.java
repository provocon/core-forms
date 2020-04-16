package com.tallence.formeditor.cae.actions;

import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Wraps the access to a SalesForce WebToCase system. Used when sending data to a given WebToCase endpoint.
 */
public interface FormEditorWebToCaseServiceAdapter {

  /**
   * Notifies the form admin about a new form request.
   *
   * Field values of type {@link com.tallence.formeditor.cae.elements.FileUpload} can not yet be included in the mail.
   *
   * @param target the ContentBean of the current Form Document
   * @param recipient address which will receive the mail.
   * @param formData formData already serialized to one plain string
   * @param elements all the form elements, containing the current form request value.
   * @param salesForceResult boolean flag that indicates, if the call to the SalesForce WebToCase instance was successful.
   *
   * @return {@code true}, if the data was send via email successfully. {@code false} otherwise
   */
  boolean sendAdminMail(FormEditor target, String recipient, String formData, List<FormElement> elements, boolean salesForceResult);

  /**
   * Sends data to a SalesForce WebToCase backend.
   *
   * @param target    the ContentBean of the current Form Document
   * @param files     List with uploaded files
   * @param elements  all the form elements, containing the current form request value.
   * @param request   the current {@link HttpServletRequest}
   * @param response  the current {@link HttpServletResponse}
   *
   * @return {@code true}, if the data was saved successfully. {@code false} otherwise
   */
  boolean sendDataToWebToCase(FormEditor target,
                              List<MultipartFile> files,
                              List<FormElement> elements,
                              HttpServletRequest request,
                              HttpServletResponse response);
}
