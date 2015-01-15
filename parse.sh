#!/bin/bash
echo ">> Parsing...";
PARSER_DIRECTORY="../parser";
PARSER_DIRECTORY="../parser-dev";
CLASS=$(cat "parser_class")
java -Xms2048m -Xmx8192m -Dfile.encoding=UTF-8 \
-classpath \
bin:\
$PARSER_DIRECTORY/bin:\
$PARSER_DIRECTORY/lib/commons-csv-1.1.jar:\
$PARSER_DIRECTORY/lib/commons-lang3-3.1.jar \
$CLASS;
echo ">> Parsing... DONE";