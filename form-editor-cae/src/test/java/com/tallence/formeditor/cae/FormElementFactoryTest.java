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

import com.coremedia.blueprint.testing.ContentTestHelper;
import com.tallence.formeditor.cae.elements.*;
import com.tallence.formeditor.cae.validator.InvalidGroupElementException;
import com.tallence.formeditor.contentbeans.FormEditor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

import static com.tallence.formeditor.cae.parser.ConsentFormCheckBoxParser.CONSENT_FORM_CHECK_BOX_TYPE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

/**
 * Tests the parsing, serialization and validation of each form field.
 */
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = FormTestConfiguration.class)
public class FormElementFactoryTest {


  @Autowired
  private FormElementFactory factory;

  @Autowired
  private ContentTestHelper contentTestHelper;


  private  <T> T getContentBean(int id) {
    return contentTestHelper.getContentBean(id);
  }

  private <T extends FormElement> T getTestFormElement(String id) {
    FormEditor article = getContentBean(2);
    return this.factory.createFormElement(article.getFormElements().getStruct(id), id);
  }

  @Test
  public void testTextField() {
    TextField formElement = getTestFormElement("TextField");

    assertThat(formElement, is(instanceOf(TextField.class)));
    assertThat(formElement.getPlaceholder(), is("Platzhalter"));

    //123 is too short and does not match the regex validator
    formElement.setValue("123");
    assertThat(formElement.getValidationResult().size(), is(2));
    formElement.setValue("");
    assertThat(formElement.getValidationResult().size(), is(1));

    //12346 ist not valid, because of the regex validator ("12345")
    formElement.setValue("12346");
    assertThat(formElement.getValidationResult().size(), is(1));
    formElement.setValue("12345");
    assertTrue(formElement.getValidationResult().isEmpty());
  }


  @Test
  public void testNumberField() {
    NumberField formElement = getTestFormElement("NumberField");

    assertThat(formElement, is(instanceOf(NumberField.class)));
    formElement.setValue("abc");
    assertThat(formElement.getValidationResult().size(), is(1));
    formElement.setValue("2");
    assertThat(formElement.getValidationResult().size(), is(1));
    formElement.setValue("150");
    assertTrue(formElement.getValidationResult().isEmpty());
  }

  @Test
  public void testRadioButton() {
    RadioButtonGroup formElement = getTestFormElement("RadioButtonsMandatory");

    assertThat(formElement, is(instanceOf(RadioButtonGroup.class)));
    formElement.setValue("value_456");
    assertTrue(formElement.getValidationResult().isEmpty());
    formElement.setValue("value_123");
    assertTrue(formElement.getValidationResult().isEmpty());
    assertThat(formElement.serializeValue(), is("display_123"));
    formElement.setValue("");
    assertThat(formElement.getValidationResult().size(), is(1));


    assertThat(formElement.getOptions(), notNullValue());
    assertThat(formElement.getOptions().get(1).isSelectedByDefault(), is(true));

  }

  @Test
  public void testDependentFields() {
    TextField formElement = getTestFormElement("DependentField");

    assertEquals("myComplexCustomId", formElement.getAdvancedSettings().getCustomId());
    assertEquals(Integer.valueOf(3), formElement.getAdvancedSettings().getColumnWidth());
    assertTrue(formElement.getAdvancedSettings().isBreakAfterElement());
    assertEquals("RadioButtonsOptional", formElement.getAdvancedSettings().getDependentElementId());
    assertEquals("value_456", formElement.getAdvancedSettings().getDependentElementValue());
    assertTrue(formElement.getAdvancedSettings().isVisibilityDependent());
  }

  @Test
  public void testOptionalRadioButton() {
    RadioButtonGroup formElement = getTestFormElement("RadioButtonsOptional");

    assertThat(formElement, is(instanceOf(RadioButtonGroup.class)));

    formElement.setValue(null);
    assertEquals("", formElement.serializeValue());

  }

  @Test (expected = InvalidGroupElementException.class)
  public void testRadioButtonInvalid() {
    RadioButtonGroup formElement = getTestFormElement("RadioButtonsEmptyValidator");
    //An Exception is expected here
    formElement.setValue("invalid");
    assertThat(formElement.getValidationResult().size(), is(1));
  }

  @Test
  public void testCheckBoxes() {
    CheckBoxesGroup formElement = getTestFormElement("CheckBoxesMandatory");

    assertThat(formElement, is(instanceOf(CheckBoxesGroup.class)));
    formElement.setValue(Arrays.asList("value_123", "value_456"));
    assertTrue(formElement.getValidationResult().isEmpty());
    assertThat(formElement.serializeValue(), is("[display_123, display_456]"));

    formElement.setValue(Collections.emptyList());
    assertThat(formElement.getValidationResult().size(), is(1));
    assertThat(formElement.getOptions(), notNullValue());
    assertThat(formElement.getOptions().get(1).isSelectedByDefault(), is(true));

  }

  @Test (expected = InvalidGroupElementException.class)
  public void testCheckBoxesInvalid() {
    CheckBoxesGroup formElement = getTestFormElement("CheckBoxesEmptyValidator");
    //An Exception is expected here
    formElement.setValue(Arrays.asList("invalid", "value_123"));
    assertThat(formElement.getValidationResult().size(), is(1));
  }

  @Test
  public void testSelectBoxes() {
    SelectBox formElement = getTestFormElement("SelectBoxMandatory");

    assertThat(formElement, is(instanceOf(SelectBox.class)));
    formElement.setValue("value_123");
    assertTrue(formElement.getValidationResult().isEmpty());
    assertThat(formElement.serializeValue(), is("display_123"));

    formElement.setValue(null);
    assertThat(formElement.getValidationResult().size(), is(1));

  }

  @Test (expected = InvalidGroupElementException.class)
  public void testSelectBoxesInvalid() {
    SelectBox formElement = getTestFormElement("SelectBoxEmptyValidator");
    //An Exception is expected here
    formElement.setValue("invalid");
    formElement.getValidationResult();
  }

  @Test
  public void testSelectBoxesOnlyDisplayName() {
    SelectBox formElement = getTestFormElement("SelectBoxOnlyDisplayName");

    assertThat(formElement, is(instanceOf(SelectBox.class)));
    formElement.setValue("display_123");
    assertTrue(formElement.getValidationResult().isEmpty());
    assertThat(formElement.serializeValue(), is("display_123"));

    formElement.setValue(null);
    assertThat(formElement.getValidationResult().size(), is(1));

  }



  @Test
  public void testTextArea() {
    TextArea formElement = getTestFormElement("TextArea");

    assertThat(formElement, is(instanceOf(TextArea.class)));
    formElement.setValue("123");
    assertTrue(formElement.getValidationResult().isEmpty());
    formElement.setValue(null);
    assertThat(formElement.getValidationResult().size(), is(1));
    formElement.setValue("12345678900");
    assertThat(formElement.getValidationResult().size(), is(1));

    assertThat(formElement.getColumns(), is(4));
    assertThat(formElement.getRows(), is(5));
  }

  @Test
  public void testTextOnly() {
    TextOnly formElement = getTestFormElement("TextOnly");

    assertThat(formElement, is(instanceOf(TextOnly.class)));
    assertThat(formElement.getHint(), is("Das ist ein langer Text zur Erklärung des Formulars"));
    assertThat(formElement.getName(), is("TextOnly"));
  }

  @Test
  public void testConsentFormCheckBox() {
    ConsentFormCheckBox formElement = getTestFormElement(CONSENT_FORM_CHECK_BOX_TYPE);

    assertThat(formElement, is(instanceOf(ConsentFormCheckBox.class)));
    assertThat(formElement.getHint(), is("Please confirm the %data protection consent form%"));
    assertNotNull(formElement.getLinkTarget());
    assertThat(formElement.getLinkTarget().getContentId(), equalTo(8));
  }

  @Test
  public void testZipField() {
    ZipField formElement = getTestFormElement("ZipFieldTest");

    assertThat(formElement.getHint(), is("Bitte Ihre Postleitzahl eingeben"));
    assertThat(formElement.getValidator().getRegexp().toString(), equalTo("\\d{5}"));

    //the field is mandatory -> an error without a value
    assertThat(formElement.getValidationResult().size(), is(1));

    formElement.setValue("22945");
    assertTrue(formElement.getValidationResult().isEmpty());
  }

  @Test
  public void testPhoneField() {
    PhoneField formElement = getTestFormElement("PhoneFieldTest");
    assertEquals(formElement.getName(), "Phone");
  }

  @Test
  public void testFaxField() {
    FaxField formElement = getTestFormElement("FaxFieldTest");
    assertEquals(formElement.getName(), "Fax");
  }

  @Test
  public void testStreetNumberField() {
    StreetNumberField formElement = getTestFormElement("StreetFieldTest");
    assertEquals(formElement.getName(), "Street and number");
  }

  @Test
  public void testDateField_min() {
    DateField formElement = getTestFormElement("DateFieldMin");
    assertEquals(formElement.getName(), "DateField min");

    assertThat(formElement, is(instanceOf(DateField.class)));
    assertFalse(formElement.getValidationResult().isEmpty());

    formElement.setValue("abc");
    assertThat(formElement.getValidationResult().size(), is(1));

    formElement.setValue(ZonedDateTime.now().minusDays(3).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    assertThat(formElement.getValidationResult().size(), is(1));

    formElement.setValue(ZonedDateTime.now().plusDays(3).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    assertThat(formElement.getValidationResult().size(), is(0));
  }

  @Test
  public void testDateField_max() {
    DateField formElement = getTestFormElement("DateFieldMax");
    assertEquals(formElement.getName(), "DateField max");

    assertThat(formElement, is(instanceOf(DateField.class)));
    assertFalse(formElement.getValidationResult().isEmpty());

    formElement.setValue("abc");
    assertThat(formElement.getValidationResult().size(), is(1));

    //using ZondeDateTime here, frontend is sending including time info
    // max date + 3
    formElement.setValue(ZonedDateTime.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    assertThat(formElement.getValidationResult().size(), is(1));

    // max date - 3
    formElement.setValue(ZonedDateTime.now().minusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    assertThat(formElement.getValidationResult().size(), is(0));

    // same date as max date
    formElement.setValue(ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    assertThat(formElement.getValidationResult().size(), is(0));

    //same check for different date format
    formElement.setValue(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    assertThat(formElement.getValidationResult().size(), is(0));

    //same check for different date format
    formElement.setValue(ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
    assertThat(formElement.getValidationResult().size(), is(0));

  }
}
