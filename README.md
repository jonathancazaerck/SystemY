[![Build Status](https://travis-ci.org/jonathancazaerck/SystemY.svg)](https://travis-ci.org/jonathancazaerck/SystemY)

# System Y

## Generating JARs
```bash
gradlew assemble
```

## Generating `.exe` for Windows
```bash
gradlew createExe
```
The `.exe` will be located in `build/launch4j`.
Make sure the `lib` folder stays in the same directory as the `.exe`.

## Generating `.app` for macOS
```bash
gradlew createApp
```
The `.app` will be located in `build/macApp`.

## Tests
```bash
gradlew check
```

## Starting the node

```bash
java -jar build/libs/node.jar <name> <ip/localhost> <port> [--gui]
```
Aliased to `bin/system-y-node` for Linux/macOS.

## Starting the nameserver

```bash
java -jar build/libs/nameserver.jar <ip/localhost>
```
Aliased to `bin/system-y-nameserver` for Linux/macOS.

## Development setup with IntelliJ IDEA

```bash
gradlew idea
```
