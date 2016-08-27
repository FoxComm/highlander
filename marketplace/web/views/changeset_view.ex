defmodule Marketplace.ChangesetView do
  use Marketplace.Web, :view

  def render("errors.json", %{changeset: changeset}) do 
    %{errors: render_many(changeset.errors, Marketplace.ChangesetView, "error.json")}
  end

  def render("error.json", %{error: error}) do 
    %{error: error.value}
  end
end
