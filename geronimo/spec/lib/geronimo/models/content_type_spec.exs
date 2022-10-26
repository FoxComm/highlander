defmodule ContentTypeSpec do
  use ESpec
  import Geronimo.Factory

  describe "to_avro" do
    context "when record id passed" do
      it "should return avro schema" do
        content_type = insert(:content_type)
        schema = Geronimo.ContentType.to_avro(content_type.id)
        expect(schema["name"]).to eq(content_type.name)
        expect(schema["created_by"]).to eq(content_type.created_by)
        expect(schema["scope"]).to eq(content_type.scope)
        expect(schema["schema"]).to eq(Poison.encode!(content_type.schema))
        expect(schema["id"]).to eq(content_type.id)
      end
    end

    context "when record is passed" do
      it "should return avro schema" do
        content_type = insert(:content_type)
        schema = Geronimo.ContentType.to_avro(content_type)
        expect(schema["name"]).to eq(content_type.name)
        expect(schema["created_by"]).to eq(content_type.created_by)
        expect(schema["scope"]).to eq(content_type.scope)
        expect(schema["schema"]).to eq(Poison.encode!(content_type.schema))
        expect(schema["id"]).to eq(content_type.id)
      end
    end
  end
end
