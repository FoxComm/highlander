defmodule Geronimo.ContentType do
  use Ecto.Schema
  import Ecto.Query
  import Ecto.Changeset
  alias Geronimo.Repo

  use Geronimo.Crud
  use Timex.Ecto.Timestamps

  @derive {Poison.Encoder, only: [:id, :name, :schema, :scope, :created_by, :inserted_at, :updated_at, :versions]}

  @required_params [:schema, :name, :scope, :created_by]
  @optional_params []

  schema "content_types" do
    field :name, :string
    field :schema, :map
    field :scope, :string
    field :created_by, :integer

    timestamps()
  end

  def changeset(content_type, attrs \\ %{}) do
    content_type
    |> cast(attrs, @required_params ++ @optional_params)
    |> validate_required(@required_params)
  end

  def version_fields do
    "id, schema, inserted_at, updated_at"
  end

  def content_field, do: :schema

end
