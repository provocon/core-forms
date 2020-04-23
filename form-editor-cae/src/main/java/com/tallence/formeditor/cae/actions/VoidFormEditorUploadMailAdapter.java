package com.tallence.formeditor.cae.actions;

import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * Void Adapter for only one reason: the cae is starting up without errors when integrating the extension.
 * Do not forget to add a {@link org.springframework.context.annotation.Primary} Annotation to your project-
 * adapter. Spring will complain about two Adapters otherwise.
 */
@Component
public class VoidFormEditorUploadMailAdapter implements FormEditorUploadMailAdapter {
  private static final Logger LOGGER = LoggerFactory.getLogger(VoidFormEditorUploadMailAdapter.class);

  @Override
  public boolean sendMail(FormEditor target, List<FormElement> elements, List<MultipartFile> files) {
    LOGGER.warn("VoidAdapter used to send Admin Mail");
    return true;
  }
}
