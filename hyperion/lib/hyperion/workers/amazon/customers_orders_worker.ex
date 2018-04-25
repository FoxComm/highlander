defmodule Hyperion.Amazon.Workers.CustomersOrdersWorker do
  use GenServer
  require Logger

  alias Hyperion.PhoenixScala.Client
  alias Hyperion.Amazon

  def start_link do
    GenServer.start_link(__MODULE__, %{})
  end

  def init(state) do
    schedule_work()
    {:ok, state}
  end

  def handle_info(:work, state) do
    do_work()
    schedule_work()
    {:noreply, state}
  end

  defp do_work() do
    try do
      fetch_amazon_orders()
      |> store_customers()
    rescue
      e in RuntimeError ->
        Logger.error("Error while fetching orders from Amazon: #{e.message}")
    end
  end

  # TODO: Add order saving into phoenix
  defp fetch_amazon_orders do
    date =
      Timex.beginning_of_day(Timex.now())
      |> Timex.format!("%Y-%m-%dT%TZ", :strftime)

    list = [fulfillment_channel: ["MFN", "AFN"], created_after: [date]]

    case MWSClient.list_orders(list, Amazon.fetch_config()) do
      {:error, error} -> raise inspect(error)
      {:warn, warn} -> raise warn["ErrorResponse"]["Error"]["Message"]
      {_, resp} -> resp["ListOrdersResponse"]["ListOrdersResult"]["Orders"]
    end
  end

  defp store_customers(orders) do
    case orders["Orders"]["Order"] do
      list when is_list(list) ->
        Enum.each(list, fn order ->
          Client.create_customer(%{
            name: order["Order"]["BuyerName"],
            email: order["Order"]["BuyerEmail"]
          })
        end)

      map when is_map(map) ->
        Client.create_customer(%{name: map["BuyerName"], email: map["BuyerEmail"]})

      empty when empty in [%{}, []] ->
        nil
    end
  end

  defp schedule_work do
    # In 24 hours
    Process.send_after(self(), :work, 24 * 60 * 60 * 1000)
  end
end
