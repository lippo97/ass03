#!/usr/bin/env sh

if [[ $# -lt 3 ]]; then
    exit 2;
fi

rows=$2
columns=$3
imagePath="image.jpg"
n=$(($1 - 1))
members=""

if ! ./gradlew installDist >/dev/null; then
	  exit 1
fi

for i in $(seq 0 "$n"); do
    port=$((i + 8080))
    m="$i:localhost:$port"
    members="$members;$m"
done
members=${members:1}

cmd=""
for i in $(seq 0 "$n"); do
    cmd="$cmd & build/install/ass03/bin/ass03 id $i members \"$members\" imagePath $imagePath rows $rows columns $columns"
done
cmd=${cmd:3}

(trap 'kill 0' SIGINT; eval "$cmd")
