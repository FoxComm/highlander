defmodule AvroSpec do
  use ESpec
  import Geronimo.Factory

  describe "avro_encode" do
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
  end # avro_encode

  describe "avro_decode" do
    it "decodes a model" do
      content_type = insert(:content_type)
      entity_attrs = %{schema_version: content_type.updated_at, content_type_id: content_type.id}
      entity = insert(:valid_entity, entity_attrs)
      {:ok, ct_binary} = Geronimo.ContentType.avro_encode(content_type)
      {:ok, e_binary} = Geronimo.Entity.avro_encode(entity)

      expect Geronimo.ContentType.avro_decode!(ct_binary) |> to(be_map)
      expect Geronimo.Entity.avro_decode!(e_binary) |> to(be_map)
    end
  end # avro_decode

  describe "avro_schema_value" do
    context "when returns schema as a map" do
      it "returns avro schema value for a model as a map" do
        ct_schema = %{fields: [%{"name" => "created_by", "type" => ["int", "null"]},
                               %{"name" => "id", "type" => ["int", "null"]},
                               %{"name" => "inserted_at", "type" => ["string", "null"]},
                               %{"name" => "name", "type" => ["string", "null"]},
                               %{"name" => "schema", "type" => ["string", "null"]},
                               %{"name" => "scope", "type" => ["string", "null"]},
                               %{"name" => "updated_at", "type" => ["string", "null"]}],
                      name: "content_types", namespace: "com.foxcommerce.geronimo", type: "record"}

        e_schema = %{fields: [%{"name" => "content", "type" => ["string", "null"]},
                              %{"name" => "content_type_id", "type" => ["int", "null"]},
                              %{"name" => "created_by", "type" => ["int", "null"]},
                              %{"name" => "id", "type" => ["int", "null"]},
                              %{"name" => "inserted_at", "type" => ["string", "null"]},
                              %{"name" => "kind", "type" => ["string", "null"]},
                              %{"name" => "schema_version", "type" => ["string", "null"]},
                              %{"name" => "scope", "type" => ["string", "null"]},
                              %{"name" => "storefront", "type" => ["string", "null"]},
                              %{"name" => "updated_at", "type" => ["string", "null"]}], name: "entities",
                     namespace: "com.foxcommerce.geronimo", type: "record"}

        expect(Geronimo.ContentType.avro_schema_value(false)).to eq(ct_schema)
        expect(Geronimo.Entity.avro_schema_value(false)).to eq(e_schema)
      end
    end # when returns schema as a map

    context "when returns schema as a string" do
      it "returns avro schema value as a string" do
        ct_schema_st = "{\"type\":\"record\",\"namespace\":\"com.foxcommerce.geronimo\",\"name\":\"content_types\",\"fields\":[{\"type\":[\"int\",\"null\"],\"name\":\"created_by\"},{\"type\":[\"int\",\"null\"],\"name\":\"id\"},{\"type\":[\"string\",\"null\"],\"name\":\"inserted_at\"},{\"type\":[\"string\",\"null\"],\"name\":\"name\"},{\"type\":[\"string\",\"null\"],\"name\":\"schema\"},{\"type\":[\"string\",\"null\"],\"name\":\"scope\"},{\"type\":[\"string\",\"null\"],\"name\":\"updated_at\"}]}"
        e_schema_st = "{\"type\":\"record\",\"namespace\":\"com.foxcommerce.geronimo\",\"name\":\"entities\",\"fields\":[{\"type\":[\"string\",\"null\"],\"name\":\"content\"},{\"type\":[\"int\",\"null\"],\"name\":\"content_type_id\"},{\"type\":[\"int\",\"null\"],\"name\":\"created_by\"},{\"type\":[\"int\",\"null\"],\"name\":\"id\"},{\"type\":[\"string\",\"null\"],\"name\":\"inserted_at\"},{\"type\":[\"string\",\"null\"],\"name\":\"kind\"},{\"type\":[\"string\",\"null\"],\"name\":\"schema_version\"},{\"type\":[\"string\",\"null\"],\"name\":\"scope\"},{\"type\":[\"string\",\"null\"],\"name\":\"storefront\"},{\"type\":[\"string\",\"null\"],\"name\":\"updated_at\"}]}"

        expect(Geronimo.ContentType.avro_schema_value).to eq(ct_schema_st)
        expect(Geronimo.Entity.avro_schema_value).to eq(e_schema_st)
      end
    end # when returns schema as a string
  end # avro_schema_value

  describe "avro_schema_key" do
    context "when returns schema key as a map" do
      it "returns avro key schema as a map" do
        k_entity = %{fields: [%{"name" => "id", "type" => ["null", "int"]}], name: "entities_pkey",
                     namespace: "com.foxcommerce.geronimo", type: "record"}
        k_content_type = %{fields: [%{"name" => "id", "type" => ["null", "int"]}],
                           name: "content_types_pkey", namespace: "com.foxcommerce.geronimo",
                           type: "record"}

        expect(Geronimo.ContentType.avro_schema_key(false)).to eq(k_content_type)
        expect(Geronimo.Entity.avro_schema_key(false)).to eq(k_entity)
      end
    end # when returns schema key as a map

    context "when returns schema key as a string" do
      it "should return avro key schema as a string" do
        k_entity_st = "{\"type\":\"record\",\"namespace\":\"com.foxcommerce.geronimo\",\"name\":\"entities_pkey\",\"fields\":[{\"type\":[\"null\",\"int\"],\"name\":\"id\"}]}"
        k_content_type_st = "{\"type\":\"record\",\"namespace\":\"com.foxcommerce.geronimo\",\"name\":\"content_types_pkey\",\"fields\":[{\"type\":[\"null\",\"int\"],\"name\":\"id\"}]}"

        expect(Geronimo.ContentType.avro_schema_key).to eq(k_content_type_st)
        expect(Geronimo.Entity.avro_schema_key).to eq(k_entity_st)
      end
    end # when returns schema key as a string
  end # avro_schema_key
end