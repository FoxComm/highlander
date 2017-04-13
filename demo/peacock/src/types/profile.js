type Account = {
  name: string,
  email: string,
  isGuest: boolean,
  id: number,
};

type EmptyAccount = {
  email: void,
  name: void,
};

export type AcountDetailsProps = {
  account: Account | EmptyAccount,
};
