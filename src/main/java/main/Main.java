package main;

import com.github.pwrlabs.pwrj.entities.FalconTransaction;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.VidaTransactionSubscription;
import io.pwrlabs.util.encoders.BiResult;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONObject;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for synchronizing VIDA transactions with the local Merkle-backed database.
 */
public final class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final long VIDA_ID = 73_746_238L;
    private static final long START_BLOCK = 1L;
    private static final PWRJ PWRJ_CLIENT = new PWRJ("https://pwrrrpc.pwrlabs.io/");
    private static List<String> peersToCheckRootHashWith;
    private static VidaTransactionSubscription subscription;

    public static void main(String[] args) {
        try {
            initializePeers(args);
            long lastBlock = DatabaseService.getLastCheckedBlock();
            long fromBlock = (lastBlock > 0) ? lastBlock : START_BLOCK;
            subscribeAndSync(fromBlock);
        } catch (IOException | RocksDBException e) {
            LOGGER.log(Level.SEVERE, "Initialization failed", e);
        }
    }

    /**
     * Initializes peer list from arguments or defaults.
     * @param args command-line arguments; if present, each arg is a peer hostname
     */
    private static void initializePeers(String[] args) {
        if (args != null && args.length > 0) {
            peersToCheckRootHashWith = Arrays.asList(args);
            LOGGER.info("Using peers from args: " + peersToCheckRootHashWith);
        } else {
            peersToCheckRootHashWith = List.of(
                    "peer1.example.com",
                    "peer2.example.com",
                    "peer3.example.com"
            );
            LOGGER.info("Using default peers: " + peersToCheckRootHashWith);
        }
    }

    private static void subscribeAndSync(long fromBlock) throws IOException, RocksDBException {
        //The subscription to VIDA transactions has a built in shutdwown hook
        subscription =
                PWRJ_CLIENT.subscribeToVidaTransactions(
                        PWRJ_CLIENT,
                        VIDA_ID,
                        fromBlock,
                        Main::onChainProgress,
                        Main::processTransaction
                );
    }

    private static Void onChainProgress(long blockNumber) {
        try {
            DatabaseService.setLastCheckedBlock(blockNumber);
            checkRootHashValidityAndSave(blockNumber);
            LOGGER.info("Checkpoint updated to block " + blockNumber);
        } catch (RocksDBException e) {
            LOGGER.log(Level.WARNING, "Failed to update last checked block: " + blockNumber, e);
        } finally {
            return null;
        }
    }

    private static void processTransaction(FalconTransaction.PayableVidaDataTxn txn) {
        try {
            JSONObject json = new JSONObject(new String(txn.getData(), StandardCharsets.UTF_8));
            String action = json.optString("action", "");
            if ("transfer".equalsIgnoreCase(action)) {
                handleTransfer(json, txn.getSender());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing transaction: " + txn.getTransactionHash(), e);
        }
    }

    private static void handleTransfer(JSONObject json, String senderHex) throws RocksDBException {
        BigInteger amount = json.optBigInteger("amount", null);
        String receiverHex = json.optString("receiver", null);
        if (amount == null || receiverHex == null) {
            LOGGER.warning("Skipping invalid transfer: " + json);
            return;
        }

        byte[] sender = decodeHexAddress(senderHex);
        byte[] receiver = decodeHexAddress(receiverHex);

        boolean success = DatabaseService.transfer(sender, receiver, amount);
        if (success) {
            LOGGER.info("Transfer succeeded: " + json);
        } else {
            LOGGER.warning("Transfer failed (insufficient funds): " + json);
        }
    }

    private static byte[] decodeHexAddress(String hex) {
        String clean = hex.startsWith("0x") ? hex.substring(2) : hex;
        return Hex.decode(clean);
    }

    private static void checkRootHashValidityAndSave(long blockNumber) {
        try {
            byte[] localRoot = DatabaseService.getRootHash();
            int peersCount = peersToCheckRootHashWith.size();
            long quorum = (peersCount * 2) / 3 + 1;
            int matches = 0;
            for (String peer : peersToCheckRootHashWith) {
                // TODO: fetch peer root via RPC and compare
                BiResult<Boolean /**/, byte[]> response = fetchPeerRootHash(peer, blockNumber);
                if(response.getFirst()) {
                    if(Arrays.equals(response.getSecond(), localRoot)) {
                        matches++;
                    }
                } else {
                    --peersCount;
                    quorum = (peersCount * 2) / 3 + 1;
                }

                if (matches >= quorum) {
                    DatabaseService.setBlockRootHash(blockNumber, localRoot);
                    LOGGER.info("Root hash validated and saved for block " + blockNumber);
                    return;
                }
            }

            LOGGER.severe("Root hash mismatch: only " + matches + "/" + peersToCheckRootHashWith.size());
            //Revert changes and reset block to reprocess the data
            DatabaseService.revertUnsavedChanges();
            subscription.setLatestCheckedBlock(DatabaseService.getLastCheckedBlock());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying root hash at block " + blockNumber, e);
        }
    }

    private static BiResult<Boolean /*Replied*/, byte[]> fetchPeerRootHash(String peer, long blockNumber) {
        try {
            return new BiResult<>(true, new byte[0]);
        } catch (Exception e) {
            return new BiResult<>(false, new byte[0]);
        }
    }
}
