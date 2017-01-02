#!/usr/bin/env bash

CATEGORIES=(shoes pants gadget mug cup chair glasses tv computer headphones electric toy)

 ./find_products_on_ebay.pl title ${CATEGORIES[*]} | sort -u | shuf >> product_titles.txt
 ./find_products_on_ebay.pl subtitle ${CATEGORIES[*]} | sort -u | shuf >> product_subtitles.txt
