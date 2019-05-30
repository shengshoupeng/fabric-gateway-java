package org.hyperledger.fabric.gateway;

public class TransactionResult {
    private String txId;
    /*Payload*/
    private byte[] result;
    /*Event结果，包含blockNum，hash等信息 */
    private TransactionEventResult eventResult;

    public TransactionResult(String txId) {
        this.txId = txId;
    }
    public String getTxId() {
        return txId;
    }

    public void setResult(byte[] result) {
        this.result=result;
    }

    public byte[] getResult() {
        return result;
    }

    public void setEventResult(TransactionEventResult eventResult) {
        this.eventResult = eventResult;
    }

    public TransactionEventResult getEventResult() {
        return eventResult;
    }
}
