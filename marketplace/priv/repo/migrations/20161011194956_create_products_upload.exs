defmodule Marketplace.Repo.Migrations.CreateProductsUpload do
  use Ecto.Migration

  def change do
    create table(:products_uploads) do
      add :file_url, :string

      timestamps
    end

  end
end
