defmodule Marketplace.Vendor do
  use Marketplace.Web, :model

  schema "vendors" do
    field :name, :string
    field :description, :string
    field :state, :string, default: "new"

    timestamps
  end

  @required_fields ~w(name description)
  @optional_fields ~w(state)
  
  use EctoStateMachine,
    states: [:new, :approved, :suspended, :cancelled],
    events: [
      [
        name: :approve,
        from: [:new, :suspended],
        to: :approved
      ], [
        name: :suspend,
        from: [:new, :approved],
        to: :suspended
      ], [
        name: :cancel, 
        from: [:suspended, :approved, :new],
        to: :cancelled
      ]
    ]

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name description), ~w(state))
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(id), ~w(name description state))
  end
end
