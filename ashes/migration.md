## Nodejs 7+

We upgraded `koa` server to version 2, and since it uses `async await`, it requires nodejs version 7+. Upgradeit via `brew` or by official node package. Our production servers already uses nodejs 7+.

## .env

Now server restarts when you change your `.env` file. But format is changed a little bit: you need to remove `exports` word:

`export API_URL=https://stage.foxcommerce.com` → `API_URL=https://stage.foxcommerce.com`

You now also could leave `# comments` inside `.env` file.

## Webpack

It is *highly* recommended to remove `node_modules` completely and reinstall them.

First `make d` run will take 1 minute, second and next runs ~ 20 seconds. All generated files will be in RAM, no files for development.

Run `make p` for production build + server.

## Gulp

No more global gulp required, you can remove it if you want.

All tasks starts with `make`, even those which uses local gulp (`make mocha-main`, for example).

See `Makefile` for details.

## lib

Remove `/lib` dir, it will never be created anymore. All `babel` cache will be hidden in `./node_modules/.cache`.

## HMR

It is working good for css. For js files it is works, but not rerenders changed components, so you need to reload the page or force component to rerender somehow. That is because some libraries uses singleton pattern (`wings/createAsyncActions`, `redux-act`, etc).
