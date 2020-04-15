package com.tallence.formeditor.cae.mocks;

import com.tallence.formeditor.cae.actions.FormEditorWebToCaseServiceAdapter;
import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


/**
 * Mocking the {@link FormEditorWebToCaseServiceAdapter} with an empty implementation.
 *
 */
@Component
public class WebToCaseAdapterMock implements FormEditorWebToCaseServiceAdapter {


  @Override
  public boolean sendAdminMail(FormEditor target,
                               String recipient,
                               String formData,
                               List<FormElement> elements,
                               boolean sfResult) {
    return true;
  }

  @Override
  public boolean sendDataToWebToCase(FormEditor target,
                                     List<MultipartFile> files,
                                     List<FormElement> elements,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
    return true;
  }


}
