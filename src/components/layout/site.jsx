import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { dissoc } from 'sprout-data';

import * as actions from 'modules/auth';

import styles from './site.css';

import Overlay from '../overlay/overlay';
import Auth from '../auth/auth';

const mapState = state => ({
  isAuthBlockVisible: state.auth.isAuthBlockVisible,
});

/* ::`*/
@connect(mapState, actions)
/* ::`*/
class Site extends Component {
  renderAuthBlock() {
    const auth = this.props.location.query.auth;
    const pathname = this.props.location.pathname;
    const query = dissoc(this.props.location.query, 'auth');
    const path = {pathname, query};
    return (
      <Overlay path={path}>
        <Auth authBlockType={auth} path={path}/>
      </Overlay>
    );
  }

  render() {
    const isAuthBlockVisible = this.props.location.query && this.props.location.query.auth;

    return (
      <div styleName="site">
        {isAuthBlockVisible && this.renderAuthBlock()}
        {this.props.children}
      </div>
    );
  }
}

Site.propTypes = {
  children: PropTypes.node,
};

export default Site;
