# Geronimo

<img src="http://a398.idata.over-blog.com/0/51/39/34/indien/apache.jpg"/>

Fox CMS system.

Access to `/v1/admin` allowed with JWT only.

Design documents can be found [in the wiki](https://github.com/FoxComm/highlander/wiki/cms)

## Setup

* install elixir as described [in the docs](https://elixir-lang.org/install.html)
* install postgres 9.2 or higher
* install `temporal_table` extension as described [in the docs](https://github.com/arkhipov/temporal_tables)
* install dependencies: `mix deps.get`
* create and migrate db (see [Makefile](https://github.com/FoxComm/protos/blob/master/geronimo/Makefile))
* create `.env.dev` file in the root of a project (see definitions below)
* run the application `iex -S mix`

**Important!**

You may get this error when `make build` task is running:

```
16:23:11.551 [error] GenServer #PID<0.132.0> terminating
** (stop) normal
    (inets) httpc_handler.erl:1676: :httpc_handler.update_session/4
    (inets) httpc_handler.erl:1316: :httpc_handler.maybe_make_session_available/2
    (inets) httpc_handler.erl:1307: :httpc_handler.answer_request/3
    (inets) httpc_handler.erl:371: :httpc_handler.terminate/2
    (stdlib) gen_server.erl:629: :gen_server.try_terminate/3
    (stdlib) gen_server.erl:795: :gen_server.terminate/7
    (stdlib) proc_lib.erl:247: :proc_lib.init_p_do_apply/3
Last message: {:ssl, {:sslsocket, {:gen_tcp, #Port<0.4745>, :tls_connection, :undefined}, #PID<0.133.0>}, "HTTP/1.1 200 OK\r\nx-amz-id-2: QcFzHmTb0kHmmU+Ev6Apxx5QA1KtgoxIH5/vgi6C26A0WTCc+TEZwJ/cnGfiQ7Un2vkrU4OU/gQ=\r\nx-amz-request-id: 4DC01FBDC05A747A\r\nx-amz-replication-status: COMPLETED\r\nLast-Modified: Mon, 08 May 2017 12:35:20 GMT\r\nETag: \"034e39a16f5026562499bd0848d14eff\"\r\nCache-Control: public, max-age=604800\r\nx-amz-meta-surrogate-key: installs\r\nx-amz-version-id: _PqAeBBOvSlcxbRraFEd18ofBaqWJyCR\r\nContent-Type: binary/octet-stream\r\nServer: AmazonS3\r\nVia: 1.1 varnish\r\nFastly-Debug-Digest: 864530c58fa2f5e6f7b455ba1b8a8fd668c94f688c98b6cdc85319515ff6f6b1\r\nContent-Length: 350\r\nAccept-Ranges: bytes\r\nDate: Thu, 08 Jun 2017 09:23:11 GMT\r\nVia: 1.1 varnish\r\nAge: 596054\r\nConnection: keep-alive\r\nX-Served-By: cache-iad2122-IAD, cache-bma7031-BMA\r\nX-Cache: HIT, HIT\r\nX-Cache-Hits: 1, 1\r\nX-Timer: S1496913791.390283,VS0,VE0\r\n\r\nhfGB2dwLVri/QWfHA1wy9bkOnV6jaMG+P5Fku1K7aShGh65+sY1V7+ADIva+nf/o\nAOVZS7+1YW0vanQj+mwJsLMRvjLmPvcRMaqaal7AkT81G5GNy6ExXLMZ1n3Yak6L\nl0dRVI6t/faBgHlv7x2baIy8G/8O2j4XCiYE8n7zL0QnpzZz7Z2YybCgoaYk/oHT\nAzy7Mp8Sy8ugtVjs2nNDVGHZHMhrpHri1R9FxLLRAbbaxCTUF/PXLvYBBBpxSLxJ\nBUbw8RH8u61LkftXsHphA3JCMnXKMYybcpuMA7WOyIpZoqLcM5cCrrCZVLW8DhXN\nAH3LXp27yb4Q+oc8QESFPw==\n"}
State: {:state, {:request, #Reference<0.0.3.324>, #PID<0.70.0>, 0, :https, {'repo.hex.pm', 443}, '/installs/rebar3-1.x.csv.signed', [], :get, {:http_request_h, :undefined, 'keep-alive', :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, 'repo.hex.pm', :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, :undefined, [], 'Mix/1.4.4', :undefined, :undefined, :undefined, '0', :undefined, :undefined, :undefined, :undefined, :undefined, ...}, {[], []}, {:http_options, 'HTTP/1.1', :infinity, true, {:essl, []}, :undefined, true, :infinity, false}, 'https://repo.hex.pm/installs/rebar3-1.x.csv.signed', [], :none, [], 1496913791071, :undefined, :undefined, false}, {:session, {{'repo.hex.pm', 443}, #PID<0.132.0>}, false, :https, {:sslsocket, {:gen_tcp, #Port<0.4745>, :tls_connection, :undefined}, #PID<0.133.0>}, {:essl, []}, 1, :keep_alive, false}, :undefined, :undefined, :undefined, {:httpc_response, :parse, [:nolimit, true]}, {[], []}, {[], []}, :new, [], :nolimit, :nolimit, {:options, {:undefined, []}, {:undefined, []}, 0, 2, 5, 120000, 2, :disabled, false, :inet, :default, :default, []}, {:timers, [], :undefined}, :httpc_mix, :inactive}
* creating /Users/retgoat/.mix/rebar3
```
This actually is not an issue. This happens because `rebar` (Erlang build tool) is deprecated in favor of `rebar3`.
Mix tries to install `rebar` gets an error and installs `rebar3` then.

Just ignore the above error.

### .env.dev

This file used by `Envy` module.

```
# db
GERONIMO_DB_HOST=localhost
GERONIMO_DB_USER=geronimo
GERONIMO_DB_NAME=geronimo_development
GERONIMO_DB_PASSWORD=''
PUBLIC_KEY=/path/to/public_key.pem
```

## Available routes

```
v1   GET    /v1/public/entities                  Get all created entities
v1   GET    /v1/public/entities/:id              Get entity with specific ID
--
v1   GET    /v1/admin/content_types              Get available content types
v1   GET    /v1/admin/content_types/:id          Get content type with given id
v1   POST   /v1/admin/content_types              Creates new content type
v1   PUT    /v1/admin/content_types/:id          Updates content type with given id
v1   GET    /v1/admin/content_types/:id/versions Get specific version
v1   PUT    /v1/admin/content_types/:id/restore  Restore specific version
v1   POST   /v1/admin/entities                   Creates new entity
v1   PUT    /v1/admin/entities/:id               Updates entity with given id
v1   GET    /v1/admin/entities/:id/versions      Get specific version
v1   PUT    /v1/admin/entities/:id/restore       Restore specific version
```

Postman collection: [https://www.getpostman.com/collections/f533132ddcafdec803b5](https://www.getpostman.com/collections/f533132ddcafdec803b5)

## TODOS

* Scope restricted access — Partially done: only `admin`
* Public routes
* Move `index` action to elastic
* Add pushing all created objects to Kafka
* Add unittests
* ~~Entity validation against `ContentType`~~

## API docs

All requests to `/v1/admin` **must** have `jwt` header.

### ContentType

Each `ContentType` describes kind of `Entity`

#### Create ContentType



Params:

|Name|Type|Description|Required?|
|----|----|-----------|---------|
|schema|JSON|JSON that describes an `Entity` aka `Schema`|Yes|
|name|String|Name of a given ContentType|Yes|

**Request:**
`POST /v1/admin/content_types`

```json
{
  "schema": {
        "title": {
          "type": ["string"],
          "required": true
        },
        "body": {
          "type": ["string"],
          "widget": "richText",
          "required": true
        },
        "author": {
          "type": ["string"],
          "required": true
        },
        "tags": {
          "type": ["array", []],
          "required": false
        }
  },
  "name":"BlogPost"
}
```

**Response**

```json
{
    "updated_at": "2017-06-12T02:52:36Z",
    "scope": "1",
    "schema": {
        "title": {
            "type": [
                "string"
            ],
            "required": true
        },
        "tags": {
            "type": [
                "array",
                []
            ],
            "required": false
        },
        "body": {
            "widget": "richText",
            "type": [
                "string"
            ],
            "required": true
        },
        "author": {
            "type": [
                "string"
            ],
            "required": true
        }
    },
    "name": "BlogPost",
    "inserted_at": "2017-06-12T02:52:36Z",
    "id": 2,
    "created_by": 4
}
```

#### Update ContentType

Params:

|Name|Type|Description|Required?|
|----|----|-----------|---------|
|schema|JSON|JSON that describes an `Entity` aka `Schema`|Yes|
|name|String|Name of a given ContentType|Yes|
|content\_type\_id|Integer|ContentType ID|Yes|

**Request:**
`PUT /v1/admin/content_types/:content_type_id`

```json
{
	"name": "BlogPost",
	"schema": {
        "title": {
          "type": ["string"],
          "required": true
        },
        "body": {
          "type": ["string"],
          "widget": "richText",
          "required": true
        },
        "author": {
          "type": ["string"],
          "required": true
        },
        "tags": {
          "type": ["array", ["a"]],
          "required": false
        }
  	}
}
```

**Response:**

```json
{
    "versions": [
        "2017-06-12T02:59:47.855854Z"
    ],
    "updated_at": "2017-06-12T02:59:47Z",
    "scope": "1",
    "schema": {
        "title": {
            "type": [
                "string"
            ],
            "required": true
        },
        "tags": {
            "type": [
                "array",
                [
                    "a"
                ]
            ],
            "required": false
        },
        "body": {
            "widget": "richText",
            "type": [
                "string"
            ],
            "required": true
        },
        "author": {
            "type": [
                "string"
            ],
            "required": true
        }
    },
    "name": "BlogPost",
    "inserted_at": "2017-06-12T02:20:01Z",
    "id": 1,
    "created_by": 4
}
```

#### Get all content types

**Request:**
`GET /v1/admin/content_types`

**Response:**

```json
{
    "items": [
        {
            "updated_at": "2017-06-12T02:52:36Z",
            "scope": "1",
            "schema": {
                "title": {
                    "type": [
                        "string"
                    ],
                    "required": true
                },
                "tags": {
                    "type": [
                        "array",
                        []
                    ],
                    "required": false
                },
                "body": {
                    "widget": "richText",
                    "type": [
                        "string"
                    ],
                    "required": true
                },
                "author": {
                    "type": [
                        "string"
                    ],
                    "required": true
                }
            },
            "name": "BlogPost",
            "inserted_at": "2017-06-12T02:52:36Z",
            "id": 2,
            "created_by": 4
        },{},{}
    ],
    "count": 2
}
```

#### Get one content type

**Request:**
`GET /v1/admin/content_types/:content_type_id`

**Response:**

```json
{
    "versions": [
        "2017-06-12T02:59:47.855854Z"
    ],
    "updated_at": "2017-06-12T02:59:47Z",
    "scope": "1",
    "schema": {
        "title": {
            "type": [
                "string"
            ],
            "required": true
        },
        "tags": {
            "type": [
                "array",
                [
                    "a"
                ]
            ],
            "required": false
        },
        "body": {
            "widget": "richText",
            "type": [
                "string"
            ],
            "required": true
        },
        "author": {
            "type": [
                "string"
            ],
            "required": true
        }
    },
    "name": "BlogPost",
    "inserted_at": "2017-06-12T02:20:01Z",
    "id": 1,
    "created_by": 4
}
```

#### Get version of a ContentType

Params:

|Name|Type|Description|Required?|
|----|----|-----------|---------|
|ver|DateTime|Version|Yes|
|content\_type\_id|Integer|ContentType ID|Yes|

**Request:**
`GET /v1/admin/content_types/:content_type_id/versions?ver=:ver`

**Response:**

```json
{
    "updated_at": "2017-06-12T02:20:01.0Z",
    "schema": {
        "title": {
            "type": [
                "string"
            ],
            "required": true
        },
        "tags": {
            "type": [
                "array",
                []
            ],
            "required": false
        },
        "body": {
            "widget": "richText",
            "type": [
                "string"
            ],
            "required": true
        },
        "author": {
            "type": [
                "string"
            ],
            "required": true
        }
    },
    "inserted_at": "2017-06-12T02:20:01.0Z",
    "id": 1
}
```

#### Restore ContentType at given version

Params:

|Name|Type|Description|Required?|
|----|----|-----------|---------|
|ver|DateTime|Version|Yes|
|content\_type\_id|Integer|ContentType ID|Yes|

**Request:**
`PUT /v1/admin/content_types/:content_type_id/restore?ver=:ver`

**Response:**

```json
{
    "versions": [
        "2017-06-12T02:59:47.855854Z",
        "2017-06-12T03:13:46.091353Z"
    ],
    "updated_at": "2017-06-12T03:13:46Z",
    "scope": "1",
    "schema": {
        "title": {
            "type": [
                "string"
            ],
            "required": true
        },
        "tags": {
            "type": [
                "array",
                []
            ],
            "required": false
        },
        "body": {
            "widget": "richText",
            "type": [
                "string"
            ],
            "required": true
        },
        "author": {
            "type": [
                "string"
            ],
            "required": true
        }
    },
    "name": "BlogPost",
    "inserted_at": "2017-06-12T02:20:01Z",
    "id": 1,
    "created_by": 4
}
```

### Entity

Each `Entity` is an instance of one `ContentType`

#### Create Entity

Entity validated against corresponding `ContentType` schema. If `Entity` is not valid — error message will be returned:

```json
{
    "error": [
        {
            "body": "must be present"
        }
    ]
}
```

Params:

|Name|Type|Description|Required?|
|----|----|-----------|---------|
|content_type_id|Integer|Corresponding ContentType id|Yes|
|content|JSON|Content of an Entity|Yes|
|storefront|string|Storefront on which given entity has been created|Yes|


**Request:**
`POST /v1/admin/entities/`

```json
{
	"content_type_id": 1,
    "storefront": "foo.bar",
	"content": {
		"title":"Some title foooooo",
		"body":"Lorem ipsum",
		"author":"John Doe",
		"tags":["tag", "another"]
	}
}
```

**Response:**

```json
{
    "updated_at": "2017-06-12T03:15:17Z",
    "scope": "1",
    "schema_version": "2017-06-12T03:13:46Z",
    "storefront": "foo.bar",
    "kind": "BlogPost",
    "inserted_at": "2017-06-12T03:15:17Z",
    "id": 3,
    "created_by": 4,
    "content_type_id": 1,
    "content": {
        "title": "Some title foooooo",
        "tags": [
            "tag",
            "another"
        ],
        "body": "Lorem ipsum",
        "author": "John Doe"
    }
}
```

#### Update Entity

`Entity` validated against corresponding `ContentType` schema. If `Entity` is not valid — error message will be returned:

```json
{
    "error": [
        {
            "body": "must be present"
        }
    ]
}
```

Params:

|Name|Type|Description|Required?|
|----|----|-----------|---------|
|content_type_id|Integer|Corresponding ContentType id|Yes|
|content|JSON|Content of an Entity|Yes|

**Request:**
`PUT /v1/admin/entities/:entity_id`

```json
{
	"content_type_id":1,
	"content": {
		"title":"Some another title",
		"body":"asdgagawgwg",
		"author":"Karl Marx",
		"tags":["tag", "another", "one_more"]
	}
}
```

**Response:**

```json
{
    "versions": [
        "2017-06-12T02:22:33.533414Z",
        "2017-06-12T02:24:04.955661Z",
        "2017-06-12T02:24:39.664791Z",
        "2017-06-12T02:25:07.708935Z",
        "2017-06-12T02:25:38.749662Z",
        "2017-06-12T03:22:10.841047Z"
    ],
    "updated_at": "2017-06-12T03:22:10Z",
    "scope": "1",
    "schema_version": "2017-06-12T02:20:01.000000Z",
    "kind": "BlogPost",
    "inserted_at": "2017-06-12T02:22:12Z",
    "id": 1,
    "created_by": 4,
    "content_type_id": 1,
    "content": {
        "title": "Some another title",
        "tags": [
            "tag",
            "another",
            "one_more"
        ],
        "body": "asdgagawgwg",
        "author": "Karl Marx"
    }
}
```

#### Get version of an Entity

Params:

|Name|Type|Description|Required?|
|----|----|-----------|---------|
|ver|DateTime|Version|Yes|
|entity_id|Integer|Entity ID|Yes|

**Request:**
`GET /v1/admin/content_types/:entity_id/versions?ver=:ver`

**Response:**

```json
{
    "updated_at": "2017-06-12T02:22:12.0Z",
    "schema_version": "2017-06-12T02:20:01.0Z",
    "kind": "BlogPost",
    "inserted_at": "2017-06-12T02:22:12.0Z",
    "id": 1,
    "content_type_id": 1,
    "content": {
        "title": "Some title",
        "tags": [
            "tag",
            "another"
        ],
        "body": "Lorem ipsum",
        "author": "John Doe"
    }
}
```

#### Restore Entity at given version

Params:

|Name|Type|Description|Required?|
|----|----|-----------|---------|
|ver|DateTime|Version|Yes|
|entity\_id|Integer|Entity ID|Yes|

**Request:**
`PUT /v1/admin/content_types/:entity_id/restore?ver=:ver`

**Response:**

```json
{
    "versions": [
        "2017-06-12T02:22:33.533414Z",
        "2017-06-12T02:24:04.955661Z",
        "2017-06-12T02:24:39.664791Z",
        "2017-06-12T02:25:07.708935Z",
        "2017-06-12T02:25:38.749662Z",
        "2017-06-12T03:22:10.841047Z"
    ],
    "updated_at": "2017-06-12T03:22:10Z",
    "scope": "1",
    "schema_version": "2017-06-12T02:20:01.000000Z",
    "kind": "BlogPost",
    "inserted_at": "2017-06-12T02:22:12Z",
    "id": 1,
    "created_by": 4,
    "content_type_id": 1,
    "content": {
        "title": "Some another title",
        "tags": [
            "tag",
            "another",
            "one_more"
        ],
        "body": "asdgagawgwg",
        "author": "Karl Marx"
    }
}
```
