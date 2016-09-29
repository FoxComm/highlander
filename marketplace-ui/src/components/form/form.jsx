/* @flow */

import cx from 'classnames';
import autosize from 'autosize';
import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import { reduxForm } from 'redux-form';

import styles from './form.css';

import validate from '../../core/lib/validation';
import Button from '../../components/button/button';
import renderField from '../../components/fields/fields';

import type { HTMLElement, FormField } from '../../core/types';

type Props = {
  fields: Array<FormField>;
  submitText?: string;
  handleSubmit: Function; // passed by reduxForm
  inProgress: boolean;
  failed: boolean;
}

class Form extends Component {
  props: Props;

  static defaultProps = {
    submitText: 'Submit',
  };

  componentDidMount(): void {
    autosize(ReactDOM.findDOMNode(this).querySelectorAll('textarea'));
  }

  componentWillUnmount(): void {
    autosize.destroy(ReactDOM.findDOMNode(this).querySelectorAll('textarea'));
  }

  render(): HTMLElement {
    const { fields, submitText, inProgress, failed, handleSubmit } = this.props;


    return (
      <form onSubmit={handleSubmit} noValidate>
        {fields.map((item: FormField) => renderField(item))}

        <Button active={inProgress} disabled={inProgress}>{submitText}</Button>
        {<span className={cx(styles.error, { [styles.errorActive]: failed })}>Error submitting form.</span>}
      </form>
    );
  }
}

export default reduxForm({
  validate: (values: Array<FormField>, props: Props) => validate(props.fields)(values),
})(Form);
