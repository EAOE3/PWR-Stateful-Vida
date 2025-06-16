package api;

import io.pwrlabs.util.encoders.Hex;

import static spark.Spark.get;

public class GET {
    public static void run() {
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
    }
}
