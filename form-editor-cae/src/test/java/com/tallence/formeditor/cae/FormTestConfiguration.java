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

import com.coremedia.blueprint.common.services.context.CurrentContextService;
import com.coremedia.blueprint.testing.ContentTestHelper;
import com.coremedia.cap.test.xmlrepo.XmlRepoConfiguration;
import com.coremedia.cap.test.xmlrepo.XmlUapiConfig;
import com.coremedia.cms.delivery.configuration.DeliveryConfigurationProperties;
import com.coremedia.objectserver.configuration.CaeConfigurationProperties;
import com.coremedia.springframework.xml.ResourceAwareXmlBeanDefinitionReader;
import com.tallence.formeditor.cae.elements.FormElement;
import com.tallence.formeditor.cae.handler.ReCaptchaService;
import com.tallence.formeditor.cae.handler.ReCaptchaServiceImpl;
import com.tallence.formeditor.cae.parser.AbstractFormElementParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

import java.util.List;

import static com.coremedia.cap.test.xmlrepo.XmlRepoResources.*;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

/**
 * Configuration class to set up form test infrastructure.
 */
@Configuration
@ImportResource(
    value = {
        CACHE,
        CONTENT_BEAN_FACTORY,
        DATA_VIEW_FACTORY,
        ID_PROVIDER,
        HANDLERS,
        LINK_FORMATTER,
        VIEW_RESOLVER,
        "classpath:/framework/spring/blueprint-contentbeans.xml",
        "classpath:/META-INF/coremedia/component-forms.xml",
        "classpath:/framework/spring/blueprint-handlers.xml",
        "classpath:/framework/spring/blueprint-services.xml",
        "classpath:/com/tallence/formeditor/cae/testdata/bundle-replace-context.xml",
    },
    reader = ResourceAwareXmlBeanDefinitionReader.class
)
@PropertySource("classpath:com/tallence/formeditor/cae/test.properties")
@Import({XmlRepoConfiguration.class})
@ComponentScan(basePackages = {
    "com.tallence.formeditor.cae",
    "com.coremedia.cms.delivery.configuration",
    "com.coremedia.objectserver.configuration",
    "com.coremedia.objectserver.web.config"
})
@EnableConfigurationProperties({
        DeliveryConfigurationProperties.class,
        CaeConfigurationProperties.class
})
public class FormTestConfiguration {

  private static final String CONTENT_REPOSITORY = "classpath:/com/tallence/formeditor/cae/testdata/contenttest.xml";

  @Bean
  @Scope(SCOPE_SINGLETON)
  public XmlUapiConfig xmlUapiConfig() {
    return new XmlUapiConfig(CONTENT_REPOSITORY);
  }

  @Bean
  @Scope(SCOPE_SINGLETON)
  public ContentTestHelper contentTestHelper() {
    return new ContentTestHelper();
  }

  @Bean
  @Scope(SCOPE_SINGLETON)
  public FormElementFactory formElementFactory(List<AbstractFormElementParser<? extends FormElement>> parsers) {
    return new FormElementFactory(parsers);
  }

  /**
   * Mocking the {@link ReCaptchaService} it is not yet tested.
   */
  @Bean
  @Scope(SCOPE_SINGLETON)
  public ReCaptchaService reCaptchaService() {
    return new ReCaptchaServiceImpl(new ReCaptchaServiceImpl.ReCaptchaAuthentication(null, null));
  }

  @Bean
  @Scope(SCOPE_SINGLETON)
  public FormFreemarkerFacade freemarkerFacade(FormElementFactory formElementFactory, ReCaptchaService reCaptchaService, CurrentContextService currentContextService) {
    return new FormFreemarkerFacade(formElementFactory, reCaptchaService, currentContextService);
  }



}
