# PWR Stateful VIDA Example

This project demonstrates how to build a simple **stateful VIDA** on PWR Chain using the [PWRJ](https://github.com/pwrlabs/pwrj) library. It subscribes to transactions for a specific VIDA, maintains a local Merkle-tree based database and exposes a small HTTP API for querying the state.

## Requirements

- Java 21
- Maven (for building)

## Building

```bash
mvn package
```

## Running

```bash
java -jar target/PWR-Stateful-Vida-1.0-SNAPSHOT.jar
```

The application will start synchronizing transactions and expose HTTP endpoints on port `4567` by default:

- `GET /rootHash?blockNumber=123` – return the stored root hash for the given block.
- `GET /balance?address=0xabc...` – return the current balance for an address.

## Notes

The code is intentionally minimal for tutorial purposes. A real application should include more robust error handling and security considerations.
