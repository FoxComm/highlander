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

def parse_products(pdp_links)
  pdp_links.reduce({}) do |products, (link, cat_info)|
    cache_filename = get_cache_filename(link)
    pdp = File.open(cache_filename) { |f| Nokogiri::HTML(f) }

    products
  end
end



links = parse_pdp_links(load_sources("./config/source.yml"))
cache_pdps(links)
products = parse_products(links)