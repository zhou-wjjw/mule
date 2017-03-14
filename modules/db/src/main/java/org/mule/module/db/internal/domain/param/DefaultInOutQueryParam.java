/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.param;

import org.mule.module.db.internal.domain.type.DbType;

public class DefaultInOutQueryParam extends AbstractQueryParam implements InOutQueryParam
{

    private final Object value;

    private final boolean hasValue;

    public DefaultInOutQueryParam(int index, DbType type, String name, Object value)
    {
        this(index,type, name, value, value != null);
    }

    private DefaultInOutQueryParam(int index, DbType type, String name, Object value, boolean hasValue)
    {
        super(index, type, name);
        this.hasValue = hasValue;
        this.value = value;
    }

    @Override
    public Object getValue()
    {
        return value;
    }

    @Override
    public boolean hasValue()
    {
        return hasValue;
    }
}
