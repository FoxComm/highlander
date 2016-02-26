/* @flow */

import React, { PropTypes } from 'react';
import _ from 'lodash';
import styles from './categories.css';
import cssModules from 'react-css-modules';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as actions from '../../modules/categories';

const getState = state => ({ list: state.categories.list });

const mapDispatchToProps = dispatch => {
  return bindActionCreators(actions, dispatch);
};

class Categories extends React.Component {

  static propTypes = {
    list: PropTypes.array,
    fetchCategories: PropTypes.func.isRequired,
  };

  static defaultProps = {
    list: [],
  };

  componentDidMount() {
    this.props.fetchCategories();
  }

  render() {
    const categoryItems = _.map(this.props.list, (item) => {
      const key = `category-${item.replace(/\s/g, '-')}`;
      return (
        <div styleName="item" key={key}>
          <a href="#" styleName="item-link">{item.toUpperCase()}</a>
        </div>
      );
    });

    return (
      <div styleName="list">
        {categoryItems}
      </div>
    );
  }
}

export default connect(getState, mapDispatchToProps)(cssModules(Categories, styles));
