#!/opt/sensu/embedded/bin/ruby

# Update client config with cli args

require 'json'

file = File.read("{{ sensu_config_path }}/client.json")
hash = JSON.load(file)
hash["client"]["name"] = ARGV[0]
hash["client"]["address"] = ARGV[1]
File.write("test.json", JSON.pretty_generate(hash))