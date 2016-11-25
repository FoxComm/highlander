/* @flow */

import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import classNames from 'classnames';

import { toggleSidebar } from 'modules/sidebar';
import { toggleActive, resetTerm } from 'modules/search';

import styles from './header.css';

import Icon from 'ui/icon';
import Search from '../search/search';
import UserTools from '../usertools/usertools';
import Navigation from '../navigation/navigation';
import TopBanner from '../top-banner/top-banner';

type Props = {
  toggleSidebar: Function,
  toggleSearch: Function,
  isSearchActive: boolean,
  resetTerm: Function,
  path: string,
  query: ?Object,
  closeBanner: Function,
  isBannerVisible: boolean,
};

type State = {
  isScrolled: boolean,
};

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

  componentWillUpdate(nextProps) {
    if (this.props.path !== nextProps.path && this.props.isSearchActive) {
      this.props.toggleSearch();
    }
  }

  checkScroll = () => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
    const isScrolled = scrollTop > 136;

    this.setState({isScrolled});
  };

  render() {
    const headerStyle = this.state.isScrolled ? 'header-scrolled' : 'header';
    const headerClass = classNames(styles[headerStyle], {
      [styles['_without-banner']]: !this.props.isBannerVisible,
    });

    return (
      <div>
        <TopBanner
          isVisible={this.props.isBannerVisible}
          onClose={this.props.closeBanner}
        />
        <div className={headerClass}>
          <div styleName="wrap">
            <div styleName="hamburger" onClick={this.props.toggleSidebar}>
              <Icon name="fc-hamburger" styleName="head-icon"/>
            </div>
            <div styleName="search">
              <Search onSearch={this.props.toggleSearch} isScrolled={this.state.isScrolled}/>
            </div>
            <Link to="/" styleName="logo-link">
              <Icon styleName="logo" name="fc-logo"/>
            </Link>
            <div styleName="navigation">
              <Navigation path={this.props.path} />
            </div>
            <div styleName="tools">
              <UserTools path={this.props.path} query={this.props.query}/>
            </div>
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
