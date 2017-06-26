defmodule Geronimo.EctoFactory do
  defmacro __using__(_opts) do
    quote do
      alias Geronimo.ContentType
      alias Geronimo.Entity

      def content_type_factory do
        %ContentType{
          name: "FooBar",
          schema:  %{"author" => %{"required" => true, "type" => ["string"]},
                     "body" => %{"required" => true, "type" => ["string"], "widget" => "richText"},
                     "tags" => %{"required" => false, "type" => ["array", []]},
                     "title" => %{"required" => true, "type" => ["string"]}},
          scope: "1",
          created_by: 1
        }
      end

      def valid_entity_factory do
        %Entity{
          kind: "Foo",
          content: %{"author" => "foo", "body" => "bar", "tags" => [1, 2, 3], "title" => "quuux"},
          schema_version: "",
          content_type_id: 1,
          created_by: 1,
          storefront: "foo",
          scope: "1"
        }
      end

      def invalid_entity_factory do
        %Entity{
          kind: "Foo",
          content: %{},
          schema_version: "",
          content_type_id: 1,
          created_by: 1,
          storefront: "foo",
          scope: "1"
        }
      end
    end
  end
end
