# Solomon

These are the basic steps that you should take to get the application running:

  * Install dependencies with `mix deps.get`
  * This application shares a database with phoenix.  *Be sure to run phoenix migrations*: ``flyway migrate -X -locations=filesystem:`pwd` ``
  * Start Phoenix endpoint with `source .env && mix phoenix.server`

Now you can visit [`localhost:4002`](http://localhost:4002) from your browser.

## JWT signing and verifying

The application looks for environment variables `private_keys_dest_dir` and
`public_keys_dest_dir` which should be the directories containing
`private_key.pem` and `public_key.pem`, respectively.

You can set these variables in `.env.local` before running `source .env`.
