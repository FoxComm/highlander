require "nokogiri"
require "open-uri"
require "json"
require "yaml"

def parse_taxon_hierarcy(values)
  values.map do |value|
    result = {'attributes': {'name':{'t':'string', 'v':value['name']}}}
    if value['swatch'] != nil
      result[:attributes][:swatch]= {'t':'swatch', 'v':value['swatch']}
    end
    if value['values'] != nil
      result[:children] = parse_taxon_hierarcy(value['values'])
    end
    result
  end
end

def parse_hierarchical_taxonomy(name, values)
  result = {'attributes': {'name':{'t':'string', 'v':name}}}
  taxons = parse_taxon_hierarcy(values)
  result[:taxons] = taxons
  result[:hierarchical] = taxons.any?{|i| i['children']!=nil}
  result
end

def parseTaxonomies()
  data = YAML.load_file("./raw/taxonomies.yml")
  result = []
  data.each do |yaml_taxonomy|
    name = yaml_taxonomy['name']
    result = result << parse_hierarchical_taxonomy(name, yaml_taxonomy['values'])
  end
  puts "Writing to json"

  File.open('taxonomies.json', 'w') do |f|
    f.puts result.to_json
  end
end

parseTaxonomies

def sanitize_content(str)
  str.tr("\n", "").tr("\t", "").gsub("\r", "").strip.gsub(/[\u0080-\u00ff]/, "")
end

def collect_pdps()
  files = ['luggage.html', 'backpacks.html', 'accessories.html', 'bags.html']
  pdp_links = []
  files.each do |file|
    filename = "./raw/#{file}"
    doc = File.open(filename) { |f| Nokogiri::HTML(f) }
    links = doc.css('.productMainLink > a').map { |l| l.attribute('href').to_s }
    pdp_links = pdp_links + links
  end

  pdp_links.sort { |x, y| x <=> y }.uniq
end

def read_images(doc)
  doc.css("div.cntr-product-alternate-items").css("img").map do |image_tag|
    thumbnail = "#{image_tag.attribute('src')}"
    full_image = thumbnail.split('?')[0]
    title = "#{image_tag.attribute('title')}"
    alt = "#{image_tag.attribute('alt')}"
    { title: title, alt: alt, thumb: thumbnail, src: full_image }
  end
end

def parse_color_option(li_content)
  # <a role="button" aria-pressed="false" href="/p/13%22-slim-solutions-laptop-cover-0114250DR" name="/p/13%22-slim-solutions-laptop-cover-0114250DR" rel="Black/Red">
  a = li_content.css('a')
  url = "#{a.attribute('href')}"
  name = "#{a.attribute('name')}"

  # <img src="https://tumi.scene7.com/is/image/Tumi/114250DR_sw" title="Black/Red" alt="">
  img = li_content.css('img')
  img_src = "#{img.attribute('src')}"
  title = "#{img.attribute('title')}"
  {url:url, name:name, img:img_src, variant_name:title}
end

def read_current_pdp_color(doc)
  doc.css("ul.choose-colors li").each do |li|
    current = (!li.attribute('class').nil? && (li['class'].include? 'select'))
    parsed = parse_color_option(li)
    if current
      return parsed
    end
  end

  nil
end

def read_current_pdp_size(doc, url)
  doc.css("select.select-size option").each do |size|
    value = size.attribute('value').to_s
    if value == url
      variant_name = sanitize_content(size.content.split(",")[0])[4..-1]
      return {
        url: value,
        name: value,
        variant_name: variant_name
      }
    end
  end

  return nil
end

def read_innovation_features(doc)
  doc.css(".innovation-details .innovation-slider-clc").map do |innov|
    title = sanitize_content(innov.css('h3').text)
  end
end

def update_sku_from_scopes(doc, sku)
  script_tags = doc.css("script")

  # Get the lpvars contents
  lp_scripts = script_tags.select do |st|
    st.text.include? "var arrLPvars"
  end

  scopes = lp_scripts[0].text.scan(/{ scope:'page', name:'([a-zA-Z0-9_\-\. ]+)', value:'([a-zA-Z0-9_\-\. ]+)' }/)

  scopes.each do |s|
    if s[0] == "ProductName"
      sku[:attributes][:name] = {
        t: "string",
        v: s[1]
      }
    elsif s[0] == "ProductPrice"
      sku[:attributes][:salePrice] = {
        t: "price",
        v: {
          currency: "USD",
          value: (s[1].to_f * 100).to_i
        }
      }
    elsif s[0] == "ProductCategories"
      sku[:taxons][:productType] = [ s[1] ]
    end
  end

  sku
end

def update_sku_from_data_properties(doc, sku)
  script_tags = doc.css("script")

  data_scripts = script_tags.select do |st|
    st.text.include? "dataLayer = [{"
  end

  props = data_scripts[0].text.scan(/'([a-zA-Z0-9_]+)':( )?('(.)*'|\[\]|[0-9\.\-]+|true|false)/)

  props.each do |d|
    if d[0] == "productName"
      sku[:attributes][:title] = {
        t: "string",
        v: d[2].gsub("'", "")
      }
    elsif d[0] == "productSubCat"
      sku[:taxons][:category] = [ d[2].gsub("'", "") ]
    elsif d[0] == "productDescription"
      sku[:attributes][:description] = {
        t: "richtext",
        v: d[2].gsub("'", "")
      }
    elsif d[0] == "product_sku_code"
      sku[:attributes][:code] = {
        t: "string",
        v: d[2].gsub("'", "")
      }
    elsif d[0] == "product_manufacturer"
      sku[:attributes][:manufacturer] = {
        t: "string",
        v: d[2].gsub("'", "")
      }
    elsif d[0] == "productURL"
      sku[:attributes][:slug] = {
        t: "string",
        v: d[2].gsub("'", "")
      }
    elsif d[0] == "product_size"
      size = d[2].gsub("'", "")
      unless size == ""
        sku[:attributes][:size] = {
          t: "string",
          v: size
        }
      end
    elsif d[0] == "product_color"
      color = d[2].gsub("'", "")
      unless color == ""
        sku[:attributes][:color] = {
          t: "string",
          v: color
        }
      end
    elsif d[0] == "productCollection"
      sku[:taxons][:collection] = [ d[2].gsub("'", "") ]
    end
  end

  sku
end

def update_sku_from_dimensions(doc, sku)
  dimensions = doc.css("ul.full-row .dimen-title").select do |d|
    !d.attribute('class').to_s.include? "hidden"
  end

  dimensions.each do |d|
    dimension_title = d.css(".title-name").first.text

    if dimension_title == "Laptop Dimensions"
      measures = doc.css('.prod-measurements .dimen-prop .dimen-attr span')
      measurements = {}

      measures.each do |m|
        attr = m.attribute("itemprop").to_s
        if attr == "height"
          sku[:attributes][:height] = {
            t: "string",
            v: sanitize_content(m.text)
          }
        elsif attr == "width"
          sku[:attributes][:width] = {
            t: "string",
            v: sanitize_content(m.text)
          }
        elsif attr == "depth"
          sku[:attributes][:depth] = {
            t: "string",
            v: sanitize_content(m.text)
          }
        end
      end
    else
      value_tag = d.parent.css('> span').first
      value_text = sanitize_content(value_tag.text)

      unless value_tag == nil || value_tag.text == ""
        if dimension_title == "Primary Material"
          sku[:taxons][:material] = [ sanitize_content(value_text) ]
        elsif dimension_title == "Weight"
          sku[:attributes][:weight] = {
            t: "string",
            v: value_text
          }
        else
          sku[:attributes][dimension_title] = {
            t: "string",
            v: value_text
          }
        end
      end
    end
  end

  sku
end


# Start by grabbing all of the products.
pdp_links = collect_pdps

# What Tumi calls a product, we call a SKU
products = {}
skus = pdp_links.map.with_index do |link, idx|
  puts "Processing PDP #{idx + 1} of #{pdp_links.count}"
  sku = {
    albums: [],
    attributes: {},
    taxons: {}
  }

  url = "https://www.tumi.com#{link}"
  doc = Nokogiri::HTML(open(url, "Cookie"=>'tumi-newTest=geoNo;tumi-geo=no'))

  sku = update_sku_from_scopes(doc, sku)
  sku = update_sku_from_data_properties(doc, sku)
  sku = update_sku_from_dimensions(doc, sku)


  sku[:albums]=[{'name':'default'}]
  sku[:albums][0][:images] = read_images(doc)
  sku[:taxons][:features] = read_innovation_features(doc)

  color = read_current_pdp_color(doc)

  # Get the ID that ties everything together.
  sku_code = sku[:attributes][:code][:v]
  product_id = /^[0-9]+/.match(sku_code).to_s
  product = products[product_id]

  if product == nil
    product = { 
      albums: sku[:albums], 
      attributes: {
        title: {
          t: "string",
          v: sku[:attributes][:title][:v]
        },
        description: {
          t: "string",
          v: sku[:attributes][:description][:v]
        }
      },
      skus: [sku],
      variants: {
        color: {},
        size: {}
      },
      taxons: sku[:taxons]
    }
  else
    product[:skus] = product[:skus] << sku
    product[:albums] = product[:albums] << sku[:albums][0]

    sku[:taxonomies].each do |taxon_name, taxon_value|
      taxon_list = product[:taxonomies][taxon_name]
      taxon_list = [] if taxon_list == nil

      taxon_list = (taxon_list << taxon_value).uniq
      product[:taxonomies][taxon_name] = taxon_list
    end
  end

  unless color == nil
    variant_name = color[:variant_name]
    variant = product[:variants][:color][variant_name]

    if variant == nil
      product[:variants][:color][variant_name] = color
      product[:variants][:color][variant_name][:sku_codes] = []
    end

    sku_codes = product[:variants][:color][variant_name][:sku_codes] << sku_code
    product[:variants][:color][variant_name][:sku_codes] = sku_codes
  end

  size = read_current_pdp_size(doc, link)

  unless size == nil
    variant_name = size[:variant_name]
    variant = product[:variants][:size][variant_name]

    if variant == nil
      product[:variants][:size][variant_name] = size
      product[:variants][:size][variant_name][:sku_codes] = []
    end

    sku_codes = product[:variants][:size][variant_name][:sku_codes] << sku_code
    product[:variants][:size][variant_name][:sku_codes] = sku_codes
  end

  products[product_id] = product
end

puts "Writing to json"

File.open('products_tumi.json', 'w') do |f|
  f.puts products.to_json
end

puts "Complete!"

# TODO: This gets the color variants
# color_tags = doc.css("ul.choose-colors a")
#
# puts "Found #{color_tags.length} colors"
#
# color_tags.each do |c|
#   puts "Name: #{c.attribute('name').to_s}"
#   puts "Image: #{c.css('img').first.attribute('src').to_s}"
# end
#
#
# TODO: This gets the size variants
# size_tags = doc.css("select.select-size option")
#
# puts "Found #{size_tags.length} sizes"
#
# size_tags.each do |s|
#   puts "Value: #{s.attribute('value').to_s}"
#   puts "Content: #{s.content}"
# end
#
