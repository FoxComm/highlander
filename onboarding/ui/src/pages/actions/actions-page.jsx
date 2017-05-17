/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

import Header from '../../components/header/header';
import Card from '../../components/card/card';

import styles from './actions-page.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  params: Object;
  push: (path: string) => void;
}

class ActionsPage extends Component {
  props: Props;

  get actions() {
    return (
      <div className={styles.actions}>
        <Card
          title="Create a product"
          description={`In a few easy steps you can create a product and be selling using FoxCommerce in minutes.
          Manage Inventory & Orders withing from your dashboard.`}
          button="Select"
          onSelect={() => window.location.replace(window.__ASHES_URL__)}
        />
        <Card
          title="Add product feed"
          description={`Use existing product data to populate products in FoxCommerce.
          Manage Inventory & Orders withing from your dashboard.`}
          button="Select"
          onSelect={() => this.props.push(`/application/${this.props.params.ref}/feed`)}
        />
        <Card
          title="Integrate Platform"
          description={`Integrate your eCommerce store with FoxCommerce with Onboarding apps or via API to use your
          exising tools to import products and receive orders.`}
          button="Select"
          onSelect={() => this.props.push(`/application/${this.props.params.ref}/integration`)}
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

export default connect(() => ({}), { push })(ActionsPage);
