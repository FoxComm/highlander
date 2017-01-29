type NavLink<T> = {
  title: string,
  to: string,
  key?: string,
  params?: T,
};

type NavLinks<T> = Array<NavLink<T>>;
