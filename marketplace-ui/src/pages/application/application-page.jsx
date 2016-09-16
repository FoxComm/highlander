/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';
import ThankYou from '../../components/thank-you/thank-you';

import { getApplication, getApplicationInProgress, getApplicationFailed } from '../../core/modules';
import { fetch, submit } from '../../core/modules/merchant-application';
import { fields } from '../../forms/application/application-fields';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';

type Props = {
  params: Object;
  application: Application;
  fetch: (reference: string) => Promise;
  submit: (data: Object) => Promise;
  inProgress: boolean;
  failed: boolean;
}

class MerchantApplicationPage extends Component {
  props: Props;

  componentWillMount(): void {
    const { fetch, params: { ref } } = this.props;

    if (ref) {
      fetch(ref);
    }
  }

  get thank() {
    if (!this.props.application.reference_number) {
      return;
    }

    return (
      <ThankYou
        message={
          <span>
            Your application has been submitted and will be evaluated soon.<br/>
            Your application id is: {this.props.application.reference_number}<br/>
            You will hear from us as soon as your account has been approved.
          </span>
        }
      />
    );
  }

  get form() {
    if (this.props.application.id) {
      return;
    }

    const { submit, inProgress, failed } = this.props;

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
          inProgress={inProgress}
          failed={failed}
          submitText="Apply"
        />
      </div>
    );
  }

  render(): HTMLElement {
    return (
      <div>
        {this.thank}
        {this.form}
      </div>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  inProgress: getApplicationInProgress(state),
  failed: getApplicationFailed(state),
});

export default connect(mapState, { fetch, submit })(MerchantApplicationPage);
