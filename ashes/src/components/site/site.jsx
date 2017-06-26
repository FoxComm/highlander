import React from 'react';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import Header from '../header/header';
import Sidebar from '../sidebar/sidebar';
import * as CountriesActions from '../../modules/countries';
import { connect } from 'react-redux';

import s from './site.css';

@connect(state => state, CountriesActions)
export default class Site extends React.Component {

  static propTypes = {
    children: PropTypes.node,
    fetchCountries: PropTypes.func,
    routes: PropTypes.array.isRequired,
    params: PropTypes.object.isRequired
  };

  componentDidMount() {
    this.props.fetchCountries();
  }

  render() {
    return (
      <div className={classNames(s.block, 'fc-admin')}>
        <Header
          className={s.header}
          routes={this.props.routes}
          params={this.props.params}
        />
        <div className={s.content}>
          <Sidebar
            routes={this.props.routes}
            params={this.props.params}
            className={s.sidebar}
          />
          <div className={classNames(s.container, 'fc-container')}>
            <main role='main' className={classNames(s.main, 'fc-main')}>
              {this.props.children}
            </main>
          </div>
        </div>
      </div>
    );
  }
}
