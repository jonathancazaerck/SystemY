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

## Generating `.app` for macOS
```bash
gradlew createApp
```

## Tests
```bash
gradlew check
```

## Starting the nameserver

```bash
java -jar build/libs/nameserver.jar <localhost/IP>
```

## Starting the node

```bash
java -jar build/libs/node.jar <name> <localhost/IP> <poortnummer>
```

## Development setup with IntelliJ IDEA

```bash
gradlew idea
```
