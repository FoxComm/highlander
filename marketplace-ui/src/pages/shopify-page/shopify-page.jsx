/* @flow */

import { get, isEmpty } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { connect } from 'react-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';
import ThanksOrNot from '../../components/thanks-or-not/thanks-or-not';
import Loader from '../../components/loader/loader';

import {
  getApplication,
  getShopify,
  getShopifySubmitInProgress,
  getShopifySubmitFailed,
} from '../../core/modules';

import { submit } from '../../core/modules/shopify-integration';

import { fields as shopifyFields } from '../../forms/shopify/shopify-fields';

import styles from './shopify-page.css';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';
import type { ShopifyIntegration } from '../../core/modules/shopify-integration';

type Props = {
  shopify: ShopifyIntegration,
  application: Application;
  shopifySubmitInProgress: boolean;
  shopifySubmitFailed: boolean;
  submit: (data: Object) => Promise<*>;
}

const TIMEOUT_REDIRECT = 3000;

class ShopifyPage extends Component {
  props: Props;

  componentWillReceiveProps(nextProps: Props) {
    if (!isEmpty(nextProps.shopify)) {
      this.handleInfoSucceeded();
    }
  }

  handleInfoSucceeded() {
    if (!window) {
      return;
    }

    setTimeout(
      () => window.location.replace(window.__ASHES_URL__),
      TIMEOUT_REDIRECT
    );
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

  get loader(): HTMLElement {
    if (isEmpty(this.props.shopify)) {
      return;
    }

    return (
      <ThanksOrNot
        className={styles.thanksOrNot}
        title="You're done!"
        message="You're being redirected to admin page now"
      >
        <Loader />
      </ThanksOrNot>
    );
  }

  get form(): HTMLElement {
    if (!isEmpty(this.props.shopify)) {
      return;
    }

    const { shopifySubmitInProgress, shopifySubmitFailed } = this.props;

    return (
      <Form
        form="shopify"
        fields={shopifyFields}
        onSubmit={this.submit}
        inProgress={shopifySubmitInProgress}
        failed={shopifySubmitFailed}
      />
    );
  }

  render(): HTMLElement {
    return (
      <div className={styles.shopify}>
        <Header
          title="Integrate with your Shopify storefront"
          legend={`If you have an existing Shopify storefront, we can automatically import your products and
          synchronize your orders, inventory, and shipments.`}
        />
        {this.loader}
        {this.form}
      </div>
    );
  }
}

const mapState = state => ({
  shopify: getShopify(state),
  application: getApplication(state),
  shopifySubmitInProgress: getShopifySubmitInProgress(state),
  shopifySubmitFailed: getShopifySubmitFailed(state),
});

export default connect(mapState, { submit })(ShopifyPage);
