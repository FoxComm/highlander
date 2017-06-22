defmodule SchemaRegistryClientSpec do
  use ESpec

  before do
    schema_registry_client_stub = Stubr.stub!([
      get_schema: fn("foo", 1) -> {:ok, %{foo: "bar"}} end,
      get_schema: fn("Not_found", 000) -> {:error, %{error: "Not found"}} end,
      get_schema: fn("err", "e") -> {:fail, "%HTTPoison.Error{reason: :econnrefused}"} end,
      store_schema: fn("foo", %{foo: "bar"}) -> {:ok, "Foo"} end,
      store_schema: fn("foo", "asdasdas") -> {:error, %{error: "Not acceptable"}} end,
      store_schema: fn(nil, nil) -> {:fail, "%HTTPoison.Error{reason: :econnrefused}"} end ],
      module: Geronimo.Kafka.SchemaRegistryClient)
    {:shared, stub: schema_registry_client_stub}
  end

  describe "get_schema" do
    context "when response is successful" do
      it "should return successful response" do
        success_response = shared.stub.get_schema("foo", 1)
        expect(success_response).to eq({:ok, %{foo: "bar"}})
      end
    end

    context "when schema is not found" do
      it "should return not_found response" do
        not_found_response = shared.stub.get_schema("Not_found", 000)
        expect(not_found_response).to eq({:error, %{error: "Not found"}})
      end
    end

    context "when request failed" do
      it "should return error response" do
        err_get_response = shared.stub.get_schema("err", "e")
        expect(err_get_response).to eq({:fail, "%HTTPoison.Error{reason: :econnrefused}"})
      end
    end
  end

  describe "store_schema" do
    context "when response is successful" do
      it "should return successful response" do
        success_response = shared.stub.store_schema("foo", %{foo: "bar"})
        expect(success_response).to eq({:ok, "Foo"})
      end
    end

    context "when schema can not be processed" do
      it "should return 422 response" do
        not_acceptable_response = shared.stub.store_schema("foo", "asdasdas")
        expect(not_acceptable_response).to eq({:error, %{error: "Not acceptable"}})
      end
    end

    context "when request failed" do
      it "should return noerrort_found response" do
        err_post_response = shared.stub.store_schema(nil, nil)
        expect(err_post_response).to eq({:fail, "%HTTPoison.Error{reason: :econnrefused}"})
      end
    end
  end
end
