/* @flow */

import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';

import { toggleSidebar } from 'modules/sidebar';
import { toggleActive, resetTerm } from 'modules/search';

import styles from './header.css';

import Icon from 'ui/icon';
import Search from '../search/search';
import UserTools from '../usertools/usertools';
import Navigation from '../navigation/navigation';

type Props = {
  toggleSidebar: Function;
  toggleSearch: Function;
  isSearchActive: boolean;
  resetTerm: Function;
  path: string;
  query: ?Object;
}

type State = {
  isScrolled: boolean;
}

class Header extends React.Component {
  props: Props;

  state: State = {
    isScrolled: false,
  };

  componentDidMount() {
    this.checkScroll();
    window.addEventListener('scroll', this.checkScroll);
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.checkScroll);
  }

  checkScroll = () => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
    const isScrolled = scrollTop > 80;

    this.setState({isScrolled});
  };

  changeCategoryCallback = () => {
    this.props.resetTerm();

    if (this.props.isSearchActive) {
      this.props.toggleSearch();
    }
  };

  render() {
    const headerStyle = this.state.isScrolled ? 'header-scrolled' : 'header';

    return (
      <div styleName={headerStyle}>
        <div styleName="wrap">
          <div styleName="hamburger" onClick={this.props.toggleSidebar}>
            <Icon name="fc-hamburger" styleName="head-icon"/>
          </div>
          <Link to="/" styleName="logo-link">
            <Icon styleName="logo" name="fc-some_brand_logo"/>
          </Link>
          <Navigation onClick={this.changeCategoryCallback}/>
          <div styleName="search">
            <Search onSearch={this.props.toggleSearch}/>
          </div>
          <div styleName="tools">
            <UserTools path={this.props.path} query={this.props.query}/>
          </div>
        </div>
      </div>
    );
  }
}

const mapState = state => ({
  isSearchActive: state.search.isActive,
});

export default connect(mapState, {
  toggleSidebar,
  toggleSearch: toggleActive,
  resetTerm,
})(Header);
