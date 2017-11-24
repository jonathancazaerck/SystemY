[![Build Status](https://travis-ci.org/jonathancazaerck/ds3.svg?branch=replicateFiles)](https://travis-ci.org/jonathancazaerck/ds3)

# System Y

## Klassen

- NameServer
  - bevat een TreeMap die hashes naar IP-adressen mapt
  - Slaat map op als JSON op harde schijf
- Node
  
## Genereren van JAR
```bash
gradle assemble
```

## Tests
```bash
gradle check
```

## Nameserver uitvoeren

```bash
java -jar build/libs/nameserver.jar <localhost/IP>
```

De nameserver start ook een RMI registry.

## Node uitvoeren

```bash
java -jar build/libs/node.jar <name> <localhost/IP> <poortnummer>
```

## Development setup

```bash
gradle idea
```
