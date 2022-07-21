#!/bin/sh
rm -rf ./ide-server/plugins
rm -rf ./ide-server/lib
rm -rf ./ide-server/jbr
ln -s /opt/plugins/ ./ide-server/plugins
ln -s /opt/lib/ ./ide-server/lib
ln -s /opt/jbr/ ./ide-server/jbr
/bin/sh ./ide-server/bin/idea.sh
