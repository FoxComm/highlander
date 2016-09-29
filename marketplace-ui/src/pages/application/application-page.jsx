/* @flow */

import get from 'lodash/get';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';
import ThanksOrNot from '../../components/thanks-or-not/thanks-or-not';

import {
  getApplication,
  getApplicationFetchFailed,
  getApplicationSubmitInProgress,
  getApplicationSubmitFailed,
} from '../../core/modules';

import { fetch, submit } from '../../core/modules/merchant-application';
import { fields } from '../../forms/application/application-fields';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';

type Props = {
  params: Object;
  replace: (path: string) => void;
  application: Application;
  fetch: (reference: string) => Promise<*>;
  submit: (data: Object) => Promise<*>;
  submitInProgress: boolean;
  fetchFailed: boolean;
  submitFailed: boolean;
}

class MerchantApplicationPage extends Component {
  props: Props;

  componentWillMount(): void {
    const { fetch, params: { ref: refParam }, application: { reference_number: ref, state } } = this.props;

    if (refParam && !ref) {
      fetch(refParam);
    }

    if (ref && state === 'approved') {
      this.props.replace(`/application/${ref}/account`);
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    const oldRef = get(this.props, 'application.reference_number');
    const newRef = get(nextProps, 'application.reference_number');
    const state = get(nextProps, 'application.state');

    if (!oldRef && newRef) {
      this.props.replace(`/application/${newRef}`);
    }

    if (newRef && state === 'approved') {
      this.props.replace(`/application/${newRef}/account`);
    }
  }

  get thank(): HTMLElement {
    const { application: { reference_number: ref } } = this.props;

    if (!ref) {
      return;
    }

    const message = (
      <span>
        Your application has been submitted and will be evaluated soon.<br />
        Your application id is: {ref}<br />
        You will hear from us as soon as your account has been approved.
      </span>
    );

    return <ThanksOrNot message={message} />;
  }

  get error(): HTMLElement {
    const { fetchFailed, params: { ref } } = this.props;
    if (!fetchFailed) {
      return;
    }

    const message = <span>No application with reference number<br />{ref}<br />found.</span>;

    return <ThanksOrNot title="Sorry" message={message} error />;
  }

  get form(): HTMLElement {
    if (this.props.application.id || this.props.fetchFailed) {
      return;
    }

    const { submit, submitInProgress, submitFailed } = this.props;

    return (
      <div>
        <Header
          title="Apply to Sell"
          legend="Apply to be a merchant on the GoldFish marketplace."
        />
        <Form
          form="application"
          fields={fields}
          onSubmit={submit}
          inProgress={submitInProgress}
          failed={submitFailed}
          submitText="Apply"
        />
      </div>
    );
  }

  render(): HTMLElement {
    return (
      <div>
        {this.error}
        {this.thank}
        {this.form}
      </div>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  fetchFailed: getApplicationFetchFailed(state),
  submitInProgress: getApplicationSubmitInProgress(state),
  submitFailed: getApplicationSubmitFailed(state),
});

export default connect(mapState, { fetch, submit, replace })(MerchantApplicationPage);
