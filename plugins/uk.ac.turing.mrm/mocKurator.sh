# Just wait a bit...
sleep 5

# ...then point to mock result file
full_path=$(realpath $0)
dir_path=$(dirname $full_path)
echo "${dir_path}/mockResult.json"