defmodule Marketplace.VendorView do 
  use Marketplace.Web, :view

  def render("index.json", %{vendors: vendors}) do
    vendors
  end
end
