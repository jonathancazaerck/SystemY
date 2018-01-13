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
Make sure the `lib` folder is in the same directory as the `.exe`.

## Generating `.app` for macOS
```bash
gradlew createApp
```

## Tests
```bash
gradlew check
```

## Starting the node

```bash
java -jar build/libs/node.jar <name> <localhost/IP> <poortnummer>
```

```bash
java -jar build/libs/node.jar --gui
```

```bash
bin/system-y-node bin/system-y-node # Linux/macOS only
```

## Starting the nameserver

```bash
java -jar build/libs/nameserver.jar <localhost/IP>
```

```bash
bin/system-y-nameserver # Linux/macOS only
```

## Development setup with IntelliJ IDEA

```bash
gradlew idea
```
