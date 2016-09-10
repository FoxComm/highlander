defmodule Marketplace.BusinessProfile do 
  use Marketplace.Web, :model

  schema "business_profiles" do
    field :monthly_sales_volume, :integer
    field :target_audience, :string
    field :categories, {:array, :string}

    belongs_to :merchant, Marketplace.Merchant
  end
end
