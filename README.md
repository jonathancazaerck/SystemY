# ds3

## Klassen

- NameServer
  - bevat een MAP met integer naar IP-adressen
  - Map opslaan op hdd als JSON (XML als het echt niet anders kan)
- Node
- File
  - hash()
  - naam
  - path
  
## RMIregistry uitvoeren
Het is belangrijk om RMIregistry in de juiste map uit te voeren!

```bash
cd out/production/ds3
rmiregistry
```

## Nameserver uitvoeren

```bash
java -jar -Djava.net.preferIPv4Stack=true -Djava.rmi.server.hostname=<rmi server ip> out/artifacts/nameserver_jar/nameserver.jar
```

## Node uitvoeren

```bash
java -jar -Djava.net.preferIPv4Stack=true -Djava.rmi.server.hostname=<rmi server ip> out/artifacts/node_jar/node.jar <hostname> <localhost/IP> <poortnummer>
```