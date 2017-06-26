/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

import Header from '../../components/header/header';
import Card from '../../components/card/card';

import styles from './actions-page.css';

import type { HTMLElement } from '../../core/types';
import Button from '../../components/button/button';

type Props = {
  params: Object;
  push: (path: string) => void;
}

class ActionsPage extends Component {
  props: Props;

  render(): HTMLElement {
    return (
      <div className={styles.info}>
        <Header
          title="Welcome to FoxCommerce"
          legend={'Log into the Admin'}
        />
        <div className={styles.gotoadmin}>
          <div className={styles.gotobutton}>
            <Button className={styles.button} onClick={() => window.location.replace(window.__ASHES_URL__)}>Go to Admin</Button>
          </div>
        </div>
        {this.actions}
      </div>
    );
  }
}

export default connect(() => ({}), { push })(ActionsPage);
