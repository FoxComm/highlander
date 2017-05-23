/* @flow */

import get from 'lodash/get';
import cx from 'classnames';
import autosize from 'autosize';
import React, { Component } from 'react';
import { findDOMNode } from 'react-dom';
import { reduxForm } from 'redux-form';

import styles from './form.css';

import validate from '../../core/lib/validation';
import Button from '../../components/button/button';
import renderField from '../../components/fields/fields';

import type { HTMLElement, FormField } from '../../core/types';

type Props = {
  fields: Array<FormField>;
  formValues?: Array<string>;
  submitText?: string;
  handleSubmit: Function; // passed by reduxForm
  inProgress: boolean;
  failed: boolean;
  className?: string;
  renderFields?: Function;
}

class Form extends Component {
  props: Props;

  static defaultProps = {
    submitText: 'Submit',
  };

  componentDidMount(): void {
    autosize(findDOMNode(this).querySelectorAll('textarea'));
  }

  componentWillUnmount(): void {
    autosize.destroy(findDOMNode(this).querySelectorAll('textarea'));
  }

  get fields(): HTMLElement {
    const { fields, formValues, renderFields } = this.props;

    if (renderFields) {
      return renderFields(fields);
    }

    return fields.map((item: FormField) => renderField(item, formValues));
  }

  render(): HTMLElement {
    const { submitText, inProgress, failed, handleSubmit, className } = this.props;

    return (
      <form className={cx(styles.form, className)} onSubmit={handleSubmit} noValidate>
        {this.fields}

        <Button type="submit" active={inProgress} disabled={inProgress}>{submitText}</Button>
        {<span className={cx(styles.error, { [styles.errorActive]: failed })}>Error submitting form.</span>}
      </form>
    );
  }
}

export default reduxForm({
  validate: (values: Array<FormField>, props: Props) => validate(props.fields)(values),
})(Form);
