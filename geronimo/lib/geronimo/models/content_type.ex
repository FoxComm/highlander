defmodule Geronimo.ContentType do
  use Ecto.Schema
  import Ecto.Query
  import Ecto.Changeset
  alias Geronimo.Repo

  use Geronimo.Crud
  use Timex.Ecto.Timestamps
  use Geronimo.Kafka.Avro

  @derive {Poison.Encoder,
           only: [:id, :name, :schema, :scope, :created_by, :inserted_at, :updated_at, :versions]}

  @required_params [:schema, :name, :scope, :created_by]
  @optional_params []

  schema "content_types" do
    field(:name, :string)
    field(:schema, :map)
    field(:scope, :string)
    field(:created_by, :integer)

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

  def to_avro(content_type_id) when is_integer(content_type_id) do
    case get(content_type_id) do
      {:ok, record} -> to_avro(record)
      {:error, err} -> raise %AvroEncodingError{message: "Avro encoding error #{inspect(err)}"}
    end
  end

  def to_avro(content_type = %Geronimo.ContentType{}) do
    %{
      "id" => content_type.id,
      "name" => content_type.name,
      "schema" => Poison.encode!(content_type.schema),
      "scope" => content_type.scope,
      "created_by" => content_type.created_by,
      "inserted_at" => Timex.format!(content_type.inserted_at, "%FT%T.%fZ", :strftime),
      "updated_at" => Timex.format!(content_type.updated_at, "%FT%T.%fZ", :strftime)
    }
  end
end
