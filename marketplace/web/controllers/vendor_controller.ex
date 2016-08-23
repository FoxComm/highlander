defmodule Marketplace.VendorController do 
  use Marketplace.Web, :controller
  alias Marketplace.Repo
  alias Marketplace.Vendor

  def index(conn, _params) do
    vendors = Repo.all(Vendor)
    render(conn, "index.json", vendors: vendors)
  end
end
