#!/bin/bash
CMD=${1:-info}
CONFIG_FILE=${2:-`pwd`/src/main/resources/flyway/conf/flyway.conf}

# To debug, use this line instead...
# mvn flyway:${CMD} -X -Dflyway.configFile=$CONFIG_FILE

mvn "flyway:$CMD" -Dflyway.configFiles=${CONFIG_FILE}
