# Elasticdump

Dumps specified Elasticsearch types to your local machine for debugging.

## Configuration

Please take a look at configuration options before proceeding:

```sh
SOURCE_URL=http://10.240.0.7:9200		# Staging URL
DESTINATION_URL=http://localhost:9200	# Local installation of Elasticsearch
INDEX_NAME=phoenix						# Specific index to dump
TYPE_NAMES=(							# Specific types to dump, others are ignored
	"products_search_view"
	"product_variants_search_view"
)
BACKUP_DIR=$PWD/backup 					# Backup directory, will be cleaned before and after proceeding
DUMP_FILENAME=data.json					# Dump filename suffix
```

## Credits

* [taskrabbit/elasticsearch-dump](https://github.com/taskrabbit/elasticsearch-dump)
