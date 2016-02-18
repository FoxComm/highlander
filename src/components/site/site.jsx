
import React, { PropTypes } from 'react';
import Header from '../header/header';
import Sidebar from '../sidebar/sidebar';
import Modal from '../modal/modal';
import classNames from 'classnames';
import * as CountriesActions from '../../modules/countries';
import { transitionTo } from '../../route-helpers';
import * as auth from '../../auth';
import { connect } from 'react-redux';

@connect(state => state, CountriesActions)
export default class Site extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {isHidden: true};
  }

  static propTypes = {
    children: PropTypes.node,
    fetchCountries: PropTypes.func,
    routes: PropTypes.array.isRequired,
    params: PropTypes.object.isRequired
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  componentDidMount() {
    this.props.fetchCountries();
    this.checkAuth();
  }

  checkAuth() {
    if (!auth.isAuthenticated()) {
      transitionTo(this.context.history, 'login');
    }
    this.setState({isHidden: false});
  }

  render() {
    const classes = classNames('fc-admin', {'_hidden': this.state.isHidden});
    return (
      <div className={classes}>
        <Sidebar routes={this.props.routes} />
        <div className="fc-container">
          <Header routes={this.props.routes} params={this.props.params} />
          <main role='main' className="fc-main">
            {this.props.children}
          </main>
        </div>
        <Modal />
      </div>
    );
  }
}
