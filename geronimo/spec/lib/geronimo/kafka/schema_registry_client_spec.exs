defmodule SchemaRegistryClientSpec do
  use ESpec
  use ExVCR.Mock, adapter: ExVCR.Adapter.Hackney

  before do
    ExVCR.Config.cassette_library_dir("spec/fixture/vcr_cassettes")
    :ok
  end

  describe "get_schema" do
    context "when response is successful" do
      it "should return successful response" do
        use_cassette "schema_registry/success_get" do
          resp = {:ok, %{id: 140, schema: "{\"type\":\"record\",\"name\":\"entities_pkey\",\"namespace\":\"com.foxcommerce.geronimo\",\"fields\":[{\"name\":\"id\",\"type\":[\"null\",\"int\"]}]}", subject: "entities-key", version: 1}}
          expect(Geronimo.Kafka.SchemaRegistryClient.get_schema("entities-key", 1)).to eq(resp)
        end
      end
    end


    context "when schema is not found" do
      it "should return not_found response" do
        use_cassette "schema_registry/not_found" do
          resp = {:error, %{error_code: 40402, message: "Version not found."}}
          expect(Geronimo.Kafka.SchemaRegistryClient.get_schema("entities-key", 112121212)).to eq(resp)
        end
      end
    end

    context "when request failed" do
      it "should return error response" do
        use_cassette "schema_registry/fail1" do
          resp = {:fail, "econnrefused"}
          expect(Geronimo.Kafka.SchemaRegistryClient.get_schema("entities-key", 1)).to eq(resp)
        end
      end
    end
  end

  describe "store_schema" do
    context "when response is successful" do
      it "should return successful response" do
        use_cassette "schema_registry/store_success" do
          resp = {:ok, %{id: 140}}
          expect(Geronimo.Kafka.SchemaRegistryClient.store_schema("entities-key", Geronimo.Entity.avro_schema_key)).to eq({:ok, %{id: 140}})
        end
      end
    end

    context "when schema can not be processed" do
      it "should return error response" do
        use_cassette "schema_registry/err" do
          resp = {:error, %{error_code: 42201, message: "Input schema is an invalid Avro schema"}}
          expect(Geronimo.Kafka.SchemaRegistryClient.store_schema("entities-key", "wrong!")).to eq(resp)
        end
      end
    end

    context "when request failed" do
      it "should return noerrort_found response" do
        use_cassette "schema_registry/fail2" do
          resp = {:fail, "econnrefused"}
          expect(Geronimo.Kafka.SchemaRegistryClient.store_schema("entities-key", %{foo: "bar"})).to eq(resp)
        end
      end
    end
  end
end
