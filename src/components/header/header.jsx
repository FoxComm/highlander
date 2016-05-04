import React from 'react';
import { connect } from 'react-redux';
import {autobind} from 'core-decorators';
import { Link } from 'react-router';

import { toggleSidebar } from 'modules/sidebar';
import { toggleActive, resetTerm } from 'modules/search';

import styles from './header.css';

import Icon from 'ui/icon';
import Search from '../search/search';
import UserTools from '../usertools/usertools';
import Categories from '../categories/categories';

type Props = {
  toggleSidebar: Function;
  toggleSearch: Function;
  isSearchActive: boolean;
  resetTerm: Function;
  path: string;
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
    const scrollTop = document.body.scrollTop;
    const isScrolled = scrollTop > 80;

    this.setState({
      isScrolled: isScrolled,
    });
  }

  changeCategoryCallback = () => {
    this.props.resetTerm();

    if (this.props.isSearchActive) {
      this.props.toggleSearch();
    }
  };

  render() {
    const className = this.state.isScrolled ? '_isScrolled' : '';

    return (
      <div styleName="header" className={className}>
        <div styleName="hamburger" onClick={this.props.toggleSidebar}>
          <Icon name="fc-hamburger" styleName="head-icon"/>
        </div>
        <Link to="/" styleName="logo-link">
          <Icon styleName="logo" name="fc-some_brand_logo"/>
        </Link>
        <div styleName="categories">
          <Categories onClick={this.changeCategoryCallback}/>
        </div>
        <div styleName="search">
          <Search onSearch={this.props.toggleSearch}/>
        </div>
        <div styleName="tools">
          <UserTools path={this.props.path}/>
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
