defmodule CrudSpec do
  use ESpec
  import Geronimo.Factory

  describe "get_all" do
    context "when there are some records for scope" do
      it "sgould return list" do
        ct = insert(:content_type)
        expect(Geronimo.ContentType.get_all("1")).to have_length(1)
      end
    end

    context "when no records for given scope" do
      it "sgould return an empty list" do
        ct = insert(:content_type)
        expect(Geronimo.ContentType.get_all("99")).to have_length(0)
      end
    end
  end # get_all

  describe "get" do
    context "when content_type exists" do
      it "should return content_type" do
        ct = insert(:content_type)
        {:ok, res} = Geronimo.ContentType.get(ct.id)
        expect(res.name).to eq(ct.name)
        expect(res.versions).to eq([])
        expect(res.created_by).to eq(ct.created_by)
        expect(res.scope).to eq(ct.scope)
      end
    end

    context "when content_type doesn't exists" do
      it "should return content_type" do
        ct = insert(:content_type)
        expect(Geronimo.ContentType.get("99")).to eq({:error, "Not found"})
      end
    end
  end # get

  describe "get_versions" do
    context "when no versions present" do
      it "should return empty list" do
        ct = insert(:content_type)
        expect Geronimo.ContentType.get_versions(ct.id) |> to(eq [])
      end
    end

    context "when versions are present" do
      it "should return versions list" do
        ct = insert(:content_type)
        ch = Geronimo.ContentType.changeset(ct, %{name: "BazQuux"})
        Geronimo.Repo.update(ch)
        expect(Geronimo.ContentType.get_versions(ct.id)).to have_length(1)
      end
    end
  end
end
