/* @flow */

import React, { Component } from 'react';
import classNames from 'classnames';

import styles from './overlay.css';

type Props = {
  shown: boolean,
  onClick?: Function,
};

class Overlay extends Component {
  props: Props;

  componentWillReceiveProps(nextProps: Props) {
    const appNode = document.getElementById('site');
    if (appNode) {
      if (nextProps.shown) {
        appNode.setAttribute('style', 'overflow: hidden !important; width: auto !important;');
      } else {
        appNode.setAttribute('style', '');
      }
    }
  }

  render() {
    const style = classNames({
      overlay: !this.props.shown,
      'overlay-shown': this.props.shown,
    });
    return <div styleName={style} onClick={this.props.onClick} />;
  }
}

export default Overlay;
