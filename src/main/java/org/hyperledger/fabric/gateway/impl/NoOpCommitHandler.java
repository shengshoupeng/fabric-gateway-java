/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.TransactionEventResult;
import org.hyperledger.fabric.gateway.spi.CommitHandler;

import java.util.concurrent.TimeUnit;

public enum NoOpCommitHandler implements CommitHandler {
    INSTANCE;

    @Override
    public void startListening() { }

    @Override
    public void waitForEvents(final long timeout, final TimeUnit timeUnit) { }

    @Override
    public void cancelListening() { }

    @Override
    public TransactionEventResult eventResult() { return null;}
}
