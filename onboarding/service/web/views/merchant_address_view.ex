defmodule OnboardingService.MerchantAddressView do 
  use OnboardingService.Web, :view

  def render("index.json", %{merchant_addresses: merchant_addresses}) do
    %{merchant_addresses: render_many(merchant_addresses, OnboardingService.MerchantAddressView, "show.json")}
  end

  def render("merchant_address.json", %{merchant_address: merchant_address}) do
    %{id: merchant_address.id,
      name: merchant_address.name,
      address1: merchant_address.address1,
      address2: merchant_address.address2,
      city: merchant_address.city,
      state: merchant_address.state,
      zip: merchant_address.zip,
      is_headquarters: merchant_address.is_headquarters,
      phone_number: merchant_address.phone_number
     }
  end

  def render("show.json", %{merchant_address: merchant_address}) do 
    %{merchant_address: render_one(merchant_address, OnboardingService.MerchantAddressView, "merchant_address.json")}
  end
end
