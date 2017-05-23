defmodule OnboardingService.Repo.Migrations.CreateShippingSolutions do
  use Ecto.Migration

  def change do
    create table(:shipping_solutions) do
      add :carrier_name, :string
      add :price, :integer

      timestamps()
    end
  end
end
