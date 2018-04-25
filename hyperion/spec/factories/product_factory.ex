defmodule Hyperion.ProductFactory do
  defmacro __using__(_opts) do
    quote do
      def product_without_varians_factory do
        %{
          body: %{
            "albums" => [
              %{
                "createdAt" => "2017-02-28T10:38:33.711Z",
                "id" => 27,
                "images" => [
                  %{
                    "alt" =>
                      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                    "id" => 7,
                    "src" =>
                      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                    "title" =>
                      "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"
                  }
                ],
                "name" => "Fox",
                "updatedAt" => "2017-02-28T10:38:33.711Z"
              }
            ],
            "attributes" => %{
              "activeFrom" => %{"t" => "date", "v" => "2017-02-28T10:38:32.559Z"},
              "description" => %{"t" => "richText", "v" => "Stylish fit, stylish finish."},
              "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
              "title" => %{"t" => "string", "v" => "Fox"}
            },
            "context" => %{
              "attributes" => %{"lang" => "en", "modality" => "desktop"},
              "name" => "default"
            },
            "id" => 25,
            "skus" => [
              %{
                "albums" => [],
                "attributes" => %{
                  "activeFrom" => %{"t" => "date", "v" => "2017-02-28T10:38:33.627Z"},
                  "amazon" => %{"t" => "bool", "v" => true},
                  "code" => %{"t" => "string", "v" => "SKU-TRL"},
                  "retailPrice" => %{
                    "t" => "price",
                    "v" => %{"currency" => "USD", "value" => 10500}
                  },
                  "salePrice" => %{
                    "t" => "price",
                    "v" => %{"currency" => "USD", "value" => 10000}
                  },
                  "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
                  "title" => %{"t" => "string", "v" => "Fox"}
                },
                "id" => 26
              }
            ],
            "slug" => "fox",
            "taxons" => [],
            "variants" => []
          }
        }
      end

      def product_with_variants_factory do
        %{
          body: %{
            "albums" => [],
            "attributes" => %{
              "activeFrom" => %{"t" => "datetime", "v" => "2017-03-09T02:21:07.763Z"},
              "activeTo" => %{"t" => "datetime", "v" => nil},
              "description" => %{"t" => "richText", "v" => "<p>bar baz</p>"},
              "tags" => %{"t" => "tags", "v" => ["a", "b", "c", "d"]},
              "title" => %{"t" => "string", "v" => "foo"}
            },
            "context" => %{
              "attributes" => %{"lang" => "en", "modality" => "desktop"},
              "name" => "default"
            },
            "id" => 468,
            "skus" => [
              %{
                "albums" => [],
                "attributes" => %{
                  "activeFrom" => %{"t" => "datetime", "v" => "2017-03-09T02:21:07.763Z"},
                  "amazon" => %{"t" => "bool", "v" => true},
                  "activeTo" => %{"t" => "datetime", "v" => nil},
                  "code" => %{"t" => "string", "v" => "SKU123"},
                  "retailPrice" => %{
                    "t" => "price",
                    "v" => %{"currency" => "USD", "value" => 1000}
                  },
                  "salePrice" => %{"t" => "price", "v" => %{"currency" => "USD", "value" => 0}},
                  "title" => %{"t" => "string", "v" => ""}
                },
                "context" => %{
                  "attributes" => %{"lang" => "en", "modality" => "desktop"},
                  "name" => "default"
                },
                "id" => 469
              }
            ],
            "slug" => "foo",
            "taxons" => [],
            "variants" => [
              %{
                "attributes" => %{
                  "name" => %{"t" => "string", "v" => "color"},
                  "type" => %{"t" => "string", "v" => ""}
                },
                "id" => 470,
                "values" => [
                  %{
                    "id" => 471,
                    "name" => "white",
                    "skuCodes" => ["SKU123"],
                    "swatch" => "ffffff"
                  }
                ]
              },
              %{
                "attributes" => %{
                  "name" => %{"t" => "string", "v" => "size"},
                  "type" => %{"t" => "string", "v" => ""}
                },
                "id" => 472,
                "values" => [
                  %{"id" => 473, "name" => "S", "skuCodes" => ["SKU123"], "swatch" => ""}
                ]
              }
            ]
          }
        }
      end

      def sku_with_images_factory do
        %{
          body: %{
            "albums" => [
              %{
                "createdAt" => "2017-03-20T07:11:18.746Z",
                "id" => 245,
                "images" => [
                  %{
                    "alt" => "81B6PnROB1L._UX522_.jpg",
                    "id" => 20,
                    "src" =>
                      "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/245/81B6PnROB1L._UX522_.jpg",
                    "title" => "81B6PnROB1L._UX522_.jpg"
                  }
                ],
                "name" => "main",
                "updatedAt" => "2017-03-20T07:11:18.746Z"
              }
            ],
            "attributes" => %{
              "activeFrom" => %{"t" => "datetime", "v" => "2017-03-20T07:11:03.901Z"},
              "activeTo" => %{"t" => "datetime", "v" => nil},
              "asin" => %{"t" => "string", "v" => "B01N12085Q"},
              "brand" => %{"t" => "string", "v" => "NorthStarTees"},
              "bullet boint 1" => %{
                "t" => "string",
                "v" => "60% combed ringspun cotton/40% polyester jersey"
              },
              "bullet point 2" => %{"t" => "string", "v" => "slightly heathered"},
              "bullet point 3" => %{
                "t" => "string",
                "v" => "fabric laundered for reduced shrinkage"
              },
              "bullet point 4" => %{
                "t" => "string",
                "v" => "Next Level Men's Premium Fitted CVC Crew Tee"
              },
              "bulletPoint1" => %{"t" => "string", "v" => "BP1"},
              "bulletPoint2" => %{"t" => "string", "v" => "BP2"},
              "bulletPoint3" => %{"t" => "string", "v" => "BP3"},
              "bulletPoint4" => %{"t" => "string", "v" => "BP4"},
              "description" => %{
                "t" => "richText",
                "v" =>
                  "<p>Machine wash cold. Non-chlorine bleach, when needed. Tumble dry medium. Do not iron decorations/customization</p>"
              },
              "manufacturer" => %{"t" => "string", "v" => "Uncle Lao inductries, LLC"},
              "nodeId" => %{"t" => "string", "v" => "9057094011"},
              "node_id" => %{"t" => "string", "v" => 6_572_918_011},
              "node_path" => %{
                "t" => "string",
                "v" =>
                  "Clothing, Shoes & Jewelry/Women/Shops/Uniforms, Work & Safety/Clothing/Work Utility & Safety/Tops/T-shirts"
              },
              "tags" => %{"t" => "tags", "v" => ["x-men", "volverine", "t-shirt", "gray"]},
              "tax code" => %{"t" => "string", "v" => "A_GEN_NOTAX"},
              "taxCode" => %{"t" => "string", "v" => "A_GEN_NOTAX"},
              "title" => %{
                "t" => "string",
                "v" => "X-Men \"Wolverine\" Mens Athletic Fit T-Shirt"
              }
            },
            "context" => %{
              "attributes" => %{"lang" => "en", "modality" => "desktop"},
              "name" => "default"
            },
            "id" => 239,
            "skus" => [
              %{
                "albums" => [
                  %{
                    "createdAt" => "2017-03-20T07:11:18.746Z",
                    "id" => 245,
                    "images" => [
                      %{
                        "alt" => "81B6PnROB1L._UX522_.jpg",
                        "id" => 20,
                        "src" =>
                          "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/245/81B6PnROB1L._UX522_.jpg",
                        "title" => "81B6PnROB1L._UX522_.jpg"
                      }
                    ],
                    "name" => "main",
                    "updatedAt" => "2017-03-20T07:11:18.746Z"
                  }
                ],
                "attributes" => %{
                  "activeFrom" => %{"t" => "datetime", "v" => "2017-03-20T07:11:03.901Z"},
                  "activeTo" => %{"t" => "datetime", "v" => nil},
                  "amazon" => %{"t" => "bool", "v" => true},
                  "asin" => %{"t" => "string", "v" => "B01N350AKW"},
                  "code" => %{"t" => "string", "v" => "XMENTEEX1"},
                  "context" => %{"v" => "default"},
                  "inventory" => %{"t" => "string", "v" => "12"},
                  "retailPrice" => %{
                    "t" => "price",
                    "v" => %{"currency" => "USD", "value" => 2000}
                  },
                  "salePrice" => %{"t" => "price", "v" => %{"currency" => "USD", "value" => 0}},
                  "title" => %{"t" => "string", "v" => ""},
                  "upc" => %{"t" => "string", "v" => "qwe"}
                },
                "context" => %{
                  "attributes" => %{"lang" => "en", "modality" => "desktop"},
                  "name" => "default"
                },
                "id" => 240
              }
            ],
            "slug" => "x-men-wolverine-mens-athletic-fit-t-shirt",
            "taxons" => [],
            "variants" => [
              %{
                "attributes" => %{
                  "name" => %{"t" => "string", "v" => "color"},
                  "type" => %{"t" => "string", "v" => ""}
                },
                "id" => 241,
                "values" => [
                  %{
                    "id" => 242,
                    "name" => "Heather Gray",
                    "skuCodes" => ["XMENTEEX1"],
                    "swatch" => "a3a3a3"
                  }
                ]
              },
              %{
                "attributes" => %{
                  "name" => %{"t" => "string", "v" => "size"},
                  "type" => %{"t" => "string", "v" => ""}
                },
                "id" => 243,
                "values" => [
                  %{"id" => 244, "name" => "X-Large", "skuCodes" => ["XMENTEEX1"], "swatch" => ""}
                ]
              }
            ]
          }
        }
      end

      def product_by_title_factory do
        %{
          "ListMatchingProductsResponse" => %{
            "ListMatchingProductsResult" => %{
              "Products" => %{
                "Product" => [
                  %{
                    "AttributeSets" => %{
                      "ItemAttributes" => %{
                        "Binding" => "Apparel",
                        "Brand" => "Gilden",
                        "Color" => "Small,black",
                        "Department" => "mens",
                        "Feature" => [
                          "Wolverine",
                          "X-Men",
                          "Marvel Comics",
                          "100% Cotten",
                          "100% Satisfaction Guaranteed"
                        ],
                        "PackageDimensions" => %{
                          "Height" => "1.60",
                          "Length" => "8.20",
                          "Weight" => "0.30",
                          "Width" => "4.60"
                        },
                        "PackageQuantity" => "1",
                        "PartNumber" => "ycbvb635053",
                        "ProductGroup" => "Apparel",
                        "ProductTypeName" => "SHIRT",
                        "Size" => "Small,Black",
                        "SmallImage" => %{
                          "Height" => "75",
                          "URL" => "http://ecx.images-amazon.com/images/I/41SXDS9-wDL._SL75_.jpg",
                          "Width" => "62"
                        },
                        "Title" => "Wolverine T-shirt Different Colors (Small, Black)",
                        "{http://www.w3.org/XML/1998/namespace}lang" => "en-US"
                      }
                    },
                    "Identifiers" => %{
                      "MarketplaceASIN" => %{
                        "ASIN" => "B01C26TGYA",
                        "MarketplaceId" => "ATVPDKIKX0DER"
                      }
                    },
                    "Relationships" => %{
                      "VariationParent" => %{
                        "Identifiers" => %{
                          "MarketplaceASIN" => %{
                            "ASIN" => "B01C26TG90",
                            "MarketplaceId" => "ATVPDKIKX0DER"
                          }
                        }
                      }
                    },
                    "SalesRankings" => %{}
                  }
                ]
              }
            },
            "ResponseMetadata" => %{"RequestId" => "06e0cea7-4e06-4ced-9cc1-e130cb1a2735"}
          }
        }
      end

      def categories_by_asin_factory do
        %{
          "GetProductCategoriesForASINResponse" => %{
            "GetProductCategoriesForASINResult" => %{
              "Self" => [
                %{
                  "Parent" => %{
                    "Parent" => %{
                      "Parent" => %{
                        "ProductCategoryId" => "7141123011",
                        "ProductCategoryName" => "Clothing, Shoes & Jewelry"
                      },
                      "ProductCategoryId" => "7141124011",
                      "ProductCategoryName" => "Departments"
                    },
                    "ProductCategoryId" => "7147441011",
                    "ProductCategoryName" => "Men"
                  },
                  "ProductCategoryId" => "7581669011",
                  "ProductCategoryName" => "Shops"
                },
                %{
                  "Parent" => %{
                    "Parent" => %{
                      "Parent" => %{
                        "Parent" => %{
                          "Parent" => %{
                            "ProductCategoryId" => "7141123011",
                            "ProductCategoryName" => "Clothing, Shoes & Jewelry"
                          },
                          "ProductCategoryId" => "7141124011",
                          "ProductCategoryName" => "Departments"
                        },
                        "ProductCategoryId" => "7147445011",
                        "ProductCategoryName" => "Novelty & More"
                      },
                      "ProductCategoryId" => "12035955011",
                      "ProductCategoryName" => "Clothing"
                    },
                    "ProductCategoryId" => "7586148011",
                    "ProductCategoryName" => "Band & Music Fan"
                  },
                  "ProductCategoryId" => "1252178011",
                  "ProductCategoryName" => "T-Shirts"
                }
              ]
            },
            "ResponseMetadata" => %{"RequestId" => "cb4f02da-6c5d-4014-ba4b-dd9d3914dba5"}
          }
        }
      end

      def submit_product_feed_data do
        [
          {[
             parentage: "parent",
             color: "white",
             size: "S",
             activefrom: "2017-03-09T02:21:07.763Z",
             description: "<p>bar baz</p>",
             tags: ["a", "b", "c", "d"],
             title: "foo",
             activefrom: "2017-03-09T02:21:07.763Z",
             code: "PARENTSKU123",
             retailprice: %{"currency" => "USD", "value" => 1000},
             saleprice: %{"currency" => "USD", "value" => 0}
           ], 1},
          {[
             parentage: "child",
             color: "white",
             size: "S",
             activefrom: "2017-03-09T02:21:07.763Z",
             description: "<p>bar baz</p>",
             tags: ["a", "b", "c", "d"],
             title: "foo",
             activefrom: "2017-03-09T02:21:07.763Z",
             code: "SKU123",
             retailprice: %{"currency" => "USD", "value" => 1000},
             saleprice: %{"currency" => "USD", "value" => 0}
           ], 2}
        ]
      end

      def submit_images_feed_data do
        [
          [
            {[
               sku: "SKU-ABC",
               type: "Main",
               location:
                 "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/487/DeathStar_400x390.jpg"
             ], 1},
            {[
               sku: "SKU-ABC",
               type: "PT",
               location:
                 "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/487/CuCeQ0IXEAAnpVx.jpg",
               id: 1
             ], 2}
          ],
          [
            [
              sku: "SKU-ABC",
              type: "Swatch",
              location:
                "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-1-1.jpg",
              idx: 3
            ],
            [
              sku: "SKU-ABC",
              type: "Swatch",
              location:
                "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-2-0.jpg",
              idx: 4
            ],
            [
              sku: "SKU-ABC",
              type: "Swatch",
              location:
                "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-2-2.jpg",
              idx: 5
            ],
            [
              sku: "SKU-ABC",
              type: "Swatch",
              location:
                "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-0-1.jpg",
              idx: 6
            ]
          ]
        ]
      end
    end
  end
end
