defmodule Marketplace.MerchantBusinessProfile do 
  use Marketplace.Web, :model

  schema "merchant_business_profiles" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :business_profile, Marketplace.BusinessProfile
  end
end
