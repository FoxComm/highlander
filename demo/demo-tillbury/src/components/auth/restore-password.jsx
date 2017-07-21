/* @flow */

import React from 'react';

import RestorePasswordForm from './restore-password-form';

import type { RestorePasswordFormProps } from './restore-password-form';

const RestorePassword = (props: RestorePasswordFormProps) => {
  return (
    <RestorePasswordForm
      title="Reset your password"
      topMessage="Please enter the email address associated with your account.\
A link to reset your password will be emailed to you."
      {...props}
    />
  );
};

export default RestorePassword;
