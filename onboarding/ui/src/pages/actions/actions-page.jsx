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
          title="Login"
          description={`You are now ready to log into FoxCommerce Admin.`}
          button="Go to Admin"
          onSelect={() => window.location.replace(window.__ASHES_URL__)}
        />
      </div>
    );
  }

  render(): HTMLElement {
    return (
      <div className={styles.info}>
        <Header
          title="Welcome to FoxCommerce"
          legend={''}
        />
        {this.actions}
      </div>
    );
  }
}

export default connect(() => ({}), { push })(ActionsPage);
