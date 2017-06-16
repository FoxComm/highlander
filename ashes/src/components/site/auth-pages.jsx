
// @flow

import React, { Element, Component } from 'react';
import SvgIcon from 'components/core/svg-icon';

import s from './auth-pages.css';

type Props = {
  children: Element<*>,
};

type State = {
  isMounted: boolean,
};

export default class AuthPages extends Component {
  props: Props;

  state: State = {
    isMounted: false,
  };

  componentDidMount() {
    // eslint-disable-next-line react/no-did-mount-set-state
    this.setState({
      isMounted: true,
    });
  }

  get body(): Element<*> {
    return React.cloneElement(
      this.props.children, {
        isMounted: this.state.isMounted,
      }
    );
  }

  render() {
    return (
      <div className={s.body}>
        <SvgIcon name="start" className={s.logo} />
        {this.body}
        <div className={s.copyright}>
          © 2017 FoxCommerce. All rights reserved. Privacy Policy. Terms of Use.
        </div>
      </div>
    );
  }
}
