/* @flow */

import React from 'react';

import RestorePasswordForm from './restore-password-form';

import type { RestorePasswordFormProps } from './restore-password-form';

const ForcedRestorePassword = (props: RestorePasswordFormProps) => {
  const message = 'Looks like it’s time to update your password! '
    + 'We’ll send you an email with a link to create a new one.';
  return (
    <RestorePasswordForm
      title="RESET PASSWORD"
      topMessage={message}
      {...props}
    />
  );
};

export default ForcedRestorePassword;
