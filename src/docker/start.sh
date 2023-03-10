#!/bin/sh

# Forward SIGTERM
_term() {
    echo "Forward SIGTERM to tinyMediaManager ($pid)"
    kill -TERM "$pid"
    wait "$pid"
}

# Forward SIGINT
_int() {
    echo "Forward SIGINT to tinyMediaManager ($pid)"
    kill -INT "$pid"
    wait "$pid"
}

# Capture SIGTERM and SIGINT so we can forward them to the Java process
trap _term TERM
trap _int INT

sleep 2s
/app/tinyMediaManager &
sleep 10s
pid=$(pidof java)
wait $pid
