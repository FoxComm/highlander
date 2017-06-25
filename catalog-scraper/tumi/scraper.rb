require 'nokogiri'
require 'open-uri'
require 'json'
require 'yaml'

def parse_taxon_hierarchy(values)
  values.map do |value|
    result = {attributes: {name:{t:'string', v:"#{value['name']}"}}}
    if value['swatch'] != nil
      result[:attributes][:swatch]= {t:'swatch', v:"#{value['swatch']}"}
    end
    if value['values'] != nil
      result[:children] = parse_taxon_hierarchy(value['values'])
    end
    result
  end
end

def parse_hierarchical_taxonomy(name, values)
  result = {attributes: {name:{t:'string', v:"#{name}"}}}
  taxons = parse_taxon_hierarchy(values)
  result[:taxons] = taxons
  result[:hierarchical] = taxons.any?{|i| i.key?(:children) && i[:children]!=nil}
  result
end

def parse_taxonomies
  data = YAML.load_file('./raw/taxonomies.yml')
  result = []
  data.each do |yaml_taxonomy|
    name = yaml_taxonomy['name']
    result = result << parse_hierarchical_taxonomy(name, yaml_taxonomy['values'])
  end
  puts 'Writing taxonomies to json'

  File.open('taxonomies.json', 'w') do |f|
    f.puts ({taxonomies: result}).to_json
  end
end

parse_taxonomies

def sanitize_content(str)
  str.tr("\n", "").tr("\t", "").gsub("\r", "").strip.gsub(/[\u0080-\u00ff]/, "")
end

def collect_pdps()
  category_files = [
    {
      filename: "luggage__carry-all.html",
      category: "luggage",
      productType: "Carry-All"
    },
    {
      filename: "luggage__carry-on-luggage.html",
      productType: "Carry-On Luggage"
    },
    {
      filename: "luggage__checked-luggage.html",
      productType: "Checked Luggage"
    },
    {
      filename: "luggage__duffels.html",
      productType: "Duffels"
    },
    {
      filename: "luggage__garment-bags.html",
      productType: "Garment Bags"
    },
    {
      filename: "luggage__packable-totes.html",
      productType: "Packable Totes"
    },
    {
      filename: "luggage__satchels.html",
      productType: "Satchels"
    },
    {
      filename: "luggage__wheeled-briefcases.html",
      productType: "Wheeled Briefcases"
    },
    {
      filename: "luggage__wheeled-duffels.html",
      productType: "Wheeled Duffels"
    },
    {
      filename: "luggage__wheeled-garment-bags.html",
      productType: "Wheeled Garment Bags"
    }
  ]



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
    sanitize_content(innov.css('h3').text)
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
      sku[:taxonomies][:productType] = [ s[1] ]
    end
  end

  sku
end

def update_sku_with_description(doc, sku)
  description_tag = doc.css(".product-desc-pdp").first
  unless description_tag == nil
    sku[:attributes][:description] = {
      t: "richText",
      v: description_tag.inner_html
    }
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
      subCat = d[2].gsub("'", "")
      sku[:taxonomies][:category] = [ subCat ] unless subCat == ""
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
      sku[:taxonomies][:collection] = [ d[2].gsub("'", "") ]
    end
  end

  sku
end

def update_sku_from_details(doc, sku)
  doc.css(".cntr-interior-exterior-feature-item").each do |item|
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

def update_sku_from_dimensions(doc, sku)
  dimensions = doc.css(".full-row .dimen-title").select do |d|
    !d.attribute('class').to_s.include? "hidden"
  end

  dimensions.each do |d|
    dimension_title = d.css(".title-name").first.text

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
          sku[:taxonomies][:material] = [ sanitize_content(value_text) ]
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
pdp_links.map.with_index do |link, idx|
  puts "Processing PDP #{idx + 1} of #{pdp_links.count}"
  sku = {
    albums: [],
    attributes: {
      activeFrom: {
        t: "datetime",
        v: "2017-03-09T19:59:21.609Z",
      }
    },
    taxonomies: {}
  }

  url = "https://www.tumi.com#{link}"
  doc = Nokogiri::HTML(open(url, "Cookie"=>'tumi-newTest=geoNo;tumi-geo=no'))

  sku = update_sku_from_scopes(doc, sku)
  sku = update_sku_from_data_properties(doc, sku)
  sku = update_sku_from_dimensions(doc, sku)
  sku = update_sku_from_details(doc, sku)
  sku = update_sku_with_description(doc, sku)

  sku[:albums]=[{name: 'default'}]
  sku[:albums][0][:images] = read_images(doc)
  sku[:taxonomies][:features] = read_innovation_features(doc)

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
          t: "richText",
          v: sku[:attributes][:description][:v]
        },
        activeFrom: {
          t: "datetime",
          v: "2017-03-09T19:59:21.609Z",
        }
      },
      skus: [sku],
      variants: {
        color: {},
        size: {}
      },
      taxonomies: sku[:taxonomies]
    }
  else
    product[:skus] = product[:skus] << sku
    product[:albums] = product[:albums] << sku[:albums][0]

    sku[:taxonomies].each do |taxon_name, taxon_value|
      taxon_list = product[:taxonomies][taxon_name]
      taxon_list = [] if taxon_list == nil

      taxon_list = (taxon_list << taxon_value).flatten.uniq
      product[:taxonomies][taxon_name] = taxon_list
    end
  end

  unless color == nil
    variant_name = color[:variant_name]
    variant = product[:variants][:color][variant_name]

    if variant == nil
      product[:variants][:color][variant_name] = color
      product[:variants][:color][variant_name][:skuCodes] = []
    end

    skuCodes = product[:variants][:color][variant_name][:skuCodes] << sku_code
    product[:variants][:color][variant_name][:skuCodes] = skuCodes
  end

  size = read_current_pdp_size(doc, link)

  unless size == nil
    variant_name = size[:variant_name]
    variant = product[:variants][:size][variant_name]

    if variant == nil
      product[:variants][:size][variant_name] = size
      product[:variants][:size][variant_name][:skuCodes] = []
    end

    skuCodes = product[:variants][:size][variant_name][:skuCodes] << sku_code
    product[:variants][:size][variant_name][:skuCodes] = skuCodes
  end

  products[product_id] = product
end

puts "Writing to json"

File.open('products_tumi.json', 'w') do |f|
  final_products = products.map do |product_id, product|
    product[:variants] = product[:variants].map do |variant_name, raw_variant|
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

    product
  end

  f.puts ({products: final_products}).to_json
end

puts "Complete!"
