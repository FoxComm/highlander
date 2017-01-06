defmodule Solomon.PingController do
  use Solomon.Web, :controller

  def ping(conn, _params) do
    send_resp(conn, 200, "")
  end
end
