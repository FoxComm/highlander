export function configureUserData(user) {
  return {
    id: user.id,
    name: user.form.attributes.firstAndLastName.v,
    email: user.form.attributes.emailAddress.v,
    phone: user.form.attributes.phoneNumber.v,
    accountState: user.accountState,
  };
}

export function configureUserState(user) {
  const { name, email, phone, accountState, ...rest } = user;

  const attributes = {
    'firstAndLastName': {
      v: name,
      t: 'string'
    },
    'emailAddress': {
      v: email,
      t: 'string'
    },
    'phoneNumber': {
      v: phone,
      t: 'string'
    }
  };

  const state = {
    accountState,
    disabled: accountState === 'invited' || accountState === 'archived',
};

  const form = {attributes};

  return {
    name,
    form,
    state,
    ...rest
  };
}
