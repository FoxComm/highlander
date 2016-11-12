defmodule Solomon.Scope do
  use Solomon.Web, :model

  schema "scopes" do 
    field :source, :string
    field :parent_path, :string
    
    #Implementing without ltree for now.  Custom relation will be needed
    belongs_to :parent, Solomon.Scope
    has_many :children, Solomon.Scope, foreign_key: :parent_id
  end

  @required_fields ~w(source parent_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
end
