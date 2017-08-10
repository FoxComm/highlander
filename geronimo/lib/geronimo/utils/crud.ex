defmodule Geronimo.Crud do
  defmacro __using__(_) do
    quote do
      alias Geronimo.Repo

      def get_all(scope) do
        (from m in __MODULE__, where: fragment("scope @> ?", ^scope))
        |> Repo.all
      end

      def get(id, scope \\ "1") do
        try do
          case Repo.one(from m in __MODULE__, where: fragment("scope @> ?", ^scope), where: m.id == ^id) do
            nil -> {:error, "Not found"}
            x -> {:ok, Map.merge(x, %{versions: get_versions(x.id)})}
          end
        rescue Ecto.NoResultsError ->
          raise %NotFoundError{message: "Entity with id #{id} not found"}
        end
      end

      def get_versions(id) do
        q = "select sys_period from #{history_table()} where id=$1::integer"
        case Ecto.Adapters.SQL.query(Repo, q, [id]) do
          {:ok, vers} ->
            Enum.map(vers.rows, fn r -> hd(r).upper |> Timex.to_datetime end)
          _ -> []
        end
      end

      def create(params, user = %Geronimo.User{}) when is_map(params) do
        payload = Map.merge(params, %{created_by: user.id, scope: user.scope})
        Repo.transaction(fn ->
          case Repo.insert(changeset(apply(__MODULE__, :__struct__, []), payload)) do
            {:ok, record} ->
              Geronimo.Kafka.Pusher.push_async(__MODULE__, record)
              record
            {_, changes} -> Repo.rollback(changes)
          end
        end)
      end

      def get_specific_version(id, version, scope) do
        datetime = Timex.parse!(version, "%FT%T.%fZ", :strftime)
                   |> Timex.shift(seconds: -1)
                   |> Timex.to_datetime
        q = """
        SELECT #{version_fields()} FROM #{table()}
        WHERE id = $1::integer AND sys_period @> $2::timestamptz AND scope @> $3::ltree
        UNION ALL
        SELECT #{version_fields()} from #{history_table()}
        WHERE id = $1::integer AND sys_period @> $2::timestamptz AND scope @> $3::ltree;
        """

        case Ecto.Adapters.SQL.query(Repo, q, [id, datetime, scope]) do
          {:ok, res} -> format_version(res)
          {:error, _} -> raise %NotFoundError{message: "Version #{version} not found"}
        end
      end

      def restore_version(id, version, scope) do
        {:ok,row} = get(id, scope)
        {:ok, version} = get_specific_version(id, version, scope)
        fld = content_field()
        changeset(row, %{fld => Map.get(version, fld)})
        |> apply_change()
      end


      def update(id, params, scope) when is_map(params) do
        case get(id, scope) do
          {:ok, row} -> changeset(row, params)
                        |> apply_change()
          {:error, _} ->
            raise %NotFoundError{message: "#{Inflex.singularize(table()) |> Inflex.camelize} with id #{id} not found"}
        end
      end

      def delete(id) do
        Repo.transaction(fn ->
          case Repo.delete(__MODULE__, id) do
            {:error, err} -> err
            _ -> nil
           end
        end)
      end

      defp apply_change(changeset) do
        Repo.transaction(fn  ->
          case Repo.update(changeset)  do
            {:ok, record} ->
              Geronimo.Kafka.Pusher.push_async(__MODULE__, record)
              Map.merge(record, %{versions: get_versions(record.id)})
            {:error, changeset} ->
              Repo.rollback(changeset)
              {:error, changeset}
          end
        end)
      end

      def format_version(data) do
        cols = Enum.map(data.columns, &(String.to_atom(&1)))
        v = Enum.flat_map(data.rows, fn(r) ->
              Enum.zip(cols, r)
            end)
            |> Enum.into(%{})
            |> Map.update!(:inserted_at, &(Timex.format!(&1, "%FT%T.%fZ", :strftime)))
            |> Map.update!(:updated_at, &(Timex.format!(&1, "%FT%T.%fZ", :strftime)))
        d = if Map.has_key?(v, :schema_version) do
              Map.update!(v, :schema_version, &(Timex.format!(&1, "%FT%T.%fZ", :strftime)))
            else
              v
            end
        {:ok, d}
      end

      def table do
        [m, t] = Inflex.underscore(__MODULE__) |> String.split(".")
        Inflex.pluralize(t)
      end

      def history_table do
        "#{table()}_history"
      end
    end
  end
end
