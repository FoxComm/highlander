defmodule Geronimo.Entity do
  use Ecto.Schema
  import Ecto.Query
  import Ecto.Changeset
  alias Geronimo.Repo
  alias Geronimo.ContentType

  use Geronimo.Crud
  use Timex.Ecto.Timestamps

  @derive {Poison.Encoder, only: [:id, :kind, :content, :schema_version,
                                  :content_type_id, :created_by, :inserted_at, :updated_at, :versions, :scope]}

  @required_params [:content, :kind, :content_type_id, :created_by, :schema_version, :scope]
  @optional_params []

  schema "entities" do
    field :kind, :string
    field :content, :map
    field :schema_version, :utc_datetime
    field :content_type_id, :integer
    field :created_by, :integer
    field :scope, :string

    timestamps()
  end

  def changeset(entity, attrs \\ %{}) do
    entity
    |> cast(attrs, @required_params ++ @optional_params)
    |> validate_required(@required_params)
  end

  def create({:ok, params}, content_type = %ContentType{}, user = %Geronimo.User{}) do
    prms = Map.merge(%{content: params}, %{schema_version: content_type.updated_at,
                               kind: content_type.name,
                               content_type_id: content_type.id,
                               created_by: user.id, scope: user.scope})
    Repo.transaction(fn ->
      case Repo.insert(changeset(%Geronimo.Entity{}, prms)) do
        {:ok, record} ->
          record
        {_, changes} ->
          Repo.rollback(changes)
          {:error, changes}
      end
    end)
  end

  def create(errors, _, _) when is_list(errors), do: wrap_errors(errors)

  def update(id, prms = {:ok, params}, user = %Geronimo.User{}) when is_tuple(prms) do
    {:ok, row} = get(id, user.scope)
    changes = changeset row, (%{content: params})

    Repo.transaction(fn ->
      case Repo.update(changes)  do
        {:ok, record} ->
          Map.merge(record, %{versions: get_versions(record.id)})
        {:error, changeset} ->
          Repo.rollback(changeset)
          {:error, changeset}
      end
    end)
  end

  def update(_id, errors, _user) when is_list(errors), do: wrap_errors(errors)

  def version_fields do
    "id, content, kind, schema_version, content_type_id, inserted_at, updated_at"
  end

  def content_field, do: :content

  def wrap_errors(errors) do
    Enum.map(errors, fn {:error, fld, _val, msg} ->
      %{fld => msg}
    end)
  end
end
