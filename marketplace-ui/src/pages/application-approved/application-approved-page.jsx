/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';

import ThankYou from '../../components/thanks-or-not/thank-you';

import { getApplication, getApplicationInProgress, getApplicationFailed } from '../../core/modules';
import { fetch, submit } from '../../core/modules/merchant-application';

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

  render(): HTMLElement {
    return (
      <ThankYou
        message={
          <span>
            You application has been accepted.<br />
            You'll be able to create an account after it would be approved.
          </span>
        }
      />
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  inProgress: getApplicationInProgress(state),
  failed: getApplicationFailed(state),
});

export default connect(mapState, { fetch, submit })(MerchantApplicationPage);
