export function configureUserData(user) {
  return {
    id: user.id,
    name: user.form.attributes.firstAndLastName.v,
    email: user.form.attributes.emailAddress.v,
    phoneNumber: user.form.attributes.phoneNumber.v,
    org: user.form.attributes.org.v,
    state: user.accountState.state,
  };
}

export function configureUserState(user) {
  const { name, email, phoneNumber, state, org, ...rest } = user;

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
    },
    'org': {
      v: org,
      t: 'string',
    },
  };

  const accountState = {
    state,
    disabled: state === 'invited' || state === 'archived',
  };

  const schema = {
    type: 'object',
    description: 'User attributes',
    properties: {
      firstAndLastName: { title: 'First & Last Name', type: 'string' },
      org: { title: 'Organization', type: 'string' },
      emailAddress: { title: 'Email Address', type: 'string' },
    },
    required: ['firstAndLastName', 'emailAddress' , 'org'],
  };

  const form = {attributes, };

  return {
    name,
    form,
    accountState,
    schema,
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
    org: 'tenant',
  };

  return configureUserState(user);
}

export function configureStateData(state) {
  return {
    state
  };
}
