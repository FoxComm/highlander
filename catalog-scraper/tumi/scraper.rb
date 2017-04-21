require "nokogiri"
require "open-uri"
require "json"

def sanitize_content(str)
  str.tr("\n", "").tr("\t", "").gsub("\r", "")
end

# Start by grabbing all of the products.
files = ["luggage.html", "backpacks.html", "accessories.html", "bags.html"]
pdp_links = []

files.each do |file|
  filename = "./raw/#{file}"
  doc = File.open(filename) { |f| Nokogiri::HTML(f) }
  links = doc.css(".productMainLink > a").map { |l| l.attribute("href").to_s }
  pdp_links = pdp_links + links
end

# What Tumi calls a product, we call a SKU
skus = pdp_links.sort { |x, y| x <=> y }.uniq.map do |link|
  sku = {
    albums: [],
    attributes: {},
    taxons: {}
  }

  url = "https://www.tumi.com#{link}"
  doc = Nokogiri::HTML(open(url))

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
      sku[:taxons][:productType] = {
        name: s[1]
      }
    end
  end

  data_scripts = script_tags.select do |st|
    st.text.include? "dataLayer = [{"
  end

  data_properties = data_scripts[0].text.scan(/'([a-zA-Z0-9_]+)':( )?('(.)*'|\[\]|[0-9\.\-]+|true|false)/)
  data_properties.each do |d|
    if d[0] == "productSubCat"
      sku[:taxons][:category] = {
        name: d[2].gsub("'", "")
      }
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
      sku[:taxons][:collection] = {
        name: d[2].gsub("'", "")
      }
    end
  end

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
      unless value_tag == nil || value_tag.text == ""
        sku[:attributes][dimension_title] = {
          t: "string",
          v: sanitize_content(value_tag.text)
        }
      end
    end
  end

  puts "Object #{sku}"
end
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
