# Solomon

These are the basic steps that you should take to get the application running:

  * Install dependencies with `mix deps.get`
  * Create and migrate your database with `mix ecto.create`
  * This application shares a database with phoenix.  *Be sure to run phoenix migrations*: `flyway migrate -X -locations=filesystem:`pwd`
  * Start Phoenix endpoint with `mix phoenix.server`

Now you can visit [`localhost:4002`](http://localhost:4002) from your browser.

