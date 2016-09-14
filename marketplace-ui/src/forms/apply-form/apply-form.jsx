/* @flow */

import autosize from 'autosize';
import React, { Component } from 'react';
import { reduxForm } from 'redux-form';

import styles from './apply-form.css';

import renderField from '../../components/fields/fields';
import { fields, validate } from './config';

import type { HTMLElement } from '../../core/types';
import type { FieldConfig } from '../../components/fields/fields';

type Props = {
  handleSubmit: Function; // passed by reduxForm
}

class ApplyForm extends Component {
  props: Props;

  componentDidMount(): void {
    autosize(document.querySelectorAll('textarea'));
  }

  componentWillUnmount(): void {
    autosize.destroy(document.querySelectorAll('textarea'));
  }

  render(): HTMLElement {
    const { handleSubmit } = this.props;

    return (
      <form className={styles.applyForm} onSubmit={handleSubmit}>
        {fields.map((item: FieldConfig) => renderField(item))}

        <button type="submit">Apply</button>
      </form>
    );
  }
}

export default reduxForm({ form: 'apply', validate })(ApplyForm);
