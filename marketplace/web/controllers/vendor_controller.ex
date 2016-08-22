defmodule Marketplace.VendorController do 
  use Marketplace.Web, :controller

  def index(conn, _params) do
    render conn, "world.html"
  end
end
