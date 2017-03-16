defmodule SubmissionResult do
  use Ecto.Schema
  alias Hyperion.Repo
  import Ecto.Query
  import Ecto.Changeset

  @derive {Poison.Encoder, only: [:product_id, :product_feed, :price_feed, :inventory_feed, :images_feed]}

  schema "amazon_submission_results" do
    field :product_id, :integer
    field :product_feed, :map
    field :price_feed, :map
    field :inventory_feed, :map
    field :images_feed, :map

    timestamps()
  end

  def changeset(results, params \\ %{}) do
    results
    |> cast(params, [:product_id, :product_feed, :price_feed, :inventory_feed, :images_feed])
    |> validate_required([:product_id])
  end

  def first_or_create(product_id) do
    q = from r in SubmissionResult,
        where: r.product_id == ^product_id
    Repo.one(q) || Repo.insert!(%SubmissionResult{product_id: product_id})
  end

  def store_step_result(product_id, changes)  do
    row = first_or_create(product_id)
    row = Ecto.Changeset.change(row, changes)
    case Repo.update(row) do
      {:ok, struct} -> {:ok, struct}
      {:error, changeset} -> {:error, changeset}
    end
  end

  def submission_result(product_id) do
    # res = Hyperion.Repo.get_by(SubmissionResult, product_id: product_id)
    case Hyperion.Repo.get_by(SubmissionResult, product_id: product_id) do
      empty when empty == %{} -> nil
      x -> x
    end
  end
end
