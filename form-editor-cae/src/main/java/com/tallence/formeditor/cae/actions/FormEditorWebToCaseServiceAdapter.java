package com.tallence.formeditor.cae.actions;

import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Wraps the access to the web2case system. Used when sending data to web2case.
 */
public interface FormEditorWebToCaseServiceAdapter {

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
   * @param elements all the form elements, containing the current form request value.
   * @return true, if the data was saved successfully. False otherwise
   */
  boolean sendDataToWebToCase(FormEditor target,
                              List<MultipartFile> files,
                               List<FormElement> elements,
                               HttpServletRequest request,
                               HttpServletResponse response);
}
