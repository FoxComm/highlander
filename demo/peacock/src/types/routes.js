export type Route = {
  path: string,
  name: string,
  indexRoute?: Object,
  titleParam?: Object,
};

export type RoutesParams = {
  rotues: Array<Route>,
  params: Object,
};
