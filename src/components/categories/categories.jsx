
import React, { PropTypes } from 'react';
import _ from 'lodash';
import styles from './categories.css';
import cssModules from 'react-css-modules';

const Categories = props => {
  const categoryItems = _.map(props.categoryList, (item) => {
    return (
      <div styleName="categoryItem">{item.toUpperCase()}</div>
    );
  });

  return (
    <div styleName="categories">
      {categoryItems}
    </div>
  );
};

Categories.propTypes = {
  categoryList: PropTypes.array,
};

Categories.defaultProps = {
  categoryList: ['all', 'eyeglasses', 'sunglasses', 'readers'],
};

export default cssModules(Categories, styles);
