defmodule Marketplace.ProductsUploadView do
  use Marketplace.Web, :view

  def render("index.json", %{products_uploads: products_uploads}) do
    %{data: render_many(products_uploads, Marketplace.ProductsUploadView, "products_upload.json")}
  end

  def render("show.json", %{products_upload: products_upload}) do
    %{data: render_one(products_upload, Marketplace.ProductsUploadView, "products_upload.json")}
  end

  def render("products_upload.json", %{products_upload: products_upload}) do
    %{id: products_upload.id,
      file_url: products_upload.file_url}
  end
end
