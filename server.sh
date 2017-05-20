#!/bin/bash

type fswatch &> /dev/null
if (( $? )); then
	echo "The fswatch command is required. Try running 'brew install fswatch'"
	exit 1
fi

set -e

pid=0
serving=1

function cleanup {
	(( $pid )) && kill $pid
}

trap cleanup EXIT

function quit {
	echo ">>> Quitting..."
	serving=0
}

trap quit INT

while (( $serving )); do
	echo ">>> Serving..."
	mvn package exec:java &
	pid=$!
	fswatch -1 src/main
	kill $pid
done
