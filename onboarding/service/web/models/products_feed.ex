defmodule OnboardingService.ProductsFeed do
  use OnboardingService.Web, :model

  schema "products_feeds" do
    field :name, :string
    field :url, :string
    field :format, :string
    field :schedule, :string

    timestamps
  end

  @required_fields ~w(name url schedule)a
  @optional_fields ~w(format)a
  @schedules ~w(once daily monday tuesday wednesday thursday friday saturday sunday)s

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_uri(:url)
    |> validate_inclusion_code(:schedule, @schedules)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_uri(:url)
    |> validate_inclusion_code(:schedule, @schedules)
  end

end
