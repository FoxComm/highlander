defmodule Marketplace.BusinessProfileView do 
  use Marketplace.Web, :view

  def render("index.json", %{business_profiles: business_profiles}) do
    %{business_profiles: render_many(business_profiles, Marketplace.BusinessProfileView, "business_profile.json")}
  end

  def render("business_profile.json", %{business_profile: business_profile}) do
    %{id: business_profile.id,
      monthly_sales_volume: business_profile.monthly_sales_volume,
      target_audience: business_profile.target_audience,
      categories: business_profile.categories
    }
  end

  def render("show.json", %{business_profile: business_profile}) do 
    %{business_profile: render_one(business_profile, Marketplace.BusinessProfileView, "business_profile.json")}
  end
end
