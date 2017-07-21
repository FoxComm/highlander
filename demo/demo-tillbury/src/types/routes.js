export type Route = {
  path: string,
  name: string,
  indexRoute?: Object,
  titleParam?: Object,
};

export type RoutesParams = {
  routes: Array<Route>,
  params: Object,
};
