defmodule Marketplace.Repo.Migrations.CreateMerchantProductsUpload do
  use Ecto.Migration

  def change do
    create table(:merchant_products_uploads) do
      add :merchant_id, references(:merchants)
      add :products_uploads_id, references(:products_feeds)
    end
  end
end
