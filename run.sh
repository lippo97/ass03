members=""
for i in `seq 0 3`; do
    port=$(($i + 8080))
    m="$i:localhost:$port"
    members="$members;$m"
done
members=${members:1}

for i in `seq 0 3`; do
    gradle run --args "id $i members $members" &
done
# gradle run --args 'id $id members $members'
