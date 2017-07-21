/* @flow */

import React, { Component } from 'react';
import _ from 'lodash';

import styles from './overlay.css';

type Props = {
  shown: boolean,
  onClick?: Function,
};

class Overlay extends Component {
  props: Props;

  shouldComponentUpdate(nextProps: Props) {
    return !_.isEqual(nextProps, this.props);
  }

  componentDidUpdate() {
    const appNode = document.getElementById('site');
    if (appNode) {
      if (this.props.shown) {
        appNode.setAttribute('style', 'overflow: hidden; width: auto;');
      } else {
        appNode.setAttribute('style', '');
      }
    }
  }

  render() {
    const style = this.props.shown ? 'overlay-shown' : 'overlay';
    return <div styleName={style} onClick={this.props.onClick} />;
  }
}

export default Overlay;
