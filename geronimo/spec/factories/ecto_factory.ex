defmodule Geronimo.EctoFactory do
  defmacro __using__(_opts) do
    quote do
      alias Geronimo.ContentType
      alias Geronimo.Entity

      def content_type_factory do
        %ContentType{
          name: "FooBar",
          schema: %{
            foo: %{type: ["string"], requred: true},
            bar: %{type: ["string"], requred: false},
            baz: %{type: ["array", []], requred: true}
          },
          scope: "1",
          created_by: 1
        }
      end

      def valid_entity_factory do
        %Entity{
          kind: "Foo",
          content: %{foo: "foo", bar: "bar", baz: [1, 2, 3]},
          schema_version: "",
          content_type_id: 1,
          created_by: 1,
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
          scope: "1"
        }
      end
    end
  end
end
