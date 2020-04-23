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

package com.tallence.formeditor.studio.fields {
import com.tallence.formeditor.studio.elements.FormElement;
import com.coremedia.ui.data.ValueExpression;
import com.tallence.formeditor.studio.model.FormElementStructWrapper;

public class DefaultFieldBase extends TextField {

  private var tecNameDefault:String;

  public function DefaultFieldBase(config:DefaultFieldBase = null) {
    super(config);
  }

  override protected function initWithDefault(ve:ValueExpression):void {
    var formElement:FormElement = findParentByType(FormElement) as FormElement;
    var wrapper:FormElementStructWrapper = formElement.getFormElementStructWrapper();
    tecNameDefault = wrapper.getType() + "_" + wrapper.getId();
    ve.setValue(tecNameDefault);
  }

}
}
