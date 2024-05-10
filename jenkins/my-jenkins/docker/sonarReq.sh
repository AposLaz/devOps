
# Run Sbom Maven
# mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom

docker compose up -d

curl -X "PUT" "http://localhost:8081/api/v1/bom" \
     -H 'Content-Type: application/json' \
     -H 'X-API-Key: odt_XTrkhTWyBYhcyQqHCHCKE2G6wlWqAYjc' \
     -d $'{
  "project": "86648e5e-ba7f-4ed4-a10f-a2f84fcb0322",
  "bom": "@payload.json"
  }'

echo '{"project": "86648e5e-ba7f-4ed4-a10f-a2f84fcb0322", "bom": "'"$(cat ./target/bom.xml | base64 -w 0)"'"}' > payload.json
# cat ./target/bom.xml | base64 -w 0 > payload.json