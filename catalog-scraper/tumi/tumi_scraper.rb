require 'json'
require 'nokogiri'
require 'open-uri'
require 'yaml'

def load_sources(path)
  YAML.load_file(path).map do |file_defn|
    filename = file_defn["filename"]
    {
      filename: "./raw/#{filename}",
      category: file_defn['category'],
      product_type: file_defn['product_type']
    }
  end
end

def sanitize_content(str)
  str.tr("\n", "").tr("\t", "").gsub("\r", "").strip.gsub(/[\u0080-\u00ff]/, "")
end

def parse_pdp_links(sources)
  sources.reduce({}) do |links, file_defn|
    cat_page = File.open(file_defn[:filename]) { |f| Nokogiri::HTML(f) }
    pdp_links = cat_page.css(".productMainLink > a").map do |l|
      l.attribute("href").to_s
    end

    pdp_links.reduce(links) do |acc, link|
      if acc[link] == nil
        acc[link] = {
          categories: [ file_defn[:category] ],
          product_types: [ file_defn[:product_type] ]
        }
      else
        acc[link][:categories] << file_defn[:category]
        acc[link][:product_types] << file_defn[:product_types]
      end

      acc
    end
  end
end

def get_cache_filename(link)
  "./cache/#{link.split("/")[2]}.html"
end

def cache_pdps(pdp_links)
  pdp_links.each.with_index do |(link, cat_info), idx|
    puts "Processing SKU #{idx + 1} of #{pdp_links.count}..."

    filename = get_cache_filename(link)
    if FileTest.exists?(filename)
      puts "#{link} exists in cache"
    else
      url = "https://www.tumi.com#{link}"
      open(url, "Cookie"=>"tumi-newTest=geoNo;tumi-geo=no") do |pdp|
        File.open(filename, "w") { |f| f.puts(pdp.read) }
      end
    end
  end
end

def sanitize_color(color_value)
  color_map = {
    "Beige"    => "Beige",
    "Black"    => "Black",
    "Blue"     => "Blue",
    "Brown"    => "Brown",
    "Green"    => "Green",
    "Grey"     => "Grey",
    "Metallic" => "Metallic",
    "Orange"   => "Orange",
    "Pink"     => "Pink",
    "Purple"   => "Purple",
    "White"    => "White",
    "Yellow"   => "Yellow",
    "No Color" => "No Color",

    "Khaki" => "Beige",
    "Tan"   => "Beige",

    "Black Pebbled" => "Black",

    "Atlantic"       => "Blue",
    "Cadet"          => "Blue",
    "Blue Spectator" => "Blue",
    "Dusk Blue"      => "Blue",
    "Moroccan Blue"  => "Blue",
    "Navy"           => "Blue",
    "Pacific Blue"   => "Blue",

    "Dark Brown" => "Brown",
    "Hickory"    => "Brown",

    "Banana Leaf Print" => "Green",
    "Fern Print"        => "Green",
    "Jungle"            => "Green",
    "Hunter"            => "Green",

    "Anthracite" => "Grey",
    "Earl Grey"  => "Grey",
    "Gunmetal"   => "Grey",

    "Nickel Satin"      => "Metallic",
    "Pewter"            => "Metallic",
    "Reflective Silver" => "Metallic",
    "Silver"            => "Metallic",
    "T-Graphite"        => "Metallic",

    "Galaxy Print" => "No Color",
    "Matte Clear"  => "No Color",

    "Sunrise" => "Orange",

    "Rose Gold" => "Pink",

    "Magenta" => "Purple",

    "Splatter Stripe" => "White"
  }
    
    
  color_value.split("/").map do |color|
    sanitized = color_map[color]

    if sanitized == nil
      raise "Unknown color #{color}"
    end

    sanitized
  end
end

def sanitize_material(material)
  material_map = {
    "Ballistic"       => ["Ballistic"],
    "Ballistic Nylon" => ["Ballistic"],
    
    "Fabric"                => ["Fabric"],
    "Nylon"                 => ["Fabric"],
    "Polyester Nylon Blend" => ["Fabric"],
    
    "Hardside"            => ["Hardside"],
    "Aluminum"            => ["Hardside"],
    "Durable Plastic"     => ["Hardside"],
    "Metal"               => ["Hardside"],
    "PC/TPU/TPU"          => ["Hardside"],
    "PC and TPU"          => ["Hardside"],
    "Polycarbonate"       => ["Hardside"],
    "Polycarbonate & ABS" => ["Hardside"],
    "Tegris"              => ["Hardside"],

    "Leather"               => ["Leather"],
    "Embossed Leather"      => ["Leather"],
    "Matte Pebbled Leather" => ["Leather"],
    "Pebbled Leather"       => ["Leather"],
    "Smooth Leather"        => ["Leather"],

    "Coated Canvas"          => ["Other"],
    "Textured Coated Canvas" => ["Other"],

    "Ballistic/Leather" => ["Ballistic", "Leather"],
    "Metal/Nylon/TPE"   => ["Fabric", "Hardside"],
    "TPU/Nylon"         => ["Fabric", "Hardside"]
  }

  sanitized = material_map[material]

  if sanitized == nil
    raise "Unknown material #{material}"
  end

  sanitized
end

def update_sku_from_data_layer(pdp, sku)
  data_layer_script = pdp.css("script").select { |s| s.text.include?("dataLayer = [{") }
  props = data_layer_script[0].text.scan(/'([a-zA-Z0-9_]+)':( )?('(.)*'|\[\]|[0-9\.\-]+|true|false)/)

  props.reduce(sku) do |updated_sku, prop|
    prop_name = prop[0]
    prop_value = prop[2].gsub("'", "")

    if prop_name == "productName"
      updated_sku[:attributes][:title] = {
        t: "string",
        v: prop_value
      }
    elsif prop_name == "product_sku_code"
      updated_sku[:attributes][:code] = {
        t: "string",
        v: prop_value
      }
    elsif prop_name == "product_manufacturer"
      updated_sku[:attributes][:manufacturer] = {
        t: "string",
        v: prop_value
      }
    elsif prop_name == "product_color" && prop_value != ""
      updated_sku[:taxonomies][:colorGroup] = sanitize_color(prop_value)
      updated_sku[:attributes][:color] = {
        t: "string",
        v: prop_value
      }
    elsif prop_name == "product_size" && prop_value != ""
      updated_sku[:taxonomies][:size] = [ prop_value ]
      updated_sku[:attributes][:size] = {
        t: "string",
        v: prop_value
      }
    elsif prop_name == "productPrice"
      price_val = (prop_value.to_s.to_f * 100).to_i
      price = {
        t: "price",
        v: {
          currency: "USD",
          value: price_val
        }
      }

      updated_sku[:attributes][:salePrice] = price
      updated_sku[:attributes][:retailPrice] = price
    elsif prop_name == "productCollection"
      updated_sku[:taxonomies][:collection] = [ prop_value ]
    end

    updated_sku
  end
end

def update_sku_from_dimensions(pdp, sku)
  dimensions = pdp.css(".full-row .dimen-title").select do |d|
    !d.attribute('class').to_s.include? "hidden"
  end

  dimensions.each do |d|
    dimension_title = sanitize_content(d.css(".title-name").first.text)

    if dimension_title == "Dimensions"
      measures = d.parent.css('.dimen-prop .dimen-attr span')

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
    elsif dimension_title == "Laptop Dimensions"
      d.parent.css('.dimen-attr').each do |measure|
        text = measure.text
        if text.start_with? "H:"
          sku[:attributes][:laptop_height] = {
            t: "string",
            v: sanitize_content(text.split(" ")[1])
          }
        elsif text.start_with? "W:"
          sku[:attributes][:laptop_width] = {
            t: "string",
            v: sanitize_content(text.split(" ")[1])
          }
        elsif text.start_with? "D:"
          sku[:attributes][:laptop_depth] = {
            t: "string",
            v: sanitize_content(text.split(" ")[1])
          }
        end
      end
    else
      value_tag = d.parent.css('> span').first
      value_text = sanitize_content(value_tag.text)

      unless value_tag == nil || value_tag.text == ""
        if dimension_title == "Primary Material"
          raw_material = sanitize_content(value_text)
          sku[:taxonomies][:material] = sanitize_material(raw_material)
          sku[:attributes][:primaryMaterial] = {
            t: "string",
            v: raw_material
          }
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

  expanded_titles = pdp.css(".full-row .title-name").select do |et|
    sanitize_content(et.text) == "Expanded Depth"
  end

  if expanded_titles.count > 0
    value_tag = expanded_titles[0].next
    value_text = sanitize_content(value_tag.text)

    unless value_tag == nil || value_tag.text == ""
      sku[:attributes][:expandedDepth] = {
        t: "string",
        v: value_text
      }
    end
  end


  sku
end

def update_sku_from_details(pdp, sku)
  pdp.css(".cntr-interior-exterior-feature-item").each do |item|
    title_tag = item.css(".feature-name").first
    content_tag = item.css(".pdpdetail-item-content").first

    unless title_tag == nil || content_tag == nil
      sku[:attributes][title_tag.text] = {
        t: "richText",
        v: content_tag.inner_html
      }
    end
  end

  sku
end

def update_sku_with_description(pdp, sku)
  description_tag = pdp.css(".product-desc-pdp").first
  unless description_tag == nil
    sku[:attributes][:description] = {
      t: "richText",
      v: description_tag.inner_html
    }
  end

  sku
end

def read_images(pdp)
  pdp.css("div.cntr-product-alternate-items img").map do |image_tag|
    thumbnail = "#{image_tag.attribute('src')}"
    full_image = thumbnail.split('?')[0]
    title = "#{image_tag.attribute('title')}"
    alt = "#{image_tag.attribute('alt')}"
    { title: title, alt: alt, thumb: thumbnail, src: full_image }
  end
end

def update_sku_with_albums(pdp, sku)
  sku[:albums] = [
    {
      name: "default",
      images: read_images(pdp)
    }
  ]

  sku
end

def update_sku_with_innovation_features!(pdp, sku)
  features = pdp.css(".innovation-details .innovation-slider-clc").map do |innov|
    sanitize_content(innov.css('h3').text)
  end

  sku[:taxonomies][:features] = features
end

def parse_sku(pdp, cat_info)
  sku = {
    albums: [],
    attributes: {
      activeFrom: {
        t: "datetime",
        v: "2017-01-01T00:00:00.000Z"
      }
    },
    taxonomies: {
      category: cat_info[:categories],
      productType: cat_info[:product_types]
    }
  }

  sku = update_sku_from_data_layer(pdp, sku)
  sku = update_sku_from_dimensions(pdp, sku)
  sku = update_sku_from_details(pdp, sku)
  sku = update_sku_with_description(pdp, sku)
  sku = update_sku_with_albums(pdp, sku)
  update_sku_with_innovation_features!(pdp, sku)

  sku
end

def get_product_id(sku)
  sku_code = sku[:attributes][:code][:v]
  /^[0-9]+/.match(sku_code).to_s
end

def get_product_for_sku(products, sku)
  product_id = get_product_id(sku)
  products[product_id] || {
    albums: [],
    attributes: {
      title: {
        t: "string",
        v: sku[:attributes][:title][:v]
      },
      description: {
        t: "richText",
        v: sku[:attributes][:description][:v]
      },
      productId: {
        t: "string",
        v: get_product_id(sku)
      },
      activeFrom: {
        t: "datetime",
        v: "2017-01-01T00:00:00.000Z",
      }
    },
    skus: [],
    variants: {},
    taxonomies: {}
  }
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

def update_product!(product, sku, color, size)
  product[:skus] << sku
  product[:albums] << sku[:albums][0] if sku[:albums].count > 0

  sku[:taxonomies].each do |taxon_name, taxon_value|
    taxon_list = product[:taxonomies][taxon_name]
    taxon_list = [] if taxon_list == nil

    taxon_list = (taxon_list << taxon_value).flatten.uniq
    product[:taxonomies][taxon_name] = taxon_list
  end

  unless color == nil
    product[:variants][:color] = {} if product[:variants][:color] == nil
    variant_name = color[:variant_name]
    variant = product[:variants][:color][variant_name]

    if variant == nil
      product[:variants][:color][variant_name] = color
      product[:variants][:color][variant_name][:skuCodes] = []
    end

    sku_code = sku[:attributes][:code][:v]
    skuCodes = product[:variants][:color][variant_name][:skuCodes] << sku_code
    product[:variants][:color][variant_name][:skuCodes] = skuCodes
  end

  unless size == nil
    product[:variants][:size] = {} if product[:variants][:size] == nil
    variant_name = size[:variant_name]
    variant = product[:variants][:size][variant_name]

    if variant == nil
      product[:variants][:size][variant_name] = size
      product[:variants][:size][variant_name][:skuCodes] = []
    end

    skuCodes = product[:variants][:size][variant_name][:skuCodes] << sku_code
    product[:variants][:size][variant_name][:skuCodes] = skuCodes
  end
end

def parse_products(pdp_links)
  pdp_links.reduce({}) do |products, (link, cat_info)|
    puts "Parsing #{link}..."
    
    cache_filename = get_cache_filename(link)
    pdp = File.open(cache_filename) { |f| Nokogiri::HTML(f) }
    sku = parse_sku(pdp, cat_info)
    color = read_current_pdp_color(pdp)
    size = read_current_pdp_size(pdp, link)

    product = get_product_for_sku(products, sku)
    update_product!(product, sku, color, size)

    productId = product[:attributes][:productId][:v]
    products[productId] = product

    products
  end
end

def illuminate_variants(product)
  product[:variants].map do |variant_name, raw_variant|
    illuminated_values = raw_variant.map do |value_name, value|
      {
        name: value_name,
        image: value[:img],
        skuCodes: value[:skuCodes]
      }
    end

    {
      attributes: { 
        name: {
          t: 'string', 
          v: variant_name
        }
      },
      values: illuminated_values
    }
  end
end



links = parse_pdp_links(load_sources("./config/source.yml"))
cache_pdps(links)
products = parse_products(links)

ps = products.reduce([]) do |acc, (p_id, p)|
  features = p[:taxonomies][:features]
  acc + features
end

puts "#{ps.uniq}"

puts "Writing to json"

File.open('products_tumi.json', 'w') do |f|
  final_products = products.map do |product_id, product|
    product[:variants] = illuminate_variants(product)
    product
  end

  f.puts ({products: final_products}).to_json
end

puts "Complete!"
