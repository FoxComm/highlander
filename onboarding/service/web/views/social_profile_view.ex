defmodule OnboardingService.SocialProfileView do 
  use OnboardingService.Web, :view

  def render("index.json", %{social_profiles: social_profiles}) do
    %{social_profiles: render_many(social_profiles, OnboardingService.SocialProfileView, "social_profile.json")}
  end

  def render("social_profile.json", %{social_profile: social_profile}) do
    %{id: social_profile.id,
      twitter_handle: social_profile.twitter_handle,
      instagram_handle: social_profile.instagram_handle,
      google_plus_handle: social_profile.google_plus_handle,
      facebook_url: social_profile.facebook_url
    }
  end

  def render("show.json", %{social_profile: social_profile}) do 
    %{social_profile: render_one(social_profile, OnboardingService.SocialProfileView, "social_profile.json")}
  end
end
