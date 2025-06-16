package main;

import io.pwrlabs.database.rocksdb.MerkleTree;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class Database {
    private static MerkleTree database;

    static {
        try {
            database = new MerkleTree("database");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //add a shutdown hook to close the database
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (database != null) {
                    database.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public static BigInteger getBalance(byte[] address) {
        try {
            // Retrieve the balance from the database
            byte[] data = database.getData(address);
            if (data == null) {
                return BigInteger.ZERO; // Return zero if no data found
            }
             else {
                 return new BigInteger(1, database.getData(address));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return BigInteger.ZERO; // Return zero if an error occurs
        }
    }

    private static boolean setBalance(byte[] address, BigInteger balance) {
        try {
            database.addOrUpdateData(address, balance.toByteArray());
            return true; // Return true if the operation was successful
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Return false if an error occurs
        }
    }

    public static boolean transfer(byte[] sender, byte[] receiver, BigInteger amount) {
        try {
            // Perform the transfer logic here
            // For example, update the balances in the database

            // Assuming we have methods to get and set balances
            BigInteger senderBalance = getBalance(sender);
            BigInteger receiverBalance = getBalance(receiver);

            if (senderBalance.compareTo(amount) < 0) {
                return false; // Insufficient funds
            }

            // Deduct amount from sender's balance
            senderBalance = senderBalance.subtract(amount);
            // Add amount to receiver's balance
            receiverBalance = receiverBalance.add(amount);

            // Update the balances in the database
            setBalance(sender, senderBalance);
            setBalance(receiver, receiverBalance);

            return true; // Transfer successful
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Transfer failed
        }
    }

    public static long getLastCheckedBlock() {
        try {
            byte[] data = database.getData("lastCheckedBlock".getBytes());
            if (data == null) {
                return 0; // Return zero if no data found
            } else {
                return ByteBuffer.wrap(data).getLong(); // Convert byte array to long
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Return zero if an error occurs
        }
    }

    public static Void setLastCheckedBlock(long block) {
        try {
            database.addOrUpdateData("lastCheckedBlock".getBytes(), ByteBuffer.allocate(Long.BYTES).putLong(block).array());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return null; // Return null as the method signature requires a Void return type
        }
    }

    public static void setBlockRootHash(long blockNumber, byte[] rootHash) {
        try {
            database.addOrUpdateData(("blockRootHash_" + blockNumber).getBytes(), rootHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] getBlockRootHash(long blockNumber) {
        try {
            return database.getData(("blockRootHash_" + blockNumber).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null if an error occurs
        }
    }
}
