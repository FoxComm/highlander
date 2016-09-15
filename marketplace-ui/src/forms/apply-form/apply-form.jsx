/* @flow */

import cx from 'classnames';
import autosize from 'autosize';
import React, { Component } from 'react';
import { reduxForm } from 'redux-form';

import styles from './apply-form.css';

import Button from '../../components/button/button';
import renderField from '../../components/fields/fields';
import { fields, validate } from './config';

import type { HTMLElement } from '../../core/types';
import type { FieldConfig } from '../../components/fields/fields';

type Props = {
  handleSubmit: Function; // passed by reduxForm
  inProgress: boolean;
  failed: boolean;
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
    const { inProgress, failed, handleSubmit } = this.props;


    return (
      <form className={styles.applyForm} onSubmit={handleSubmit}>
        {fields.map((item: FieldConfig) => renderField(item))}

        <Button active={inProgress}>Apply</Button>
        {<span className={cx(styles.error, { [styles.errorActive]: failed })}>Error submitting form.</span>}
      </form>
    );
  }
}

export default reduxForm({ form: 'apply', validate })(ApplyForm);
