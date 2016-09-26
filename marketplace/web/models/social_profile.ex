defmodule Marketplace.SocialProfile do 
  use Marketplace.Web, :model

  schema "social_profiles" do
    field :twitter_handle, :string
    field :instagram_handle, :string
    field :google_plus_handle, :string
    field :facebook_url, :string

    timestamps
  end

  @required_fields ~w()a
  @optional_fields ~w(twitter_handle instagram_handle google_plus_handle facebook_url)a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
end
