export function configureUserData(user) {
  return {
    id: user.id,
    name: user.form.attributes.firstAndLastName.v,
    email: user.form.attributes.emailAddress.v,
    phoneNumber: user.form.attributes.phoneNumber.v,
    state: user.accountState.state,
  };
}

export function configureUserState(user) {
  const { name, email, phoneNumber, state, ...rest } = user;

  const attributes = {
    'firstAndLastName': {
      v: name,
      t: 'string',
    },
    'emailAddress': {
      v: email,
      t: 'string'
    },
    'phoneNumber': {
      v: phoneNumber,
      t: 'string'
    }
  };

  const options = {
    'firstAndLastName': {
      required: true,
    },
    'emailAddress': {
      required: true,
    }
  };

  const accountState = {
    state,
    disabled: state === 'invited' || state === 'archived',
};

  const form = {attributes, };

  return {
    name,
    form,
    accountState,
    ...rest
  };
}

export function createEmptyUser() {
  const user = {
    id: '',
    name: '',
    email: '',
    phoneNumber: '',
    state: 'invited',
  };

  return configureUserState(user);
}
