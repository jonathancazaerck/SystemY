# ds3

## Klassen

- ds3.NameServer
  - bevat een MAP met integer naar IP-adressen
  - Map opslaan op hdd als JSON (XML als het echt niet anders kan)
- ds3.Node
- ds3.File
  - hash()
  - naam
  - path
  
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

## ds3.Node uitvoeren

```bash
java -jar build/libs/node.jar <name> <localhost/IP> <poortnummer>
```

## Development

```bash
gradle idea
```
