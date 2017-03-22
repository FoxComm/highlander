defmodule Marketplace.PageController do
  use Marketplace.Web, :controller

  def index(conn, _params) do
    render conn, "index.html"
  end
end
