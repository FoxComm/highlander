defmodule ValidationSpec do
  use ESpec
  import Geronimo.Factory

  describe "validate!" do
    context "when valid params are passed" do
      it "should return {:ok, valid_data}" do
        schema = params_for(:content_type).schema
        data = params_for(:valid_entity).content
        expect Geronimo.Validation.validate!(data, schema)
        |> to(eq {:ok, %{author: "foo", body: "bar", tags: [1, 2, 3], title: "quuux"}})
      end
    end # valid params

    context "when params are invalid" do
      it "should return {:error, valid_data}" do
        schema = params_for(:content_type).schema
        expect Geronimo.Validation.validate!(%{}, schema)
        |> to(eq [{:error, :author, :presence, "must be present"},
                  {:error, :body, :presence, "must be present"},
                  {:error, :title, :presence, "must be present"}])
      end
    end

  end

  describe "validate integer" do
    let :schema, do: %{"amount" => %{"required" => true, "type" => ["integer"]}}

    context "when integer is passed" do
      it "should return :ok" do
        expect Geronimo.Validation.validate!(%{"amount" => 123}, schema)
        |> to(eq {:ok, %{amount: 123}})
      end
    end # :ok

    context "when string is passed" do
      it "should return an error" do
        expect Geronimo.Validation.validate!(%{"amount" => "foo"}, schema)
        |> to(eq [{:error, :amount, :format, "only digits accepted"}])
      end
    end # :error
  end # integers

  describe "validate decimal" do
    let :schema, do: %{"amount" => %{"required" => true, "type" => ["float"]}}

    context "when decimal is passed" do
      it "should return :ok" do
        expect Geronimo.Validation.validate!(%{"amount" => 12.3}, schema)
        |> to(eq {:ok, %{amount: 12.3}})
      end
    end # :ok

    context "when string is passed" do
      it "should return an error" do
        expect Geronimo.Validation.validate!(%{"amount" => "foo"}, schema)
        |> to(eq [{:error, :amount, :format, "should be like 123.45"}])
      end
    end # :error
  end # decimals

  describe "validate arrays" do
    let :schema, do: %{"tags" => %{"required" => true, "type" => ["array"]}}

    context "when array is passed" do
      it "should return :ok" do
        expect Geronimo.Validation.validate!(%{"tags" => [1, 2]}, schema)
        |> to(eq {:ok, %{tags: [1, 2]}})
      end

      it "should return :ok on empty arrays" do
        expect Geronimo.Validation.validate!(%{"tags" => []}, %{"tags" => %{"required" => false, "type" => ["array"]}})
        |> to(eq {:ok, %{tags: []}})
      end
    end # :ok

    context "when string is passed" do
      it "should return an error" do
        expect Geronimo.Validation.validate!(%{"tags" => "foo"}, schema)
        |> to(eq [{:error, :tags, :by, "must be valid"}])
      end

      it "should return :error on wrong format but not required" do
        expect Geronimo.Validation.validate!(%{"tags" => "foo"}, %{"tags" => %{"required" => false, "type" => ["array"]}})
        |> to(eq [{:error, :tags, :by, "must be valid"}])
      end
    end # :error
  end # arrays

  describe "validate date" do
    let :schema, do: %{"date" => %{"required" => true, "type" => ["date"]}}

    context "when date is passed" do
      it "should return :ok" do
        expect Geronimo.Validation.validate!(%{"date" => "1970-01-01T12:12:12.123456Z"}, schema)
        |> to(eq {:ok, %{date: "1970-01-01T12:12:12.123456Z"}})
      end
    end # :ok

    context "when string is passed" do
      it "should return an error" do
        expect Geronimo.Validation.validate!(%{"date" => "foo"}, schema)
        |> to(eq [{:error, :date, :by, "should be like 1970-01-01T00:00:00.0Z"}])
      end
    end # :error
  end # arrays
end
