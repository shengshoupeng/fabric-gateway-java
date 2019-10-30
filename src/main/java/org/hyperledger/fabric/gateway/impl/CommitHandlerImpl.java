/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TransactionEventResult;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitListener;
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;

public final class CommitHandlerImpl implements CommitHandler {
    private final String transactionId;
    private final Network network;
    private final CommitStrategy strategy;
    private final CommitListener listener = new CommitListener() {
        @Override
        public void acceptCommit(final BlockEvent.TransactionEvent transactionEvent) {
            onTxEvent(transactionEvent);
        }

        @Override
        public void acceptDisconnect(final PeerDisconnectEvent disconnectEvent) {
            onDisconnectEvent(disconnectEvent);
        }
    };
    private final Set<Peer> peers;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<ContractException> error = new AtomicReference<>();

    private TransactionEventResult eventResult;

    public CommitHandlerImpl(String transactionId, Network network, CommitStrategy strategy) {
        this.transactionId = transactionId;
        this.network = network;
        this.strategy = strategy;
        this.peers = Collections.synchronizedSet(new HashSet<>(strategy.getPeers()));
        eventResult = new TransactionEventResult(transactionId);
    }

    @Override
    public void startListening() {
        if (peers.isEmpty()) {
            cancelListening();
        } else {
            network.addCommitListener(listener, peers, transactionId);
        }
    }

    @Override
    public void waitForEvents(final long timeout, final TimeUnit timeUnit) throws ContractException, TimeoutException, InterruptedException {
        try {
            boolean complete = latch.await(timeout, timeUnit);
            if (!complete) {
                throw new TimeoutException("Timeout waiting for commit of transaction " + transactionId);
            }
        } finally {
            cancelListening();
        }

        ContractException cause = error.get();
        if (cause != null) {
            throw cause;
        }
    }

    @Override
    public void cancelListening() {
        latch.countDown();
        network.removeCommitListener(listener);
        peers.clear();
    }

    @Override
    public String toString() {
        return GatewayUtils.toString(this,
                "transactionId=" + transactionId,
                "strategy=" + strategy,
                "peers=" + peers);
    }

    private void onTxEvent(final BlockEvent.TransactionEvent event) {
        if (!transactionId.equals(event.getTransactionID()) || !peers.remove(event.getPeer())) {
            // Not a transaction or peer we were looking for
            return;
        }

        if (event.isValid()) {
            CommitStrategy.Result result = strategy.onEvent(event);
            eventResult.setBlockNum(event.getBlockEvent().getBlockNumber());
            eventResult.setDataHash(event.getBlockEvent().getDataHash());
            processStrategyResult(result);
        } else {
            String peerName = event.getPeer().getName();
            fail(new ContractException("Transaction commit was rejected by peer " + peerName));
        }
    }

    private void onDisconnectEvent(final PeerDisconnectEvent event) {
        if (!peers.remove(event.getPeer())) {
            // Not a peer we were looking for
            return;
        }

        CommitStrategy.Result result = strategy.onError(event);
        processStrategyResult(result);
    }

    private void processStrategyResult(final CommitStrategy.Result strategyResult) {
        if (strategyResult == CommitStrategy.Result.SUCCESS) {
            eventResult.setSuccess(true);
            cancelListening();
        } else if (strategyResult == CommitStrategy.Result.FAIL) {
            fail(new ContractException("Commit strategy failed"));
        }
    }

    private void fail(final ContractException e) {
        error.set(e);
        cancelListening();
    }


    @Override
    public TransactionEventResult eventResult(){
        return eventResult;
    }
}
