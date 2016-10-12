defmodule Solomon.Claim do 
  use Solomon.Web, :model

  schema "claims" do
    field :frn, :string #Fox Resource Name
  end

  def changeset_from_frn(model, params \\ :empty) do
    model
    |> cast(params, ~w(frn)a)
    |> validate_required(~w(frn)a)
  end
end

