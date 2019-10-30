package org.hyperledger.fabric.gateway;

public class TransactionEventResult {

    private final String txId;
    private long blockNum;
    private byte[] dataHash;
    private boolean isSucc;

    public TransactionEventResult(String txId) {
        this.txId = txId;
        isSucc = false;
    }

    public void setSuccess(boolean isSuccess) {this.isSucc=isSuccess;}
    public boolean isSuccess() {return isSucc;}
    public void setBlockNum(long blockNum) {this.blockNum = blockNum;}
    public long getBlockNum() {return blockNum;}
    public void setDataHash(byte[] dataHash) {this.dataHash = dataHash;}
    public byte[] getDataHash() {return dataHash;}

}
