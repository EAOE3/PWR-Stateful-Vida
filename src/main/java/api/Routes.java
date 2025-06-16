package api;

import io.pwrlabs.util.encoders.Hex;

import java.math.BigInteger;

import static spark.Spark.get;

/**
 * Defines the HTTP routes exposed by this example application.
 */
public class Routes {

    /**
     * Start all HTTP GET routes.
     */
    public static void start() {
        // Returns the stored Merkle root hash for a specific block number.
        get("/rootHash", (request, response) -> {
            try {
                long blockNumber = Long.parseLong(request.queryParams("blockNumber"));

                byte[] rootHash = main.Database.getBlockRootHash(blockNumber);
                if (rootHash != null) {
                    return Hex.toHexString(rootHash);
                } else {
                    return "";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        });

        // Returns the current balance for the given address (hex encoded).
        get("/balance", (request, response) -> {
            try {
                String addressHex = request.queryParams("address");
                if (addressHex == null) {
                    response.status(400);
                    return "address query parameter is required";
                }
                addressHex = addressHex.startsWith("0x") ? addressHex.substring(2) : addressHex;
                byte[] address = Hex.decode(addressHex);
                BigInteger balance = main.Database.getBalance(address);
                return balance.toString();
            } catch (Exception e) {
                e.printStackTrace();
                response.status(500);
                return "error";
            }
        });
    }
}
