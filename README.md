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

## ds3.Node uitvoeren

```bash
java -jar -Djava.net.preferIPv4Stack=true -Djava.rmi.server.hostname=<rmi server ip> out/artifacts/node_jar/node.jar <hostname> <localhost/IP> <poortnummer>
```