/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.declaration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_DOMAIN_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.POLICY_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.from;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.to;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.app.declaration.fluent.ArtifactDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ElementDeclarer;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.SimpleConfigAttribute;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Document;

/**
 * //TODO
 */
public class DefaultXmlArtifactDeclarationLoader implements XmlArtifactDeclarationLoader {

  @Override
  public ArtifactDeclaration load(String configResource, DslResolvingContext context) {
    try {
      final Map<ExtensionModel, DslSyntaxResolver> resolvers = context.getExtensions().stream()
        .collect(toMap(e -> e, e -> DslSyntaxResolver.getDefault(e, context)));

      InputStream appIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(configResource);
      checkArgument(appIs != null, "The given application was not found as resource");

      Document document = new XmlConfigurationDocumentLoader().loadDocument(Optional.empty(), configResource, appIs);

      ConfigLine configLine = new XmlApplicationParser(new SpiServiceRegistry())
        .parse(document.getDocumentElement())
        .orElseThrow(() -> new Exception("Failed to load config"));

      ArtifactDeclarer artifactDeclarer = ElementDeclarer.newArtifact();




      return artifactDeclarer.getDeclaration();

    } catch (Exception e){
      e.printStackTrace();
      return null;
    }
  }

  public ComponentModel extractComponentDefinitionModel(ConfigLine configLine, String configFileName) {

    String namespace = configLine.getNamespace() == null ? CORE_PREFIX : configLine.getNamespace();

    ComponentModel.Builder builder = new ComponentModel.Builder()
      .setIdentifier(builder().withPrefix(namespace).withName(configLine.getIdentifier()).build())
      .setTextContent(configLine.getTextContent())
      .setConfigFileName(configFileName)
      .setLineNumber(configLine.getLineNumber());

    to(builder).addNode(from(configLine).getNode());

    for (SimpleConfigAttribute simpleConfigAttribute : configLine.getConfigAttributes().values()) {
      builder.addParameter(simpleConfigAttribute.getName(), simpleConfigAttribute.getValue(), simpleConfigAttribute.isValueFromSchema());
    }

    List<ComponentModel> componentModels = configLine.getChildren().stream()
      .map(childConfigLine -> extractComponentDefinitionModel(childConfigLine, configFileName))
      .collect(Collectors.toList());
    componentModels.stream().forEach(componentDefinitionModel -> {
      builder.addChildComponentModel(componentDefinitionModel);
    });
    ConfigLine parent = configLine.getParent();
    if (parent != null && isConfigurationTopComponent(parent)) {
      builder.markAsRootComponent();
    }
    ComponentModel componentModel = builder.build();
    for (ComponentModel innerComponentModel : componentModel.getInnerComponents()) {
      innerComponentModel.setParent(componentModel);
    }
    return componentModel;
  }

  private boolean isConfigurationTopComponent(ConfigLine parent) {
    return (parent.getIdentifier().equals(MULE_ROOT_ELEMENT) || parent.getIdentifier().equals(MULE_DOMAIN_ROOT_ELEMENT) ||
      parent.getIdentifier().equals(POLICY_ROOT_ELEMENT));
  }
  //
  //private DslElementModel createIdentifiedElement(ComponentConfiguration configuration) {
  //
  //  final ComponentIdentifier identifier = configuration.getIdentifier();
  //
  //  Optional<Map.Entry<ExtensionModel, DslSyntaxResolver>> entry =
  //    resolvers.entrySet().stream()
  //      .filter(e -> e.getKey().getXmlDslModel().getNamespace().equals(identifier.getPrefix()))
  //      .findFirst();
  //
  //  if (!entry.isPresent()) {
  //    return null;
  //  }
  //
  //  currentExtension = entry.get().getKey();
  //  dsl = entry.get().getValue();
  //
  //
  //  Reference<DslElementModel> elementModel = new Reference<>();
  //  new ExtensionWalker() {
  //
  //    @Override
  //    protected void onConfiguration(ConfigurationModel model) {
  //      final DslElementSyntax elementDsl = dsl.resolve(model);
  //      getIdentifier(elementDsl).ifPresent(elementId -> {
  //        if (elementId.equals(identifier)) {
  //          DslElementModel.Builder<ConfigurationModel> element = createElementModel(model, elementDsl, configuration);
  //          addConnectionProvider(model, dsl, element, configuration);
  //          elementModel.set(element.build());
  //          stop();
  //        }
  //      });
  //
  //    }
  //
  //    @Override
  //    protected void onOperation(HasOperationModels owner, OperationModel model) {
  //      final DslElementSyntax elementDsl = dsl.resolve(model);
  //      getIdentifier(elementDsl).ifPresent(elementId -> {
  //        if (elementId.equals(identifier)) {
  //          elementModel.set(createElementModel(model, elementDsl, configuration).build());
  //          stop();
  //        }
  //      });
  //    }
  //
  //    @Override
  //    protected void onSource(HasSourceModels owner, SourceModel model) {
  //      final DslElementSyntax elementDsl = dsl.resolve(model);
  //      getIdentifier(elementDsl).ifPresent(elementId -> {
  //        if (elementId.equals(identifier)) {
  //          elementModel.set(createElementModel(model, elementDsl, configuration).build());
  //          stop();
  //        }
  //      });
  //    }
  //
  //  }.walk(currentExtension);
  //
  //  if (elementModel.get() == null) {
  //    resolveBasedOnTypes(configuration)
  //      .ifPresent(elementModel::set);
  //  }
  //
  //  return elementModel.get();
  //}

}
