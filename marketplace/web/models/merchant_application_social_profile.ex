defmodule Marketplace.MerchantApplicationSocialProfile do 
  use Marketplace.Web, :model

  schema "merchant_application_social_profiles" do
    belongs_to :merchant_application, Marketplace.MerchantApplication
    belongs_to :social_profile, Marketplace.SocialProfile
  end
end
