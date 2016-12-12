#!/opt/sensu/embedded/bin/ruby

# Update client config with cli args
require 'json'

conf = "{{ sensu_config_path }}/client.json"
f = File.read(conf)
hash = JSON.load(f)
hash["client"]["name"] = ARGV[0]
hash["client"]["address"] = ARGV[1]
hash["client"]["consul-server"]["server"] = ARGV[0]
hash["client"]["mesos"]["server"] = ARGV[1]
hash["redis"]["host"] = ARGV[2]
File.write(conf, JSON.pretty_generate(hash))
exec( "systemctl restart sensu-client" )
