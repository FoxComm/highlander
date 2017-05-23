defmodule OnboardingService.SocialProfile do 
  use OnboardingService.Web, :model

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
    |> validate_required_code(@required_fields)
    |> validate_uri(:facebook_url)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_uri(:facebook_url)
  end
end
