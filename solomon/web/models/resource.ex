defmodule Solomon.Resource do
  use Solomon.Web, :model

  schema "resources" do
    field(:name, :string)
    field(:description, :string)
    field(:actions, {:array, :string})

    belongs_to(:system, Solomon.System)
  end
end
