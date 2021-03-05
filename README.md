# sample-akka-entity-replication

This is an example project showing how to use [akka-entity-replication](https://github.com/lerna-stack/akka-entity-replication).

## Usage

### Run with docker-compose

Create this app docker image.

```bash
sbt --batch docker:publishLocal
```

To start cluster, run following a command.

```bash
docker-compose up -d
```

You can watch logs of running services.

```
docker-compose logs -f --tail=10 node1
```
```
docker-compose logs -f --tail=10 node2
```
```
docker-compose logs -f --tail=10 node3
```
```
docker-compose logs -f --tail=10 cassandra
```
```
docker-compose logs -f --tail=10 haproxy
```

You can run the following command to see application behavior.

```bash
bin/demo-request.sh 100 # An account No must be pass!
```

### Run with sbt

To start cluster, run following commands on separated terminals.

```bash
docker-compose up -d cassandra
```

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
