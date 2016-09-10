defmodule Marketplace.SocialProfile do 
  use Marketplace.Web, :model

  schema "social_profiles" do
    field :twitter_handle, :string
    field :instagram_handle, :string
    field :google_plus_handle, :string
    field :facebook_url, :string

  end
end
