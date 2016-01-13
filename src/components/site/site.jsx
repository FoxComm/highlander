
import React, { PropTypes } from 'react';
import Header from '../header/header';
import Sidebar from '../sidebar/sidebar';
import Modal from '../modal/modal';
import * as CountriesActions from '../../modules/countries';
import { connect } from 'react-redux';

@connect(state => state, CountriesActions)
export default class Site extends React.Component {

  static propTypes = {
    children: PropTypes.node,
    fetchCountries: PropTypes.func
  };

  componentDidMount() {
    this.props.fetchCountries();
  }

  render() {
    return (
      <div className="fc-admin">
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
