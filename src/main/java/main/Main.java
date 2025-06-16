package main;

import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.VidaTransactionSubscription;
import io.pwrlabs.database.rocksdb.MerkleTree;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class Main {
    public static final long vidaId = 73746238;
    public static final long startingBlock = 1;
    public static final PWRJ pwrj = new PWRJ("https://pwrrrpc.pwrlabs.io/");
    private static List<String> peersToCheckRootHashWith;

    public static void main(String[] args) throws IOException {
        synchronize();
    }

    public static void synchronize() throws IOException {
        long blockToCheck = Database.getLastCheckedBlock() == 0 ? startingBlock : Database.getLastCheckedBlock();

        VidaTransactionSubscription sub = pwrj.subscribeToVidaTransactions(pwrj, vidaId, blockToCheck, Database::setLastCheckedBlock, txn -> {
            byte[] data = txn.getData();
            String senderHexAddress = txn.getSender().startsWith("0x") ? txn.getSender().substring(2) : txn.getSender();
            byte[] senderAddress = Hex.decode(senderHexAddress);
            JSONObject json = new JSONObject(new String(data));

            if(json.has("action")) {
                String action = json.getString("action");

                if(action.equalsIgnoreCase("transfer")) {
                    BigInteger amount = json.optBigInteger("amount", null);
                    String receiverHexAddress = json.optString("receiver", null);

                    if(amount != null && receiverHexAddress != null) {
                        receiverHexAddress = receiverHexAddress.startsWith("0x") ? receiverHexAddress.substring(2) : receiverHexAddress;
                        byte[] receiverAddress = Hex.decode(receiverHexAddress);

                        boolean success = Database.transfer(
                                senderAddress,
                                receiverAddress,
                                amount
                        );

                        if(success) {
                            System.out.println("Transfer successful: " + json.toString());
                        } else {
                            System.err.println("Transfer failed: " + json.toString());
                        }
                    } else {
                        System.err.println("Invalid transfer transaction: " + json.toString());
                    }
                }
            }
        });
    }

    public static void checkRootHashValidityAndSaveProgress(long blockNumber) {
        //check and validate that majority of peers have the same root hash for the given block number 2/3 are needed

        //save root hash
    }
}
