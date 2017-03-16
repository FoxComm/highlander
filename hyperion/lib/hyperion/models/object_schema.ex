defmodule ObjectSchema do

  use Ecto.Schema
  import Ecto.Changeset

  @derive {Poison.Encoder, only: [:id, :schema_name, :schema]}

  schema "object_schema" do
    field :schema_name
    field :schema, :map

    timestamps()
  end

  def changeset(schema, params \\ %{}) do
    schema
    |> cast(params, [:schema_name, :schema])
    |> validate_required([:schema_name, :schema])
  end
end
