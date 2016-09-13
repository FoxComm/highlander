/* @flow */

import React, { Component } from 'react';
import { Field, reduxForm } from 'redux-form';

import type { HTMLElement } from '../../core/types';

import styles from './apply-form.css';

type Props = {
  onSubmit: Function;
}

const renderInput = ({ input, type, placeholder, meta }) => (
  <div>
    <input {...input} placeholder={placeholder} type={type} />
    {meta.touched && meta.error && <span className="error">{meta.error}</span>}
  </div>
);

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

export default reduxForm({ form: 'apply' })(ApplyForm);
