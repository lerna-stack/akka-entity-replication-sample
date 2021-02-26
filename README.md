# sample-akka-entity-replication

This is an example project showing how to use [akka-entity-replication](https://github.com/lerna-stack/akka-entity-replication).

## Usage

To start cluster, run following commands on separated terminals.

```bash
sbt --batch runNode1
```

```bash
sbt --batch runNode2
```

```bash
sbt --batch runNode3
```

You can run the following command to see application behavior.

```bash
bin/demo-request.sh 100 # An account No must be pass!
```
