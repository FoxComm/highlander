defmodule Marketplace.Ping do
  use Marketplace.Web, :controller

  def ping(conn, _params) do
    send_resp(conn, 204, "")
  end
end
