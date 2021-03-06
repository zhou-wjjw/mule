/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

/**
 * <code>ServerNotificationListener</code> is an observer interface that objects can implement and register themselves with the
 * Mule Server to receive notifications when the server, model and components stop, start, initialise, etc.
 */
public interface ServerNotificationListener<T extends ServerNotification> {

  /**
   * @return true if this listener is expected to perform blocking I/O operations, false otherwise.
   */
  default boolean isBlocking() {
    return true;
  }

  void onNotification(T notification);
}
