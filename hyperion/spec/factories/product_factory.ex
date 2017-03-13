defmodule Hyperion.ProductFactory do
  defmacro __using__(_opts) do
    quote do
      def product_without_varians_factory do
        %{body: %{"albums" => [%{"createdAt" => "2017-02-28T10:38:33.711Z", "id" => 27,
               "images" => [%{"alt" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                  "id" => 7,
                  "src" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                  "title" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"}],
               "name" => "Fox", "updatedAt" => "2017-02-28T10:38:33.711Z"}],
            "attributes" => %{"activeFrom" => %{"t" => "date",
                "v" => "2017-02-28T10:38:32.559Z"},
              "description" => %{"t" => "richText",
                "v" => "Stylish fit, stylish finish."},
              "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
              "title" => %{"t" => "string", "v" => "Fox"}},
            "context" => %{"attributes" => %{"lang" => "en", "modality" => "desktop"},
              "name" => "default"}, "id" => 25,
            "skus" => [%{"albums" => [],
               "attributes" => %{"activeFrom" => %{"t" => "date",
                   "v" => "2017-02-28T10:38:33.627Z"},
                 "code" => %{"t" => "string", "v" => "SKU-TRL"},
                 "retailPrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 10500}},
                 "salePrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 10000}},
                 "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
                 "title" => %{"t" => "string", "v" => "Fox"}}, "id" => 26}],
            "slug" => "fox", "taxons" => [], "variants" => []}}
      end

      def product_with_variants_factory do
        %{body: %{"albums" => [],
            "attributes" => %{"activeFrom" => %{"t" => "datetime",
                "v" => "2017-03-09T02:21:07.763Z"},
              "activeTo" => %{"t" => "datetime", "v" => nil},
              "description" => %{"t" => "richText", "v" => "<p>bar baz</p>"},
              "tags" => %{"t" => "tags", "v" => ["a", "b", "c", "d"]},
              "title" => %{"t" => "string", "v" => "foo"}},
            "context" => %{"attributes" => %{"lang" => "en", "modality" => "desktop"},
              "name" => "default"}, "id" => 468,
            "skus" => [%{"albums" => [],
               "attributes" => %{"activeFrom" => %{"t" => "datetime",
                   "v" => "2017-03-09T02:21:07.763Z"},
                 "activeTo" => %{"t" => "datetime", "v" => nil},
                 "code" => %{"t" => "string", "v" => "SKU123"},
                 "retailPrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 1000}},
                 "salePrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 0}},
                 "title" => %{"t" => "string", "v" => ""}},
               "context" => %{"attributes" => %{"lang" => "en",
                   "modality" => "desktop"}, "name" => "default"}, "id" => 469}],
            "slug" => "foo", "taxons" => [],
            "variants" => [%{"attributes" => %{"name" => %{"t" => "string",
                   "v" => "color"}, "type" => %{"t" => "string", "v" => ""}},
               "id" => 470,
               "values" => [%{"id" => 471, "name" => "white", "skuCodes" => ["SKU123"],
                  "swatch" => "ffffff"}]},
             %{"attributes" => %{"name" => %{"t" => "string", "v" => "size"},
                 "type" => %{"t" => "string", "v" => ""}}, "id" => 472,
               "values" => [%{"id" => 473, "name" => "S", "skuCodes" => ["SKU123"],
                  "swatch" => ""}]}]}}
      end

      def sku_with_images_factory do
        %{body: %{"albums" => [%{"createdAt" => "2017-02-28T10:38:33.711Z", "id" => 27,
               "images" => [%{"alt" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                  "id" => 7,
                  "src" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                  "title" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"}],
               "name" => "Fox", "updatedAt" => "2017-02-28T10:38:33.711Z"}],
            "attributes" => %{"activeFrom" => %{"t" => "date",
                "v" => "2017-02-28T10:38:32.559Z"},
              "description" => %{"t" => "richText",
                "v" => "Stylish fit, stylish finish."},
              "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
              "title" => %{"t" => "string", "v" => "Fox"}},
            "context" => %{"attributes" => %{"lang" => "en", "modality" => "desktop"},
              "name" => "default"}, "id" => 25,
            "skus" => [%{"albums" => [%{"createdAt" => "2017-02-28T10:38:33.711Z", "id" => 27,
               "images" => [%{"alt" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                  "id" => 7,
                  "src" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                  "title" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"}],
               "name" => "Fox", "updatedAt" => "2017-02-28T10:38:33.711Z"}],
               "attributes" => %{"activeFrom" => %{"t" => "date",
                   "v" => "2017-02-28T10:38:33.627Z"},
                 "code" => %{"t" => "string", "v" => "SKU-TRL"},
                 "retailPrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 10500}},
                 "salePrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 10000}},
                 "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
                 "title" => %{"t" => "string", "v" => "Fox"}}, "id" => 26}],
            "slug" => "fox", "taxons" => [], "variants" => []}}
      end

      def product_by_title_factory do
        %{"ListMatchingProductsResponse" =>
          %{"ListMatchingProductsResult" =>
            %{"Products" =>
              %{"Product" => [
                  %{"AttributeSets" => %{"ItemAttributes" => %{"Binding" => "Apparel",
                        "Brand" => "Gilden", "Color" => "Small,black",
                        "Department" => "mens",
                        "Feature" => ["Wolverine", "X-Men", "Marvel Comics",
                         "100% Cotten", "100% Satisfaction Guaranteed"],
                        "PackageDimensions" => %{"Height" => "1.60", "Length" => "8.20",
                          "Weight" => "0.30", "Width" => "4.60"},
                        "PackageQuantity" => "1", "PartNumber" => "ycbvb635053",
                        "ProductGroup" => "Apparel", "ProductTypeName" => "SHIRT",
                        "Size" => "Small,Black",
                        "SmallImage" => %{"Height" => "75",
                          "URL" => "http://ecx.images-amazon.com/images/I/41SXDS9-wDL._SL75_.jpg",
                          "Width" => "62"},
                        "Title" => "Wolverine T-shirt Different Colors (Small, Black)",
                        "{http://www.w3.org/XML/1998/namespace}lang" => "en-US"}},
                    "Identifiers" => %{"MarketplaceASIN" => %{"ASIN" => "B01C26TGYA",
                        "MarketplaceId" => "ATVPDKIKX0DER"}},
                    "Relationships" => %{"VariationParent" => %{"Identifiers" => %{"MarketplaceASIN" => %{"ASIN" => "B01C26TG90",
                            "MarketplaceId" => "ATVPDKIKX0DER"}}}},
                    "SalesRankings" => %{}}]}},
             "ResponseMetadata" => %{"RequestId" => "06e0cea7-4e06-4ced-9cc1-e130cb1a2735"}}}
      end

      def categories_by_asin_factory do
        %{"GetProductCategoriesForASINResponse" =>
          %{"GetProductCategoriesForASINResult" =>
            %{"Self" => [%{"Parent" => %{"Parent" => %{"Parent" => %{"ProductCategoryId" => "7141123011",
                        "ProductCategoryName" => "Clothing, Shoes & Jewelry"},
                      "ProductCategoryId" => "7141124011",
                      "ProductCategoryName" => "Departments"},
                    "ProductCategoryId" => "7147441011",
                    "ProductCategoryName" => "Men"},
                  "ProductCategoryId" => "7581669011",
                  "ProductCategoryName" => "Shops"},
                %{"Parent" => %{"Parent" => %{"Parent" => %{"Parent" => %{"Parent" => %{"ProductCategoryId" => "7141123011",
                            "ProductCategoryName" => "Clothing, Shoes & Jewelry"},
                          "ProductCategoryId" => "7141124011",
                          "ProductCategoryName" => "Departments"},
                        "ProductCategoryId" => "7147445011",
                        "ProductCategoryName" => "Novelty & More"},
                      "ProductCategoryId" => "12035955011",
                      "ProductCategoryName" => "Clothing"},
                    "ProductCategoryId" => "7586148011",
                    "ProductCategoryName" => "Band & Music Fan"},
                  "ProductCategoryId" => "1252178011",
                  "ProductCategoryName" => "T-Shirts"}]},
             "ResponseMetadata" => %{"RequestId" => "cb4f02da-6c5d-4014-ba4b-dd9d3914dba5"}}}
      end

      def submit_product_feed_data do
        [{[parentage: "parent", color: "white", size: "S",
           activefrom: "2017-03-09T02:21:07.763Z", description: "<p>bar baz</p>",
           tags: ["a", "b", "c", "d"], title: "foo",
           activefrom: "2017-03-09T02:21:07.763Z", code: "PARENTSKU123",
           retailprice: %{"currency" => "USD", "value" => 1000},
           saleprice: %{"currency" => "USD", "value" => 0}], 1},
         {[parentage: "child", color: "white", size: "S",
           activefrom: "2017-03-09T02:21:07.763Z", description: "<p>bar baz</p>",
           tags: ["a", "b", "c", "d"], title: "foo",
           activefrom: "2017-03-09T02:21:07.763Z", code: "SKU123",
           retailprice: %{"currency" => "USD", "value" => 1000},
           saleprice: %{"currency" => "USD", "value" => 0}], 2}]
      end
    end
  end
end
