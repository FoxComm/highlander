type Customer = {
  id: number,
  email: string,
  name: string,

  phoneNumber?: string,
  isGuest?: boolean,
  groups?: Array<string>,
  avatarUrl?: string,
  rank?: number,
  location?: string,
};
