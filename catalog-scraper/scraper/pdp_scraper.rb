require 'json'
require 'open-uri'
require 'nokogiri'

def sanitize_content(str)
  str.tr("\n", "").tr("\t", "")
end

def parse_page(gender, starting_position)
  url = "http://www.adidas.com/us/#{gender}?sz=120&start=#{starting_position}"
  doc = Nokogiri::HTML(open(url))
  doc.css('.product-tile').map { |tile| parse_listing(tile) }
end

def extract_image(image_tag)
  thumbnail = "#{image_tag.attribute('src')}"
  full_image = thumbnail.split('?')[0]
  title = "#{image_tag.attribute('title')}"
  alt = "#{image_tag.attribute('alt')}"
  { title: title, alt: alt, thumbnail: thumbnail, full: full_image }
end

def parse_pdp(url)
  doc = Nokogiri::HTML(open(url))
  pdp = {}

  # Get all of the product images.
  pdp[:images] = doc.css("li.pdp-image-carousel-item img").map { |i| extract_image(i) }

  # Get the size options
  pdp[:options] = {}
  size_options = doc.css('.size-select option').map do |option_tag|
    {
      :sku => option_tag.attribute('value').to_s,
      :max_available => option_tag.attribute('data-maxorderqty').to_s.to_i,
      :max_order_qty => option_tag.attribute('data-maxorderqty').to_s.to_i,
      :inventory_status => option_tag.attribute('data-status').to_s,
      :size => sanitize_content(option_tag.content)
    }
  end

  pdp[:options][:size] = size_options[1..size_options.length - 1]

  # Get the color options
  color_options = doc.css('.color-variations-thumb-color').map do |option_tag|
    link_tag = option_tag.css('a').first
    image_tag = link_tag.css('img').first

    {
      :sku => option_tag.attribute('data-sku').to_s,
      :inventory_status => option_tag.attribute('data-instock').to_s,
      :color => link_tag.attribute('title').to_s,
      :swatch => extract_image(image_tag)
    }
  end

  pdp[:options][:color] = color_options[1..color_options.length - 1]

  # Get the product description
  description_block = doc.css('.product-segment.ProductDescription')
  short_description = description_block.css('h4').first
  pdp[:short_description] = sanitize_content(short_description.content) unless short_description == nil

  description = description_block.css('.prod-details').first
  full_description = description_block.css('.prod-details-full').first
  if description != nil
    pdp[:description] = sanitize_content(description.content)
  elsif full_description != nil
    pdp[:description] = sanitize_content(full_description.content)
  end

  description_list = description_block.css('ul.bullets_list').first
  unless description_list == nil
    pdp[:description_list] = sanitize_content(description_list.inner_html)
  end

  pdp_category = doc.css('.pdp-category-in').first
  unless pdp_category == nil
    pdp[:pdp_category] = sanitize_content(pdp_category.content)
  end

  pdp
end

puts "Starting parser..."

puts "Reading contents of listings.json..."
listing_file = File.open("listings.json", "r")
listings = JSON.parse(listing_file.read)

listing_count = listings.length
puts "There are #{listing_count} products to get data for..."

products = listings.map.with_index do |listing, idx|
  puts "Processing listing #{idx + 1} of #{listing_count}..."

  url = listing['url']
  puts "Grabbing data from #{url}..."
  listing[:details] = parse_pdp(url)

  listing
end

puts "Writing to products.json..."

File.open('products.json', 'w') do |f|
  f.puts products.to_json
end

puts "Complete!"
