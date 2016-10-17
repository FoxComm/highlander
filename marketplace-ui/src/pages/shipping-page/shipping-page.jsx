/* @flow */

import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import ShippingForm from '../../forms/shipping/shipping-form';

import {
  getApplication,
  getShipping,
  getShippingSubmitInProgress,
  getShippingSubmitFailed,
} from '../../core/modules';
import { submit } from '../../core/modules/shipping-solution';

import { fields } from '../../forms/shipping/shipping-fields';

import styles from './shipping-page.css';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';

type Props = {
  params: Object;
  application: Application;
  shippingSubmitInProgress: boolean;
  shippingSubmitFailed: boolean;
  submit: (data: Object) => Promise<*>;
  replace: (path: string) => void;
}


class ShippingPage extends Component {
  props: Props;

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.shipping.length !== nextProps.shipping.length) {
      this.props.replace(`/application/${this.props.params.ref}/actions`);
    }
  }

  @autobind
  submit(data) {
    const merchantId = get(this.props.application, 'merchant.id');

    if (!merchantId) {
      console.error('No merchantId');

      return;
    }

    return this.props.submit(merchantId, data);
  }

  get form(): HTMLElement {
    const { shippingSubmitInProgress, shippingSubmitFailed } = this.props;

    return (
      <ShippingForm
        form="shipping"
        fields={fields}
        onSubmit={this.submit}
        inProgress={shippingSubmitInProgress}
        failed={shippingSubmitFailed}
        className={styles.form}
      />
    );
  }

  render(): HTMLElement {
    return (
      <div className={styles.shipping}>
        <Header
          title="What is your shipping solution"
          legend={'Choose from existing solution providers or contact us to discuss other options.'}
        />
        <div className={styles.forms}>
          {this.form}
        </div>
      </div>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  shipping: getShipping(state),
  shippingSubmitInProgress: getShippingSubmitInProgress(state),
  shippingSubmitFailed: getShippingSubmitFailed(state),
});

export default connect(mapState, { submit, replace })(ShippingPage);
