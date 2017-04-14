require 'json'
require 'open-uri'
require 'nokogiri'

def parse_attrs(attrs_string)
  attrs_string.split(";").inject({}) do |attrs_map, attr|
    k, v = attr.split(":")
    attrs_map[k.strip] = v || ""
    attrs_map
  end
end

def parse_listing(product_tile)
  listing = {}

  # All of the Adidas product information lives in what they call a "hockeycard"
  hockeycard = product_tile.css('.hockeycard')

  # Get the SKU, colors, and size
  attrs_string = "#{hockeycard.attribute('data-context')}"
  listing = parse_attrs(attrs_string)

  # Get what we call taxonomies
  taxonomies_tag = hockeycard.css('.hidden')[0]
  taxonomies_string = "#{taxonomies_tag.attribute('data-context')}"
  listing[:taxonomies] = parse_attrs(taxonomies_string)

  # Everything else (image, name, PDP link) is on the inner card
  innercard = hockeycard.css('.innercard')

  # Get the image
  image_tag = innercard.css('.plp-image-bg-link img')
  listing_image = "#{image_tag.attribute('data-original')}"
  full_image = listing_image.split('?')[0]
  mobile_image = "#{image_tag.attribute('data-stackmobileview')}"
  title = "#{image_tag.attribute('title')}"
  alt = "#{image_tag.attribute('alt')}"
  listing[:image] = {
    :listing => listing_image,
    :full => full_image,
    :mobile => mobile_image,
    :title => title,
    :alt => alt
  }

  # Get the title and URL
  product_link = innercard.css('a.product-link')
  listing[:url] = "#{product_link.attribute('href')}"
  listing[:title] = product_link.css('.title').first.content
  listing[:subtitle] = product_link.css('.subtitle').first.content

  # Get the price
  price_tag = innercard.css('.product-info-price-rating .price')
  price_string = "#{price_tag.attribute('data-context')}"
  listing[:price] = parse_attrs(price_string)

  # Get the rating, if it exists
  rating_tag = innercard.css('.product-info-price-rating .rating')
  if rating_tag.length > 0
    rating_string = "#{rating_tag.attribute('data-context')}"
    listing[:rating] = parse_attrs(rating_string)
  end

  listing
end

def parse_page(gender, starting_position)
  url = "http://www.adidas.com/us/#{gender}?sz=120&start=#{starting_position}"
  doc = Nokogiri::HTML(open(url))
  doc.css('.product-tile').map { |tile| parse_listing(tile) }
end

page_size = 120
puts "Starting parser..."

puts "Parsing mens listings..."
mens_listings = (0..18).inject([]) do |full, page|
  puts "Parsing page number #{page + 1}..."
  page_listings = parse_page('men', page * page_size)
  full + page_listings
end

puts "Parsing womens listings..."
womens_listings = (0..9).inject([]) do |full, page|
  puts "Parsing page number #{page + 1}..."
  page_listings = parse_page('women', page * page_size)
  full + page_listings
end

puts "Parsing kids listings..."
kids_listings = (0..4).inject([]) do |full, page|
  puts "Parsing page number #{page + 1}..."
  page_listings = parse_page('kids', page * page_size)
  full + page_listings
end

listings = mens_listings.concat(womens_listings).concat(kids_listings)

puts "Successfully retrieved #{listings.length} listings..."
puts "Writing to listings.json..."

File.open('listings.json', 'w') do |f|
  f.puts listings.to_json
end 

puts "Complete!"
