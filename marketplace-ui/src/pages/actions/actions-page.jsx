/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import Card from '../../components/card/card';

import {
  getApplication,
  getApplicationFetched,
  getApplicationFetchFailed,
} from '../../core/modules';
import { fetch as fetchApplication, clearErrors } from '../../core/modules/merchant-application';

import styles from './actions-page.css';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';

type Props = {
  params: Object;
  application: Application;
  applicationFetched: boolean;
  applicationFetchFailed: boolean;
  fetchApplication: (reference: string) => Promise<*>;
  clearErrors: () => void;
  replace: (path: string) => void;
}


class ActionsPage extends Component {
  props: Props;

  componentWillMount(): void {
    const {
      fetchApplication,
      params: { ref },
      applicationFetched,
      applicationFetchFailed,
      clearErrors,
      replace,
    } = this.props;

    if (!applicationFetched) {
      fetchApplication(ref);
    }

    if (applicationFetchFailed) {
      clearErrors();
      replace('/application');
    }
  }

  get actions() {
    return (
      <div className={styles.actions}>
        <Card
          title="Create a product"
          description={`In a few easy steps you can create a product and be selling through Goldfish in minutes.
          Manage Inventory & Orders withing from your dashboard.`}
          button="Select"
          onSelect={() => window.location.replace(process.env.ASHES_URL)}
        />
        <Card
          title="Add product feed"
          description={`Use existing product data to populate products in Goldfish.
          Manage Inventory & Orders withing from your dashboard.`}
          button="Select"
          onSelect={() => this.props.replace(`/application/${this.props.params.ref}/actions`)}
        />
        <Card
          title="Integrate Platform"
          description={`Integrate your eCommerce store with Goldfish with Marketplace apps or via API to use your
          exising tools to import products and receive orders.`}
          button="Select"
          onSelect={() => window.location.replace(process.env.ASHES_URL)}
        />
      </div>
    );
  }

  render(): HTMLElement {
    return (
      <div className={styles.info}>
        <Header
          title="What works for you?"
          legend={'We have a variety of tools to help you add products swiftly and scale.'}
        />
        {this.actions}
      </div>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  applicationFetched: getApplicationFetched(state),
  applicationFetchFailed: getApplicationFetchFailed(state),
});

export default connect(mapState, { fetchApplication, clearErrors, replace })(ActionsPage);
