defmodule Marketplace.Repo.Migrations.AddBusinessProfiles do
  use Ecto.Migration

  def change do
    create table (:business_profiles) do 
      add :monthly_sales_volume, :integer
      add :target_audience, :string
      add :categories, {:array, :string}

      timestamps
    end
  end
end
