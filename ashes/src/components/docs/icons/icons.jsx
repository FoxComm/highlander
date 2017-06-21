// libs
import React from 'react';

// components
import Icon from 'components/core/icon';

// styles
import s from './icons.css';

const iconSet = [
  'add',
  'bell',
  'chevron-right',
  'chevron-left',
  'chevron-up',
  'chevron-down',
  'customer',
  'customers',
  'discounts',
  'drag-drop',
  'edit',
  'external-link-2',
  'external-link',
  'gift-cards',
  'help',
  'home',
  'inventory',
  'items',
  'orders',
  'returns',
  'settings-col',
  'settings',
  'success',
  'error',
  'trash',
  'warning',
  'calendar',
  'lock',
  'unlock',
  'usd',
  'search',
  'visa',
  'dinners',
  'store-credit',
  'amex',
  'discover',
  'jcb',
  'close',
  'warning-dark',
  'success-dark',
  'error-dark',
  'check',
  'list',
  'grid',
  'barcode',
  'category',
  'back',
  'filter',
  'share',
  'ellipsis',
  'minus',
  'location',
  'down',
  'up',
  'desktop',
  'tablet',
  'sort',
  'mastercard',
  'google',
  'phone',
  'mobile',
  'upload',
  'save',
  'save-2',
  'heart',
  'promotion',
  'numbers',
  'align-left',
  'align-right',
  'bullets',
  'align-center',
  'align-justify',
  'indent_increase',
  'indent_decrease',
  'hyperlink',
  'size',
  'bold',
  'underline',
  'italic',
  'html',
  'dot',
  'category-collapse',
  'category-expand',
  'hierarchy',
  'hierarchy-rotated',
  'clear-formatting',
  'markdown',
  'export',
];

const Icons = () => {
  const icons = iconSet.map((iconType) => (
    <div key={iconType} className={s.box}>
      <div className={s.iconWrapper}>
        <Icon name={iconType} />
      </div>
      <span className={s.text}>{iconType}</span>
    </div>
  ));

  return (
    <div className={s.container}>
      {icons}
    </div>
  );
};

export default Icons;
