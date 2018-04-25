defmodule Geronimo.Kafka.Avro do
  @moduledoc """
  Provides macro for encoding/decoding model instances from/to Apache AVRO
  """
  defmacro __using__(_) do
    quote do
      @doc """
      Encodes model instance according to Apache avro protocol
      """
      def avro_encode(data_id) do
        data = apply(__MODULE__, :to_avro, [data_id])
        schema = avro_schema()
        type = 'com.foxcommerce.geronimo.' ++ String.to_char_list(model_name())
        Avrolixr.Codec.encode(data, schema, type)
      end

      @doc """
      Encodes model instance according to Apache avro protocol
      """
      def avro_encode!(data_id) do
        case avro_encode(data_id) do
          {:ok, avro} -> avro
          {:error, msg} -> raise %AvroEncodingError{message: "Avro encoding error #{msg}"}
        end
      end

      @doc """
      Decodes model instance according to Apache avro protocol
      """
      def avro_decode(binary), do: Avrolixr.Codec.decode(binary)

      @doc """
      Decodes model instance according to Apache avro protocol
      """
      def avro_decode!(binary), do: Avrolixr.Codec.decode!(binary)

      @doc """
      Returns AVRO schema for given model according to Ecto.Schema
      """
      def avro_schema(as_json \\ true) do
        fields =
          apply(__MODULE__, :__schema__, [:types])
          |> Enum.into([])
          |> Enum.map(fn {k, v} -> avro_field_type({k, v}) end)

        schema = %{
          fields: fields,
          name: model_name(),
          namespace: "com.foxcommerce.geronimo",
          type: "record"
        }

        case as_json do
          true -> Poison.encode!(schema)
          _ -> schema
        end
      end

      defp model_name do
        [_a, name] = Inflex.underscore(__MODULE__) |> String.split(".")
        name
      end

      defp avro_field_type({k, v}) do
        case v do
          :integer -> %{"name" => Atom.to_string(k), "type" => ["int", "null"]}
          :id -> %{"name" => Atom.to_string(k), "type" => ["int", "null"]}
          :string -> %{"name" => Atom.to_string(k), "type" => ["string", "null"]}
          :map -> %{"name" => Atom.to_string(k), "type" => ["string", "null"]}
          :utc_datetime -> %{"name" => Atom.to_string(k), "type" => ["string", "null"]}
          Timex.Ecto.DateTime -> %{"name" => Atom.to_string(k), "type" => ["string", "null"]}
          other -> raise %UnknownSchemaTypeError{message: "Unknown field type #{other}"}
        end
      end
    end
  end
end
