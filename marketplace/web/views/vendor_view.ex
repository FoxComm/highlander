defmodule Marketplace.VendorView do 
  use Marketplace.Web, :view

  def render("index.json", %{vendors: vendors}) do
    %{vendors: render_many(vendors, Marketplace.VendorView, "vendor.json")}
  end

  def render("vendor.json", %{vendor: vendor}) do
    %{id: vendor.id,
      name: vendor.name,
      description: vendor.description,
      state: vendor.state}
  end

  def render("show.json", %{vendor: vendor}) do 
    %{vendor: render_one(vendor, Marketplace.VendorView, "vendor.json")}
  end
end
