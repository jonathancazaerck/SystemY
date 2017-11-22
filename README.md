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
  
## Genereren van JAR door gebruik te maken van Gradle
```bash
graddle idea
graddle assemble
```
  
## RMIregistry uitvoeren
Het is belangrijk om RMIregistry in de juiste map uit te voeren!

```bash
cd out/production/ds3
rmiregistry
```

## Nameserver uitvoeren

```bash
java -jar build/libs/nameserver.jar <localhost/IP>
```

## ds3.Node uitvoeren

```bash
java -jar build/libs/node.jar <name> <localhost/IP> <poortnummer>
```
