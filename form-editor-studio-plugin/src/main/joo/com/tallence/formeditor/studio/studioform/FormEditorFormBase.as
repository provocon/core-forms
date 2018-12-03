package com.tallence.formeditor.studio.studioform {
import com.coremedia.cms.editor.sdk.premular.DocumentTabPanel;
import ext.data.ArrayStore;
import ext.data.Store;


[ResourceBundle('com.tallence.formeditor.studio.bundles.FormEditor')]
public class FormEditorFormBase extends DocumentTabPanel {

  public function FormEditorFormBase(config:FormEditorForm = null) {
    super(config);
  }

  public function getStore():Store {
    var arrayStore:Store = new ArrayStore(ArrayStore({
      data:getFormActionOptions(),
      fields:['id', 'value']
    }));
    return arrayStore;
  }


  public function getFormActionOptions():Array {
    // TODO: read the array ids and values from settings
      var result:Array = [
      [resourceManager.getString('com.tallence.formeditor.studio.bundles.FormEditor', 'FormEditor_actions_default'), 'default'],
      [resourceManager.getString('com.tallence.formeditor.studio.bundles.FormEditor', 'FormEditor_actions_mail'), 'mailAction']
    ];
    return result;
  }

}
}
