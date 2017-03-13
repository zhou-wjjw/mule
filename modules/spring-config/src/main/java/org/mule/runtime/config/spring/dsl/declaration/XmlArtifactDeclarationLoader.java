/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.declaration;

import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.dsl.DslResolvingContext;

/**
 * //TODO
 */
public interface XmlArtifactDeclarationLoader {

  ArtifactDeclaration load(String configFile, DslResolvingContext context);

}
