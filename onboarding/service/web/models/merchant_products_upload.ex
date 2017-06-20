defmodule OnboardingService.MerchantProductsUpload do
  use OnboardingService.Web, :model

  schema "merchant_products_uploads" do
    belongs_to :merchant, OnboardingService.Merchant
    belongs_to :products_upload, OnboardingService.ProductUpload
  end

  @required_fields ~w(merchant_id products_upload_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
  end
end
