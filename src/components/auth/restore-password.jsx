/* @flow */

import React from 'react';

import RestorePasswordForm from './restore-password-form';

import type { RestorePasswordFormProps } from './restore-password-form';

const RestorePassword = (props: RestorePasswordFormProps) => {
  return (
    <RestorePasswordForm
      title="FORGOT PASSWORD"
      topMessage="No worries! We’ll email you instructions on how to reset your password."
      {...props}
    />
  );
};

export default RestorePassword;
