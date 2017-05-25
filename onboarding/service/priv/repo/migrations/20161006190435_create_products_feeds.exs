defmodule OnboardingService.Repo.Migrations.CreateProductsFeeds do
  use Ecto.Migration

  def change do
    create table(:products_feeds) do
      add :name, :string
      add :url, :string
      add :format, :string
      add :schedule, :string

      timestamps
    end

  end
end
