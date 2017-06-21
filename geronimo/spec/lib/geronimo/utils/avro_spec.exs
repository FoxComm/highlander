defmodule AvroSpec do
  use ESpec
  import Geronimo.Factory

  describe "avro_encoding" do
    it "encodes a model" do
      content_type = insert(:content_type)
      entity_attrs = %{schema_version: content_type.updated_at, content_type_id: content_type.id}
      entity = insert(:valid_entity, entity_attrs)

      {:ok, ct_binary} = Geronimo.ContentType.avro_encode(content_type)
      {:ok, e_binary} = Geronimo.Entity.avro_encode(entity)

      expect  ct_binary |> to (be_binary)
      expect  e_binary |> to (be_binary)

      expect Geronimo.ContentType.avro_encode!(content_type) |> to(be_binary)
      expect Geronimo.Entity.avro_encode!(entity) |> to(be_binary)
    end

    it "decodes a model" do
      content_type = insert(:content_type)
      entity_attrs = %{schema_version: content_type.updated_at, content_type_id: content_type.id}
      entity = insert(:valid_entity, entity_attrs)
      {:ok, ct_binary} = Geronimo.ContentType.avro_encode(content_type)
      {:ok, e_binary} = Geronimo.Entity.avro_encode(entity)

      expect Geronimo.ContentType.avro_decode!(ct_binary) |> to(be_map)
      expect Geronimo.Entity.avro_decode!(e_binary) |> to(be_map)
    end

    it "returns avro schema for a model as a map" do
      ct_schema = %{fields: [%{"name" => "created_by", "type" => ["int", "null"]},
                             %{"name" => "id", "type" => ["int", "null"]},
                             %{"name" => "inserted_at", "type" => ["string", "null"]},
                             %{"name" => "name", "type" => ["string", "null"]},
                             %{"name" => "schema", "type" => ["string", "null"]},
                             %{"name" => "scope", "type" => ["string", "null"]},
                             %{"name" => "updated_at", "type" => ["string", "null"]}],
                    name: "content_type", namespace: "com.foxcommerce.geronimo", type: "record"}
      e_schema = %{fields: [%{"name" => "content", "type" => ["string", "null"]},
                            %{"name" => "content_type_id", "type" => ["int", "null"]},
                            %{"name" => "created_by", "type" => ["int", "null"]},
                            %{"name" => "id", "type" => ["int", "null"]},
                            %{"name" => "inserted_at", "type" => ["string", "null"]},
                            %{"name" => "kind", "type" => ["string", "null"]},
                            %{"name" => "schema_version", "type" => ["string", "null"]},
                            %{"name" => "scope", "type" => ["string", "null"]},
                            %{"name" => "updated_at", "type" => ["string", "null"]}], name: "entity",
                   namespace: "com.foxcommerce.geronimo", type: "record"}

      expect(Geronimo.ContentType.avro_schema(false)).to eq(ct_schema)
      expect(Geronimo.Entity.avro_schema(false)).to eq(e_schema)
    end

    it "returns avro schema as a string" do
      ct_schema_st = "{\"type\":\"record\",\"namespace\":\"com.foxcommerce.geronimo\",\"name\":\"content_type\",\"fields\":[{\"type\":[\"int\",\"null\"],\"name\":\"created_by\"},{\"type\":[\"int\",\"null\"],\"name\":\"id\"},{\"type\":[\"string\",\"null\"],\"name\":\"inserted_at\"},{\"type\":[\"string\",\"null\"],\"name\":\"name\"},{\"type\":[\"string\",\"null\"],\"name\":\"schema\"},{\"type\":[\"string\",\"null\"],\"name\":\"scope\"},{\"type\":[\"string\",\"null\"],\"name\":\"updated_at\"}]}"
      e_schema_st = "{\"type\":\"record\",\"namespace\":\"com.foxcommerce.geronimo\",\"name\":\"entity\",\"fields\":[{\"type\":[\"string\",\"null\"],\"name\":\"content\"},{\"type\":[\"int\",\"null\"],\"name\":\"content_type_id\"},{\"type\":[\"int\",\"null\"],\"name\":\"created_by\"},{\"type\":[\"int\",\"null\"],\"name\":\"id\"},{\"type\":[\"string\",\"null\"],\"name\":\"inserted_at\"},{\"type\":[\"string\",\"null\"],\"name\":\"kind\"},{\"type\":[\"string\",\"null\"],\"name\":\"schema_version\"},{\"type\":[\"string\",\"null\"],\"name\":\"scope\"},{\"type\":[\"string\",\"null\"],\"name\":\"updated_at\"}]}"

      expect(Geronimo.ContentType.avro_schema).to eq(ct_schema_st)
      expect(Geronimo.Entity.avro_schema).to eq(e_schema_st)
    end
  end
end