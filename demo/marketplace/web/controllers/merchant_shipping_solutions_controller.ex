defmodule Marketplace.MerchantShippingSolutionController do
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.ShippingSolution
  alias Marketplace.MerchantShippingSolution
  alias Marketplace.ShippingSolutionView

  def index(conn, params), do: secured_route(conn, params, &index/3)
  defp index(conn, %{"merchant_id" => merchant_id}, claims) do
    shipping_solutions = Repo.all(from ss in ShippingSolution,
                              join: mss in MerchantShippingSolution,
                              where: ss.id == mss.shipping_solution_id
                              and mss.merchant_id == ^merchant_id,
                              select: ss)
    render(conn, ShippingSolutionView, "index.json", shipping_solutions: shipping_solutions)
  end

  def show(conn, params), do: secured_route(conn, params, &show/3)
  defp show(conn, %{"merchant_id" => m_id}, claims) do
    m_ss = Repo.get_by!(MerchantShippingSolution, merchant_id: m_id)
    |> Repo.preload(:shipping_solution)

    conn
    |> render(ShippingSolutionView, "show.json", shipping_solution: m_ss.shipping_solution)
  end
  
  def create(conn, params), do: secured_route(conn, params, &create/3)
  defp create(conn, %{"shipping_solutions" => shipping_solutions_params, "merchant_id" => merchant_id}, claims) do
    case insert_and_relate(shipping_solutions_params, merchant_id) do
      {:ok, results_maps} ->
        shipping_solutions = Enum.map(results_maps, fn m -> Map.get(m, :shipping_solution) end)
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_shipping_solutions_path(conn, :index, merchant_id))
        |> render(ShippingSolutionView, "index.json", %{shipping_solutions: shipping_solutions})
      {:error, _failed_operation, failed_value, _changes_completed} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  defp insert_and_relate(shipping_solutions_params, merchant_id) when is_list(shipping_solutions_params) do
    results = shipping_solutions_params
            |> Enum.map(fn solution -> insert_and_relate(solution, merchant_id) end)
            |> Enum.reduce({:ok, []}, fn (res, acc) -> reduce_insert_statuses(res, acc) end)
    results
  end

  defp insert_and_relate(solution, merchant_id) when is_map(solution) do
    Multi.new
    |> Multi.insert(:shipping_solution, ShippingSolution.changeset(%ShippingSolution{}, solution))
    |> Multi.run(:merchant_shipping_solution, fn %{shipping_solution: shipping_solution} ->
      map_shipping_solution_to_merchant(shipping_solution, merchant_id) end
    )
    |> Repo.transaction
  end

  defp map_shipping_solution_to_merchant(shipping_solution, merchant_id) do
    mass_cs = MerchantShippingSolution.changeset(%MerchantShippingSolution{}, %{
        "merchant_id" => merchant_id,
        "shipping_solution_id" => shipping_solution.id
      })

    Repo.insert(mass_cs)
  end

  defp reduce_insert_statuses({:ok, map}, {:ok, maps}) do
    {:ok, [map | maps]}
  end

  defp reduce_insert_statuses({:error, op, val, chgs}, _other) do
    {:error, op, val, chgs}
  end

  defp reduce_insert_statuses(_other, {:error, op, val, chgs}) do
    {:error, op, val, chgs}
  end
end
