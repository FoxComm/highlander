/* @flow */

// libs
import React from 'react';

// styles
import styles from './stores-page.css';

export default class StoresPage extends React.Component {
  render() {
    return (
      <div>
        <header>
          <div styleName="header-wrap">
            <div styleName="text-wrap">
              <span styleName="description">Live in the freater baltimore area?</span>
              <h1 styleName="title">Come visit us!</h1>
            </div>
          </div>
        </header>
      </div>
    );
  }
}
