import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';

import * as actions from 'modules/auth';

import styles from './site.css';

import Overlay from '../overlay/overlay';
import Auth from '../pages/auth/auth';
import Login from '../pages/auth/login';


const mapState = state => ({
  isAuthBlockVisible: state.auth.isAuthBlockVisible,
});

/* ::`*/
@connect(mapState, actions)
/* ::`*/
class Site extends Component {
  render() {
    return (
      <div styleName="site">
        {this.props.children}
        {this.props.isAuthBlockVisible && <Overlay><Auth><Login /></Auth></Overlay>}
      </div>
    );
  }
}

Site.propTypes = {
  children: PropTypes.node,
};

export default Site;
