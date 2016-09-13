/* @flow */

import cx from 'classnames';
import React, { Component } from 'react';
import { Field, reduxForm } from 'redux-form';

import type { HTMLElement } from '../../core/types';

import styles from './apply-form.css';

type Props = {
  onSubmit: Function;
}

const renderInput = ({ input, type, placeholder, meta }) => {
  const hasError = meta.touched && meta.error;

  return (
    <div className={cx(styles.field, { [styles.fieldError]: hasError })}>
      <input {...input} placeholder={placeholder} type={type} />
      {<span className={cx(styles.error, {[styles.errorActive]: hasError})}>{meta.error}</span>}
    </div>
  );
};

const validate = values => {
  const errors = {};
  if (!values.businessName) {
    errors.businessName = 'Required';
  }

  if (!values.phone) {
    errors.phone = 'Required';
  }

  if (!values.email) {
    errors.email = 'Required';
  } else if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i.test(values.email)) {
    errors.email = 'Invalid email address';
  }

  return errors;
};

const ApplyForm = (props: Props): HTMLElement => {
  const { handleSubmit } = props;

  return (
    <form className={styles.applyForm} onSubmit={handleSubmit}>
      <Field
        name="businessName"
        placeholder="Business Name"
        component={renderInput}
        type="text"
      />
      <Field
        name="phone"
        placeholder="Phone Number"
        component={renderInput}
        type="tel"
      />
      <Field
        name="email"
        placeholder="Email Address"
        component={renderInput}
        type="email"
      />
      <Field
        name="monthlySales"
        placeholder="Monthly Sales Volume"
        component={renderInput}
        type="number"
      />
      <Field
        name="twitter"
        placeholder="Twitter Handle"
        component={renderInput}
        type="url"
      />
      <Field
        name="url"
        placeholder="Site URL"
        component={renderInput}
        type="url"
      />
      <button type="submit">Submit</button>
    </form>
  );
};

export default reduxForm({ form: 'apply', validate })(ApplyForm);
