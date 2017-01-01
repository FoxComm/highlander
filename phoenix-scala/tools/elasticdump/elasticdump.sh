#!/usr/bin/env bash
# A script to partially dump Elasticsearch index to local machine for debugging

# Configuration
SOURCE_URL=http://10.240.0.7:9200
DESTINATION_URL=http://localhost:9200
INDEX_NAME=phoenix
TYPE_NAMES=(
	"products_search_view"
	"sku_search_view"
)
BACKUP_DIR=$PWD/backup
DUMP_FILENAME=data.json

# Prepare
echo '⇒ Installing elasticdump if necessary...'
npm install --silent -g elasticdump

echo '⇒ Deleting Elasticsearch index if necessary...'
curl -XDELETE $DESTINATION_URL/$INDEX_NAME --silent > /dev/null

echo '⇒ Creating Elasticsearch index with specific settings...'
curl -XPUT $DESTINATION_URL/$INDEX_NAME -d @index_settings.json \
	--header "Content-Type: application/json" \
	--silent > /dev/null

echo '⇒ Creating backup directory if necessary...'
rm -rf $BACKUP_DIR/*
mkdir -p $BACKUP_DIR

# Export
echo '⇒ Exporting mappings...'
elasticdump \
	--input=$SOURCE_URL/$INDEX_NAME \
	--output=$DESTINATION_URL/$INDEX_NAME \
	--type=mapping

for TYPE_NAME in "${TYPE_NAMES[@]}"
do
	echo "⇒ Exporting data for type $TYPE_NAME..."
	elasticdump \
		--input=$SOURCE_URL \
		--input-index=$INDEX_NAME/$TYPE_NAME \
		--output=$BACKUP_DIR/$TYPE_NAME.$DUMP_FILENAME \
		--type=data
done

# Import
for TYPE_NAME in "${TYPE_NAMES[@]}"
do
	echo "⇒ Importing data for type $TYPE_NAME..."
	elasticdump \
		--input=$BACKUP_DIR/$TYPE_NAME.$DUMP_FILENAME \
		--output=$DESTINATION_URL/$INDEX_NAME \
		--type=data
done

# Clean
echo '⇒ Cleaning backup directory...'
rm -rf $BACKUP_DIR
