import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';

import * as actions from 'modules/auth';

import styles from './site.css';

import Overlay from '../overlay/overlay';
import Auth from '../pages/auth/auth';

const mapState = state => ({
  isAuthBlockVisible: state.auth.isAuthBlockVisible,
});

/* ::`*/
@connect(mapState, actions)
/* ::`*/
class Site extends Component {
  renderAuthBlock() {
    if (this.props.location.query) {
      const auth = this.props.location.query.auth;
      const path = this.props.location.pathname;
      if (auth) {
        return (
          <Overlay path={path}>
            <Auth authBlockType={auth} path={path}/>
          </Overlay>
        );
      }
    }
  }

  render() {
    return (
      <div styleName="site">
        {this.props.children}
        {this.renderAuthBlock()}
      </div>
    );
  }
}

Site.propTypes = {
  children: PropTypes.node,
};

export default Site;
