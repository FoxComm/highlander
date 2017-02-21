defmodule Hyperion.Amazon.Templates.SubmitProductFeed do
  use Timex

  def template_string do
    """
    <?xml version="1.0" ?>
    <AmazonEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="amzn- envelope.xsd">
    <Header>
      <DocumentVersion>1.01</DocumentVersion>
      <MerchantIdentifier><%= seller_id %></MerchantIdentifier>
    </Header>
    <MessageType>Product</MessageType>
    <PurgeAndReplace><%= purge_and_replace %></PurgeAndReplace>
    <%= for {p, idx} <- products do %>
    <Message>
      <MessageID><%= idx %></MessageID>
      <OperationType>Update</OperationType>
        <Product>
          <%= if p[:parentage] == "child" do %>
            <SKU><%= p[:code] %></SKU>
              <%= cond do %>
                <% Keyword.has_key?(p, :asin) -> %>
                  <StandardProductID>
                  <Type>ASIN</Type>
                  <Value><%= p[:asin]%></Value>
                  </StandardProductID>
                <% Keyword.has_key?(p, :upc) -> %>
                  <StandardProductID>
                  <Type>UPC</Type>
                  <Value><%= p[:upc]%></Value>
                  </StandardProductID>
                <% Keyword.has_key?(p, :ean) -> %>
                  <StandardProductID>
                  <Type>EAN</Type>
                  <Value><%= p[:ean]%></Value>
                  </StandardProductID>
                <% Keyword.has_key?(p, :isbn) -> %>
                  <StandardProductID>
                  <Type>ISBN</Type>
                  <Value><%= p[:isbn]%></Value>
                  </StandardProductID>
                <% true -> %>
                  <% nil %>
              <% end %>
            <ProductTaxCode><%= p[:taxcode] %></ProductTaxCode>
            <LaunchDate><%= Hyperion.Amazon.Templates.SubmitProductFeed.format_date_time(p[:activefrom]) %></LaunchDate>
          <% end %>
          <DescriptionData>
            <Title><%= p[:title] %></Title>
            <Brand><%= p[:brand] %></Brand>
            <Description><%= HtmlSanitizeEx.strip_tags(p[:description]) %></Description>
            <%= for t <- p[:tags] do %>
              <SearchTerms><%= t %></SearchTerms>
            <% end %>
            <%= Hyperion.Amazon.Templates.SubmitProductFeed.render_bullet_points(p) %>
            <!-- <ItemType></ItemType>
            <Manufacturer><%#= p[:manufacturer] %></Manufacturer>
            <IsGiftWrapAvailable>false</IsGiftWrapAvailable>
            <IsGiftMessageAvailable>false</IsGiftMessageAvailable> -->
          </DescriptionData>
          <ProductData>
            <%= cond do %>
              <% p[:category] == "books" -> %>
                <%= Hyperion.Amazon.TemplateBuilder.books_category(p) %>
              <% p[:category] == "clothing" -> %>
                <%= Hyperion.Amazon.TemplateBuilder.clothing_category(p) %>
              <% true -> %>
                <% nil %>
            <% end %>
          </ProductData>
        </Product>
    </Message>
    <% end %>
    </AmazonEnvelope>
    """
  end

  def render_bullet_points(product) do
    points = Enum.filter(product, fn{k, v} -> String.match?(to_string(k), ~r/bulletpoint/) end)
             |> Enum.with_index(1)
    for {{_k, v}, i} <- points do
      unless v == nil do
        """
        <BulletPoint#{i}>#{v}</BulletPoint#{i}>
        """
      end
    end
  end

  def format_date_time(dt_str) do
    {:ok, dt} = Timex.Parse.DateTime.Parser.parse(dt_str, "{ISO:Extended:Z}")
    Timex.format(dt, "%Y-%m-%dT%H:%M:%S+00:00", :strftime) |> elem(1)
  end
end