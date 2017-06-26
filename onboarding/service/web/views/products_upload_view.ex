defmodule OnboardingService.ProductsUploadView do
  use OnboardingService.Web, :view

  def render("index.json", %{products_uploads: products_uploads}) do
    %{products_uploads: render_many(products_uploads, OnboardingService.ProductsUploadView, "products_upload.json")}
  end

  def render("show.json", %{products_upload: products_upload}) do
    %{products_upload: render_one(products_upload, OnboardingService.ProductsUploadView, "products_upload.json")}
  end

  def render("products_upload.json", %{products_upload: products_upload}) do
    %{id: products_upload.id,
      file_url: products_upload.file_url}
  end
end
