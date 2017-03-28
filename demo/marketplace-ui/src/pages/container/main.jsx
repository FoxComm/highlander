/* @flow */

import { get, isEmpty } from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import { replace } from 'react-router-redux';

import Steps from '../../components/steps/steps';

import {
  getApplication,
  getApplicationApproved,
  getApplicationFetched,
  getApplicationFetchFailed,
  getAccounts,
  getAccountsFetched,
  getFeed,
  getFeedFetched,
  getShipping,
  getShippingFetched,
} from '../../core/modules';

import { fetch as fetchApplication } from '../../core/modules/merchant-application';
import { fetch as fetchAccounts } from '../../core/modules/merchant-account';
import { fetch as fetchFeed } from '../../core/modules/products-feed';
import { fetch as fetchShipping } from '../../core/modules/shipping-solution';

import ThanksOrNot from '../../components/thanks-or-not/thanks-or-not';

import type { HTMLElement } from '../../core/types';

import type { Application } from '../../core/modules/merchant-application';
import type { Accounts } from '../../core/modules/merchant-account';
import type { Shipping } from '../../core/modules/shipping-solution';

import styles from './main.css';

type Props = {
  application: Application;
  accounts: Accounts,
  shipping: Shipping,
  applicationApproved: boolean;
  applicationFetched: boolean;
  applicationFetchFailed: boolean;
  accountsFetched: boolean;
  shippingFetched: boolean;
  fetchApplication: (reference: string) => Promise<*>;
  fetchAccounts: (merchantId: number) => Promise<*>;
  fetchFeed: (merchantId: number) => Promise<*>;
  fetchShipping: (merchantId: number) => Promise<*>;

  children?: HTMLElement;
  location: { pathname: string };
  params: { ref?: string };
  replace: (path: string) => void;
}

const STEP_APPLICATION = 'application';
const STEP_ACCOUNT = 'account';
const STEP_INFO = 'info';

const steps = (pathname) => [
  {
    key: STEP_APPLICATION,
    active: /application\/?([\w\-]+\/?)?$/.test(pathname),
    title: 'Apply',
  },
  {
    key: STEP_ACCOUNT,
    active: /\/account\/?/.test(pathname),
    title: 'Create Account',
  },
  {
    key: STEP_INFO,
    active: /\/info|actions|feed|shipping|integration\/?$/.test(pathname),
    title: 'More Info',
  },
];

class Main extends Component {
  props: Props;

  componentWillMount() {
    const {
      application,
      applicationApproved,
      applicationFetched,
      accounts,
      accountsFetched,
      feed,
      feedFetched,
      shipping,
      shippingFetched,
      fetchApplication,
      fetchAccounts,
      fetchFeed,
      fetchShipping,
      params: { ref },
    } = this.props;

    /** no application fetched yet - fetching application */
    if (ref && !applicationFetched) {
      fetchApplication(ref);
    }

    /** application is not approved yet - "thank you" page */
    if (applicationFetched && !applicationApproved) {
      this.replace(`/application/${ref}`);
    }

    /** application is approved but no accounts fetched - fetching accounts */
    if (applicationApproved && !accountsFetched) {
      fetchAccounts(get(application, 'merchant.id'));
    }

    /** accounts fetched but empty - account page */
    if (accountsFetched && isEmpty(accounts)) {
      this.replace(`/application/${ref}/account`);
    }

    if (accountsFetched && !isEmpty(accounts)) {
      /** accounts fetched and not empty - fetching shipping solutions */
      fetchShipping(get(application, 'merchant.id'));
    }

    /** shipping solutions fetched but empty - shipping solutions page */
    if (shippingFetched && isEmpty(shipping)) {
      this.replace(`/application/${ref}/shipping`);
    }

    /** shipping solutions fetched and not empty - actions page */
    if (shippingFetched && !isEmpty(shipping)) {
      this.replace(`/application/${ref}/actions`);
    }
  }

  replace(path: string) {
    if (this.props.location.pathname != path) {
      this.props.replace(path);
    }
  }

  get error(): HTMLElement {
    return (
      <ThanksOrNot
        title="Sorry"
        message={<span>No application with reference number<br />{this.props.params.ref}<br />found.</span>}
        error
      />
    );
  }

  get content(): HTMLElement {
    if (this.props.applicationFetchFailed) {
      return this.error;
    }

    return this.props.children;
  }

  render(): HTMLElement {
    return (
      <main className={styles.main}>
        <Steps steps={steps(this.props.location.pathname)} />
        {this.content}
      </main>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  applicationApproved: getApplicationApproved(state),
  applicationFetched: getApplicationFetched(state),
  applicationFetchFailed: getApplicationFetchFailed(state),
  accounts: getAccounts(state),
  accountsFetched: getAccountsFetched(state),
  feed: getFeed(state),
  feedFetched: getFeedFetched(state),
  shipping: getShipping(state),
  shippingFetched: getShippingFetched(state),
});

const mapActions = { fetchApplication, fetchAccounts, fetchFeed, fetchShipping, replace };

export default connect(mapState, mapActions)(withRouter(Main));
