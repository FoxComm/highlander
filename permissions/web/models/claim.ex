defmodule Permission.Claim do 
  use Permissions.Web, :model

  schema "claims" do
    field :frn, :string #Fox Resource Name
  end

  def changeset_from_frn(model, frn \\ :empty) do
    model
    |> change
    |> put_change(:frn, frn)
  end
