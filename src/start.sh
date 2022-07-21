#!/bin/sh
ln -s /opt/plugins/ ./ide-server/plugins
ln -s /opt/lib/ ./ide-server/lib
ln -s /opt/jbr/ ./ide-server/jbr
/bin/sh ./ide-server/bin/idea.sh
