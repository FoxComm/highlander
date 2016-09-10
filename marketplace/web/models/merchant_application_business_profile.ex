defmodule Marketplace.MerchantApplicationBusinessProfile do 
  use Marketplace.Web, :model

  schema "merchant_application_business_profiles" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :business_profile, Marketplace.BusinessProfile
  end
end
