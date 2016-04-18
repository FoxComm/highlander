# Add Buildkite signet apt repo
sudo sh -c 'echo deb https://apt.buildkite.com/buildkite-agent stable main > /etc/apt/sources.list.d/buildkite-agent.list'
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 32A37959C2FA5C3C99EFBC32A79206696452D198

# Install agent
sudo apt-get update && sudo apt-get install -y buildkite-agent

# Configure agent token
sudo sed -i "s/xxx/2d1e50045123e180d8b12c3b46c8a8dff29e38aaa4c486824d/g" /etc/buildkite-agent/buildkite-agent.cfg

# Start agent
sudo systemctl enable buildkite-agent && sudo systemctl start buildkite-agent
