#!/bin/sh
rm -rf ./ide-server/plugins
rm -rf ./ide-server/lib
rm -rf ./ide-server/jbr
ln -s ../ide-plugins/plugins/ ./ide-server/plugins
ln -s ../ide-lib/lib/ ./ide-server/lib
ln -s ../ide-jbr/jbr/ ./ide-server/jbr
/bin/sh ./ide-server/bin/idea.sh
