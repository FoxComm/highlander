defmodule Marketplace.MerchantShippingSolutionController do
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.ShippingSolution
  alias Marketplace.MerchantShippingSolution
  alias Marketplace.ShippingSolutionView

  def index(conn, %{"merchant_id" => merchant_id}) do
    shipping_solutions = Repo.all(from ss in ShippingSolution,
                              join: mss in MerchantShippingSolution,
                              where: ss.id == mss.shipping_solution_id
                              and mss.merchant_id == ^merchant_id,
                              select: ss)
    render(conn, ShippingSolutionView, "index.json", shipping_solutions: shipping_solutions)
  end

  def show(conn, %{"merchant_id" => m_id}) do
    m_ss = Repo.get_by!(MerchantShippingSolution, merchant_id: m_id)
    |> Repo.preload(:shipping_solution)

    conn
    |> render(ShippingSolutionView, "show.json", shipping_solution: m_ss.shipping_solution)
  end
  
  def create(conn, %{"shipping_solutions" => shipping_solutions_params, "merchant_id" => merchant_id}) do
    case Repo.transaction(insert_and_relate(shipping_solutions_params, merchant_id)) do
      {:ok, %{shipping_solution: shipping_solution, merchant_shipping_solution: m_ss}} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_shipping_solutions_path(conn, :show, merchant_id))
        |> render(ShippingSolutionView, "shipping_solution.json", shipping_solution: shipping_solution)
      {:error, failed_operation, failed_value, changes_completed} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  defp insert_and_relate(shipping_solutions_params, merchant_id) do
    multi = Ecto.Multi.new
    for shipping_solution_param <- shipping_solutions_params do
        IO.puts(shipping_solution_param)
        ss_cs = ShippingSolution.changeset(%ShippingSolution{}, shipping_solution_param)
        rhs = Multi.new
                      |> Multi.insert(:shipping_solution, ss_cs)
                      |> Multi.run(:merchant_shipping_solution, fn %{shipping_solution: shipping_solution} ->
                        map_shipping_solution_to_merchant(shipping_solution, merchant_id) end
                      )
        multi = Multi.append(multi, rhs)
    end

    multi
  end

  defp map_shipping_solution_to_merchant(shipping_solution, merchant_id) do
    mass_cs = MerchantShippingSolution.changeset(%MerchantShippingSolution{}, %{
        "merchant_id" => merchant_id,
        "shipping_solution_id" => shipping_solution.id
      })

    Repo.insert(mass_cs)
  end
end
