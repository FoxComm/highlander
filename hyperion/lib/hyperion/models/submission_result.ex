defmodule SubmissionResult do
  use Ecto.Schema
  alias Hyperion.Repo
  require Logger
  import Ecto.Query
  import Ecto.Changeset

  @derive {Poison.Encoder,
           only: [
             :product_id,
             :product_feed,
             :variations_feed,
             :price_feed,
             :inventory_feed,
             :images_feed,
             :product_feed_result,
             :variations_feed_result,
             :price_feed_result,
             :inventory_feed_result,
             :images_feed_result,
             :completed
           ]}

  schema "amazon_submission_results" do
    field(:product_id, :integer)
    field(:product_feed, :map)
    field(:variations_feed, :map)
    field(:price_feed, :map)
    field(:inventory_feed, :map)
    field(:images_feed, :map)
    field(:product_feed_result, :map)
    field(:variations_feed_result, :map)
    field(:price_feed_result, :map)
    field(:inventory_feed_result, :map)
    field(:images_feed_result, :map)
    field(:completed, :boolean)

    timestamps()
  end

  def changeset(results, params \\ %{}) do
    results
    |> cast(params, [
      :product_id,
      :product_feed,
      :variations_feed,
      :price_feed,
      :inventory_feed,
      :images_feed
    ])
    |> validate_required([:product_id])
  end

  def first_or_create(product_id) do
    pid = if is_integer(product_id), do: product_id, else: String.to_integer(product_id)
    q = from(r in SubmissionResult, where: r.product_id == ^product_id)
    Repo.one(q) || Repo.insert!(%SubmissionResult{product_id: pid})
  end

  def store_step_result(product_id, changes) do
    row = first_or_create(product_id)
    row = Ecto.Changeset.change(row, changes)

    case Repo.update(row) do
      {:ok, struct} -> {:ok, struct}
      {:error, changeset} -> {:error, changeset}
    end
  end

  def submission_result(product_id, false) do
    case Hyperion.Repo.get_by(SubmissionResult, product_id: product_id) do
      nil -> first_or_create(product_id)
      x -> x
    end
  end

  def submission_result(product_id, true) do
    q = from(s in SubmissionResult, where: s.product_id == ^product_id)

    case Hyperion.Repo.delete_all(q) do
      {_, nil} -> first_or_create(product_id)
      {:error, changeset} -> raise changeset.error
    end
  end

  def mark_as_complete(submission_result) do
    r = Ecto.Changeset.change(submission_result, %{completed: true})

    case Repo.update(r) do
      {:ok, struct} ->
        Logger.info("Push ID: #{struct.id} completed successfully")

      {:error, changeset} ->
        Logger.error("Push ID: #{submission_result.id} error occured: #{inspect(changeset)}")
    end
  end
end
