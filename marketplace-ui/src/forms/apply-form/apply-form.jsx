/* @flow */

import cx from 'classnames';
import React from 'react';
import { Field, reduxForm } from 'redux-form';

import type { HTMLElement } from '../../core/types';

import styles from './apply-form.css';

type Props = {
  handleSubmit: Function; // passed by reduxForm
}

const LIST_AUDIENCE = [
  'Men', 'Women', 'Both', 'Kids',
];

const LIST_CATEGORIES = [
  'Accessories', 'Action', 'Sports', 'Activewear', 'Apparel', 'Beauty', 'Bridal', 'Eyewear',
  'Grooming', 'Handbags', 'Home', 'Intimates', 'Jeans', 'Jewelry', 'Kids', 'Shoes',
  'Sleepwear', 'Swimwear', 'Tech', 'Vintage',
];

const renderInput = ({ input, type, placeholder, meta }) => {
  const hasError = meta.touched && meta.error;

  return (
    <div className={cx(styles.field, { [styles.fieldError]: hasError })}>
      <input {...input} placeholder={placeholder} type={type} />
      {<span className={cx(styles.error, { [styles.errorActive]: hasError })}>{meta.error}</span>}
    </div>
  );
};

const renderSelect = ({ input, values, label, meta }) => {
  const hasError = meta.touched && meta.error;

  return (
    <div className={cx(styles.field, { [styles.fieldError]: hasError })}>
      {!input.value && <label htmlFor={input.name}>{label}</label>}
      <select {...input}>
        <option disabled />
        {Object.keys(values).map(value =>
          <option value={value} key={value}>{values[value]}</option>
        )}
      </select>
      {<span className={cx(styles.error, { [styles.errorActive]: hasError })}>{meta.error}</span>}
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

  const urlRegex = /(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/i;

  if (!!values.twitter && !urlRegex.test(values.twitter)) {
    errors.twitter = 'Invalid URL';
  }

  if (!!values.url && !urlRegex.test(values.url)) {
    errors.url = 'Invalid URL';
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
      />
      <Field
        name="phone"
        placeholder="Phone Number"
        component={renderInput}
      />
      <Field
        name="email"
        placeholder="Email Address"
        component={renderInput}
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
      />
      <Field
        name="url"
        placeholder="Site URL"
        component={renderInput}
      />
      <Field
        name="audience"
        label="Audience"
        component={renderSelect}
        values={LIST_AUDIENCE}
      />
      <Field
        name="category"
        label="Category"
        component={renderSelect}
        values={LIST_CATEGORIES}
      />

      <button type="submit">Submit</button>
    </form>
  );
};

export default reduxForm({ form: 'apply', validate })(ApplyForm);
