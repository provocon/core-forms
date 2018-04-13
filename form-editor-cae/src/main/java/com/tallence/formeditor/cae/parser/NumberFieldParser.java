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

package com.tallence.formeditor.cae.parser;

import com.coremedia.cap.struct.Struct;
import com.tallence.formeditor.cae.elements.NumberField;
import com.tallence.formeditor.cae.elements.TextField;
import com.tallence.formeditor.cae.validator.NumberValidator;
import org.springframework.stereotype.Component;

/**
 * Parser for elements of type {@link TextField}
 */
@Component
public class NumberFieldParser extends AbstractFormElementParser<NumberField> {

  public static final String parserKey = "NumberField";


  @Override
  public NumberField instantiateType(Struct elementData) {
    return new NumberField();
  }


  @Override
  public void parseSpecialFields(NumberField formElement, Struct elementData) {
    doForStructElement(elementData, FORM_DATA_VALIDATOR, validator -> {
      NumberValidator numberValidator = new NumberValidator();

      numberValidator.setMandatory(parseBoolean(validator, FORM_VALIDATOR_MANDATORY));
      numberValidator.setMinSize(parseInteger(validator, FORM_VALIDATOR_MINSIZE));
      numberValidator.setMaxSize(parseInteger(validator, FORM_VALIDATOR_MAXSIZE));

      formElement.setValidator(numberValidator);
    });
  }

  @Override
  public String getParserKey() {
    return this.parserKey;
  }
}