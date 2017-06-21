// libs
import React from 'react';

// components
import SvgIcon from 'components/core/svg-icon';

// styles
import s from './svg-icons.css';

const iconSet = [
  'applications',
  'carts',
  'categories',
  'channels',
  'customers',
  'gift-cards',
  'groups',
  'orders',
  'plugins',
  'products',
  'promotions',
  'skus',
  'tags',
  'taxonomies',
];

const SvgIcons = () => {
  const icons = iconSet.map(iconType =>
    <div key={iconType} className={s.box}>
      <div className={s.iconWrapper}>
        <SvgIcon name={iconType} className={s.svgStyle} viewBox="0 0 19 19" />
      </div>
      <span className={s.text}>{iconType}</span>
    </div>
  );

  return (
    <div className={s.container}>
      {icons}
    </div>
  );
};

export default SvgIcons;
