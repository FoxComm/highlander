/* @flow */

import { get, isEmpty } from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';
import ThanksOrNot from '../../components/thanks-or-not/thanks-or-not';
import Button from '../../components/button/button';
import styles from './application-page.css';

import {
  getApplication,
  getApplicationSubmitInProgress,
  getApplicationSubmitFailed,
} from '../../core/modules';

import { submit } from '../../core/modules/merchant-application';
import { fields } from '../../forms/application/application-fields';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';

type Props = {
  replace: (path: string) => void;
  application: Application;
  submit: (data: Object) => Promise<*>;
  submitInProgress: boolean;
  submitFailed: boolean;
}

class MerchantApplicationPage extends Component {
  props: Props;

  componentWillReceiveProps(nextProps: Props) {
    const oldRef = get(this.props, 'application.reference_number');
    const newRef = get(nextProps, 'application.reference_number');

    if (!oldRef && newRef) {
      this.props.replace(`/application/${newRef}`);
    }
  }

  get thank(): HTMLElement {
    const { application } = this.props;

    if (isEmpty(application)) {
      return;
    }

    const message = (
      <span>
        Your application has been submitted and will be evaluated soon.<br />
        Your application id is:<br />{application.reference_number}<br />
        You will hear from us as soon as your account has been approved.
      </span>
    );

    return <ThanksOrNot message={message} />;
  }

  get form(): HTMLElement {
    if (!isEmpty(this.props.application)) {
      return;
    }

    const { submit, submitInProgress, submitFailed } = this.props;

    return (
      <Form
        form="application"
        fields={fields}
        onSubmit={submit}
        inProgress={submitInProgress}
        failed={submitFailed}
        submitText="Apply"
      />
    );
  }

  get applyMessage(): HTMLElement {
    if (!isEmpty(this.props.application)) {
      return;
    }
    return (
      <div className={styles.apply}>
        <div className={styles.applybutton}>
          <Button onClick={() => window.location.replace("http://foxcommerce.com/#join-beta")}>Apply Now</Button>
        </div>
      </div>);

  }

  render(): HTMLElement {
    return (
      <div>
        <Header
          title="Apply to Sell"
          legend="Apply to be a merchant on the Fox Platform."
        />
      {this.thank}
      {this.applyMessage}
      </div>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  submitInProgress: getApplicationSubmitInProgress(state),
  submitFailed: getApplicationSubmitFailed(state),
});

export default connect(mapState, { submit, replace })(MerchantApplicationPage);
