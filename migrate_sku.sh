#!/bin/bash 
set -e

usage() { echo "Usage: $0 -h <hostname> -e <email> -p <password> -o <org>" 1>&2; exit 1; }

while getopts ":h:e:p:o:" o; do
    case "${o}" in
        h)
            HOSTNAME=${OPTARG}
            ;;
        e)
            EMAIL=${OPTARG}
            ;;
        p)
            PASSWORD=${OPTARG}
            ;;
        o)
            ORG=${OPTARG}
            ;;
        *)
            usage
            ;;
    esac
done
shift $((OPTIND-1))

if [ -z "${HOSTNAME}" ] || [ -z "${EMAIL}" ] || [ -z "${PASSWORD}" ] || [ -z "${ORG}" ]; then
    usage
fi

echo "HOSTNAME = ${HOSTNAME}"
echo "EMAIL = ${EMAIL}"
echo "PASSWORD = ${PASSWORD}"
echo "ORG = ${ORG}"

CHUNK_SIZE=100

#GENERATE new JWT
JWT=$(curl "https://$HOSTNAME/api/v1/public/login" -H "Content-Type: application/json" --data-binary "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"org\":\"$ORG\"}" --compressed --silent -D - | grep Jwt: | cut -d ' ' -f 2)

if [[ $JWT == "" ]]; then
  echo "Unable to authenticate"
  exit 1
fi

#GET SKUS
SKU_NUM=$(curl "https://$HOSTNAME/api/search/admin/sku_search_view/_search?from=0&size=1" -H "JWT:$JWT" --silent | jq -r ".pagination.total")

END=$(($SKU_NUM/$CHUNK_SIZE))
for i in $(seq 0 $END);
do
  echo "Iteration $i out of $END"
  START=$(($i*$CHUNK_SIZE))
  SKUS=$(curl "https://$HOSTNAME/api/search/admin/sku_search_view/_search?from=$START&size=$CHUNK_SIZE" -H "JWT:$JWT" --silent --compressed | jq -r ".result | .[] | .skuCode")

  for sku in $SKUS
  do
    echo "Trying to GET sku: $sku"
    sku_id=$(curl "https://$HOSTNAME/api/v1/skus/default/$sku" -H "JWT:$JWT" --silent --compressed | jq -r ".id")
    if [[ $sku_id -eq "null" ]]; then
      echo "SKU with code: $sku does not exist in Phoenix"
      continue
    fi 

    echo "Patching: $sku with id: $sku_id"
    curl "https://$HOSTNAME/api/v1/skus/default/$sku" -X PATCH -H "JWT:$JWT" -H 'Content-Type: application/json;charset=UTF-8' --data-binary "{\"id\":$sku_id, \"attributes\":{\"code\":{\"t\":\"string\",\"v\":\"$sku\"}}}" --compressed --silent
done
done
