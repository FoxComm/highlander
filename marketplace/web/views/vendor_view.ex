defmodule Marketplace.VendorView do 
  use Marketplace.Web, :view

  def render("index.json", %{vendors: vendors}) do
    %{vendors: render_many(vendors, Marketplace.VendorView, "vendor.json")}
  end

  def render("vendor.json", %{vendor: vendor}) do
    %{id: vendor.id,
      name: vendor.name,
      descripiton: vendor.description,
      state: vendor.state}
  end
end
