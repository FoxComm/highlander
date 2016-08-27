defmodule Marketplace.ChangesetView do
  use Marketplace.Web, :view

  def translate_errors(changeset) do 
    Ecto.Changeset.traverse_errors(changeset, &translate_error/1)
  end
  
  def render("errors.json", %{changeset: changeset}) do 
    %{errors: translate_errors(changeset)}
  end

  #def render("error.json", %{error: error}) do 
  #  %{error: error.value}
  #end
end
