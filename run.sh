rows=4
columns=4
imagePath="image.jpg"
n=$1
members=""

./gradlew installDist

if ! ./gradlew installDist; then
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
echo "$cmd"

(trap 'kill 0' SIGINT; eval "$cmd")
